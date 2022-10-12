package org.keycloak.plugins.groups.services;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipRepository;
import org.keycloak.services.ServicesLogger;

public class GroupAdminGroupMember {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final UserGroupMembershipEntity member;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipRepository userGroupMembershipRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipEntity member) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.member = member;
    }

    @POST
    @Path("/suspend")
    public Response suspendUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            userGroupMembershipRepository.suspendUser(user, member, justification, group);
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
        UserModel user = userGroupMembershipRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            userGroupMembershipRepository.activateUser(user, member, justification, group);
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
