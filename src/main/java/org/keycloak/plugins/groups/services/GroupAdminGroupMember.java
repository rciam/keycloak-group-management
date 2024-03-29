package org.keycloak.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.LoginEventHelper;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class GroupAdminGroupMember {

    @Context
    private ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final UserGroupMembershipExtensionEntity member;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel groupAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository, GroupAdminRepository groupAdminRepository) {
        this.session = session;
        this.realm = realm;
        this.groupAdmin = groupAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupRolesRepository = groupRolesRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.member = member;
    }

    @PUT
    @Consumes("application/json")
    public Response updateMember(UserGroupMembershipExtensionRepresentation rep) throws UnsupportedEncodingException {
        userGroupMembershipExtensionRepository.update(rep, member, group, session, groupAdmin, clientConnection);

        try {
            UserModel memberUser = session.users().getUserById(realm, member.getUser().getId());
            customFreeMarkerEmailTemplateProvider.setUser(memberUser);
            customFreeMarkerEmailTemplateProvider.sendMemberUpdateUserInformEmail(group.getName(), groupAdmin);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendMemberUpdateAdminInformEmail(group.getName(), memberUser, groupAdmin);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }


    @DELETE
    public Response deleteMember() {
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        userGroupMembershipExtensionRepository.deleteMember(member, group, user, clientConnection, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId()), memberUserAttribute);
        return Response.noContent().build();
    }

    @POST
    @Path("/role")
    public Response addGroupRole(@QueryParam("name") String name) throws UnsupportedEncodingException {
        GroupRolesEntity role = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (role == null) throw new NotFoundException(" This role does not exist in this group");
        if (member.getGroupRoles() == null) {
            member.setGroupRoles(Stream.of(role).collect(Collectors.toList()));
        } else if (member.getGroupRoles().stream().noneMatch(x -> role.getId().equals(x.getId()))) {
            member.getGroupRoles().add(role);
        }
        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
        String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
        memberUserAttributeValues.add(Utils.createMemberUserAttribute(groupName, name, memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority()));
        user.setAttribute(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());
        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) throws UnsupportedEncodingException {
        if (member.getGroupRoles() == null || member.getGroupRoles().stream().noneMatch(x -> name.equals(x.getName())))
            throw new NotFoundException("Could not find this user group member role");

        member.getGroupRoles().removeIf(x -> name.equals(x.getName()));
        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
        String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
        memberUserAttributeValues.removeIf(x -> x.startsWith(memberUserAttribute.getUrnNamespace() + Utils.groupStr + groupName + Utils.roleStr + name));
        user.setAttribute(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

        return Response.noContent().build();
    }

    @POST
    @Path("/suspend")
    public Response suspendUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipExtensionRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            List<String> subgroupPaths = userGroupMembershipExtensionRepository.suspendUser(user, member, justification, group, memberUserAttributeConfigurationRepository);
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_SUSPEND, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendSuspensionEmail(ModelToRepresentation.buildGroupPath(group), subgroupPaths, justification);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem suspended group member");
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/activate")
    public Response activateUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipExtensionRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            userGroupMembershipExtensionRepository.activateUser(user, member, justification, group);
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            Utils.changeUserAttributeValue(user, member, Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm), memberUserAttribute);

            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_CREATE, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem activate group member");
        }
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendActivationEmail(group.getName(), justification);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

}
