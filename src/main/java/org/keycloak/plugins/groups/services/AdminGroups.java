package org.keycloak.plugins.groups.services;


import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
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
    private final GroupAdminRepository groupAdminRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public AdminGroups(KeycloakSession session, AdminPermissionEvaluator realmAuth, GroupModel group,  RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.realmAuth = realmAuth;
        this.group = group;
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, session.getContext().getRealm());
        this.groupAdminRepository =  new GroupAdminRepository(session, session.getContext().getRealm());
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
  }

    @GET
    @Path("/configuration/{id}}")
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration(@PathParam("id") String id) {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(id);
        //if not exist, group have only created from main Keycloak
        if(groupConfiguration == null) {
            throw new NotFoundException("Could not find this Group Configuration");
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, realm);
        }
    }

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        realmAuth.groups().requireManage(group);
        if (rep.getId() == null ) {
            groupConfigurationRepository.create(rep, group.getId(), realmAuth.adminAuth().getUser().getId());
        } else {
            GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(rep.getId());
            if (entity != null) {
                groupConfigurationRepository.update(entity, rep, realmAuth.adminAuth().getUser().getId());
            } else {
                throw new NotFoundException("Could not find this group configuration");
            }
        }
        //aup change action
        return Response.noContent().build();
    }

    @POST
    @Path("/admin/{userId}")
    public Response addVoAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        realmAuth.users().requireManageGroupMembership(user);
        try {
            groupAdminRepository.addGroupAdmin(userId, group.getId());

            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), true);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ModelDuplicateException.class.equals(e.getClass()) ? "Admin has already been existed" : "Problem during admin save").build();
        }
    }

    @DELETE
    @Path("/admin/{userId}")
    public Response removeVoAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        realmAuth.users().requireManageGroupMembership(user);
        GroupAdminEntity admin = groupAdminRepository.getGroupAdminByUserAndGroup(userId, group.getId());
        if (admin != null) {
            groupAdminRepository.deleteEntity(admin.getId());
            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), false);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        } else {
            throw new NotFoundException("This admin does not exist");
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
            groupConfigurationRepository.createDefault(groupId, rep.getName());
        }
        return Response.noContent().build();
    }


}
