package org.keycloak.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class GroupAdminGroupMember {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final UserGroupMembershipExtensionEntity member;
    private final AdminEventBuilder adminEvent;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupRolesRepository = groupRolesRepository;
        this.memberUserAttributeConfigurationRepository =  new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.member = member;
        this.adminEvent = adminEvent;
    }


    @DELETE
    public Response deleteMember() {
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        userGroupMembershipExtensionRepository.deleteMember(member,group, user);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(member, realm)).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    @POST
    @Path("/role")
    public Response addGroupRole(@QueryParam("name") String name) {
        GroupRolesEntity role = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (role == null )
            throw new NotFoundException(" This role does not exist in this group");
        if (member.getGroupRoles() == null) {
            member.setGroupRoles(Stream.of(role).collect(Collectors.toList()));
        } else if (! member.getGroupRoles().stream().anyMatch(x -> role.getId().equals(x.getId()))) {
            member.getGroupRoles().add(role);
        }
        userGroupMembershipExtensionRepository.update(member);
        try {
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            UserModel user = session.users().getUserById(realm, member.getUser().getId());
            List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
            String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
            memberUserAttributeValues.add(Utils.createMemberUserAttribute(groupName, name, memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority()));
            user.setAttribute(memberUserAttribute.getUserAttribute(),memberUserAttributeValues);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) {
        if (member.getGroupRoles() == null || member.getGroupRoles().stream().noneMatch(x -> name.equals(x.getName())))
            throw new NotFoundException("Could not find this user group member role");

        member.getGroupRoles().removeIf(x -> name.equals(x.getName()));
        userGroupMembershipExtensionRepository.update(member);
        try {
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            UserModel user = session.users().getUserById(realm, member.getUser().getId());
            List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
            String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
            memberUserAttributeValues.removeIf(x-> x.startsWith(memberUserAttribute.getUrnNamespace()+Utils.groupStr+groupName+Utils.roleStr+name));
            user.setAttribute(memberUserAttribute.getUserAttribute(),memberUserAttributeValues);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
            userGroupMembershipExtensionRepository.suspendUser(user, member, justification, group);
            try {
                MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
                String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
                memberUserAttributeValues.removeIf(x-> x.startsWith(memberUserAttribute.getUrnNamespace()+Utils.groupStr+groupName));
                user.setAttribute(memberUserAttribute.getUserAttribute(),memberUserAttributeValues);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem suspended group member");
        }
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendSuspensionEmail(group.getName(), justification);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
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
            try {
                MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
                String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
                memberUserAttributeValues.removeIf(x-> x.startsWith(memberUserAttribute.getUrnNamespace()+Utils.groupStr+groupName));
                if (member.getGroupRoles() == null || member.getGroupRoles().isEmpty()) {
                    memberUserAttributeValues.add(Utils.createMemberUserAttribute(groupName, null, memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority()));
                } else {
                    memberUserAttributeValues.addAll(member.getGroupRoles().stream().map(role -> {
                        try {
                            return Utils.createMemberUserAttribute(groupName, role.getName(), memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority());
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList()));
                }
                user.setAttribute(memberUserAttribute.getUserAttribute(),memberUserAttributeValues);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
           adminEvent.operation(OperationType.UPDATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(member, realm)).resourcePath(session.getContext().getUri()).success();

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
