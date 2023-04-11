package org.keycloak.plugins.groups.services;

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
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
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
    private final GroupAdminRepository groupAdminRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final UserGroupMembershipExtensionEntity member;
    private final AdminEventBuilder adminEvent;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupAdminRepository groupAdminRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupRolesRepository = groupRolesRepository;
        this.groupAdminRepository = groupAdminRepository;
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
        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{id}")
    public Response deleteGroupRole(@PathParam("id") String id) {
        if (member.getGroupRoles() != null && (member.getGroupRoles().removeIf(x -> id.equals(x.getId()))))
            userGroupMembershipExtensionRepository.update(member);
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

    @POST
    @Path("/admin")
    public Response addAsGroupAdmin(){
        groupAdminRepository.addGroupAdmin(member.getUser().getId(), group.getId());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm, member.getUser().getId()));
            customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), true);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();

    }


}
