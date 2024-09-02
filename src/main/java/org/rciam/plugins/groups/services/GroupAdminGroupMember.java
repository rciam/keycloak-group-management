package org.rciam.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ForbiddenException;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.LoginEventHelper;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.services.ServicesLogger;

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
    private final boolean isGroupAdmin;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel groupAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository, GroupAdminRepository groupAdminRepository,boolean isGroupAdmin) {
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
        this.isGroupAdmin = isGroupAdmin;
    }

    @PUT
    @Consumes("application/json")
    public Response updateMember(UserGroupMembershipExtensionRepresentation rep) throws UnsupportedEncodingException {
        //validation tasks
        //1. active member
        //2. at least one role
        //3. Member since: date cannot be in the past or after expiration for PENDING members
        //4. Expiration date: date cannot be in the past
        if (MemberStatusEnum.SUSPENDED.equals(member.getStatus())) {
            throw new BadRequestException("Suspended members can not be updated");
        } else if (rep.getGroupRoles() == null || rep.getGroupRoles().isEmpty()) {
            throw new BadRequestException("At least one role must be existed");
        } else if (rep.getMembershipExpiresAt() != null && LocalDate.now().isAfter(rep.getMembershipExpiresAt())) {
            throw new BadRequestException("Expiration date must not be in the past");
        } else if (MemberStatusEnum.PENDING.equals(member.getStatus()) && ( rep.getValidFrom() == null || LocalDate.now().isAfter(rep.getValidFrom()) || (rep.getMembershipExpiresAt() != null && rep.getMembershipExpiresAt().isBefore(rep.getValidFrom())))) {
            throw new BadRequestException("Member since must not be in the past or after expiration date");
        } else if (MemberStatusEnum.ENABLED.equals(member.getStatus())) {
            //For enabled member do to change valid from
            rep.setValidFrom(member.getValidFrom());
        }

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
        if (!isGroupAdmin){
            throw new ForbiddenException();
        }

        GroupRolesEntity role = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (role == null) throw new NotFoundException(" This role does not exist in this group");
        if (member.getGroupRoles() == null) {
            member.setGroupRoles(Stream.of(role).collect(Collectors.toList()));
        } else if (member.getGroupRoles().stream().noneMatch(x -> role.getId().equals(x.getId()))) {
            member.getGroupRoles().add(role);
        }  else {
            throw new BadRequestException("This role already exists.");
        }

        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        List<String> memberUserAttributeValues = user.getAttributeStream(memberUserAttribute.getUserAttribute()).collect(Collectors.toList());
        String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
        memberUserAttributeValues.add(Utils.createMemberUserAttribute(groupName, name, memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority()));
        user.setAttribute(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);

        String groupPath = ModelToRepresentation.buildGroupPath(group);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendRolesChangesUserEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendRolesChangesGroupAdminEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), groupAdmin, user);
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
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) throws UnsupportedEncodingException {
        if (!isGroupAdmin){
            throw new ForbiddenException();
        }

        if (member.getGroupRoles() == null || member.getGroupRoles().stream().noneMatch(x -> name.equals(x.getName())))
            throw new NotFoundException("Could not find this user group member role");

        member.getGroupRoles().removeIf(x -> name.equals(x.getName()));
        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        List<String> memberUserAttributeValues = user.getAttributeStream(memberUserAttribute.getUserAttribute()).collect(Collectors.toList());
        String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
        memberUserAttributeValues.removeIf(x -> x.startsWith(memberUserAttribute.getUrnNamespace() + Utils.groupStr + groupName + Utils.colon + Utils.roleStr + name));
        user.setAttribute(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);

        String groupPath = ModelToRepresentation.buildGroupPath(group);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendRolesChangesUserEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendRolesChangesGroupAdminEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), groupAdmin, user);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/suspend")
    public Response suspendUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipExtensionRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        } else if (MemberStatusEnum.ENABLED.equals(member.getStatus())) {
            throw new BadRequestException("Only enabled users can be suspended.");
        }
        try {
            String groupPath = ModelToRepresentation.buildGroupPath(group);
            List<String> subgroupPaths = userGroupMembershipExtensionRepository.suspendUser(user, member, justification, group, memberUserAttributeConfigurationRepository);
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_SUSPEND, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendSuspensionEmail(groupPath, subgroupPaths, justification);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendSuspensionEmailToAdmins(groupPath, subgroupPaths, justification, user, groupAdmin);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
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
        } else if (MemberStatusEnum.SUSPENDED.equals(member.getStatus())) {
            throw new BadRequestException("Only suspended users can be reactivated.");
        }
        try {
            String groupPath = ModelToRepresentation.buildGroupPath(group);
            List<String> subgroupPaths = userGroupMembershipExtensionRepository.reActivateUser(user, member, justification, group, memberUserAttributeConfigurationRepository);

            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_CREATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendActivationEmail(groupPath, subgroupPaths, justification);

                groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                    try {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendActivationEmailToAdmins(groupPath, subgroupPaths, justification, user, groupAdmin);
                    } catch (EmailException e) {
                        ServicesLogger.LOGGER.failedToSendEmail(e);
                    }
                });
        } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem activate group member");
        }

        return Response.noContent().build();
    }

}
