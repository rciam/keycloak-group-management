package org.keycloak.plugins.groups.services;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.keycloak.services.resources.admin.GroupsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.theme.FreeMarkerUtil;

public class AdminGroups {

    private static final Logger logger = Logger.getLogger(AdminGroups.class);

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator realmAuth;
    private final GroupModel group;
    private final GroupConfigurationRepository groupConfigurationRepository;
    private final UserVoGroupMembershipRepository userVoGroupMembershipRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public AdminGroups(KeycloakSession session, AdminPermissionEvaluator realmAuth, GroupModel group,  RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.realmAuth = realmAuth;
        this.group = group;
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, session.getContext().getRealm());
        this.userVoGroupMembershipRepository =  new UserVoGroupMembershipRepository(session, session.getContext().getRealm());
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
  }

    @GET
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration() {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(group.getId());
        //if not exist, group have only created from main Keycloak
        if(groupConfiguration == null) {
            throw new NotFoundException("Could not find this Group");
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, realm);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        realmAuth.groups().requireManage(group);
        GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(group.getId());
        if ( entity != null) {
            groupConfigurationRepository.update(entity, rep, realmAuth.adminAuth().getUser().getId());
        } else {
            throw new NotFoundException("Could not find this Group");
        }
        //aup change action
        return Response.noContent().build();
    }

    @POST
    @Path("/member/{userId}/admin")
    @Produces("application/json")
    public Response addOrRemoveVoAdmin(@PathParam("userId") String userId, @QueryParam("addVoMember") Boolean addVoMember) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        realmAuth.users().requireManageGroupMembership(user);
        UserVoGroupMembershipEntity member = userVoGroupMembershipRepository.getByUserAndGroup(group.getId(), user.getId());
        if ( member == null ) {
            throw new NotFoundException("Could not find this Group member");
        }
        member.setIsAdmin(addVoMember);
        userVoGroupMembershipRepository.update(member);
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), addVoMember);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }


    @POST
    @Path("children")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AdminPermissionEvaluator realmAuth = authHelper.authenticateRealmAdminRequest();
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, realmAuth.adminAuth(), session, clientConnection);
        adminEvent.realm(realm).resource(ResourceType.REALM);
        GroupResource groupResource = new GroupResource(realm, group, session, realmAuth,adminEvent);
        Response response = groupResource.addChild(rep);
        if (response.getStatus() >= 400) {
            //error response from client creation
            return response;
        } else  {
            //group creation
            //get id from GroupRepresentation (response body)
            String groupId = response.readEntity(GroupRepresentation.class).getId();
            groupConfigurationRepository.createDefault(groupId);
        }
        return Response.noContent().build();
    }


}
