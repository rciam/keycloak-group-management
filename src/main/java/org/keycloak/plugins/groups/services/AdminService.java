package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.GeneralJpaService;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupManagementEventRepository;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class AdminService {

    private static final Logger logger = Logger.getLogger(AdminService.class);
    private static final List<String> realmAttributesNames = Stream.of(Utils.expirationNotificationPeriod, Utils.invitationExpirationPeriod).collect(Collectors.toList());

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private final RealmModel realm;
    private  final AdminPermissionEvaluator realmAuth;
    private final AdminEventBuilder adminEvent;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GeneralJpaService generalJpaService;

    private final GroupManagementEventRepository groupManagementEventRepository;

    public AdminService(KeycloakSession session, RealmModel realm, ClientConnection clientConnection, AdminPermissionEvaluator realmAuth) {
        this.session = session;
        this.realm = realm;
        this.clientConnection = clientConnection;
        this.realmAuth =  realmAuth;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.generalJpaService =  new GeneralJpaService(session, realm, groupEnrollmentConfigurationRepository);
        this.groupManagementEventRepository = new GroupManagementEventRepository (session, realm);
        this.adminEvent =  new AdminEventBuilder(realm, realmAuth.adminAuth(), session, clientConnection);
        adminEvent.realm(realm);
    }

    @GET
    @Path("/server-url")
    public Response configureServerUrl(@NotNull @QueryParam("url") String url) {
        GroupManagementEventEntity eventEntity = groupManagementEventRepository.getEntity(Utils.eventId);
        eventEntity.setServerUrl(url);
        groupManagementEventRepository.update(eventEntity);
        return Response.noContent().build();
    }


    @PUT
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configureGroupManagement(Map<String, String> attributes) {
        realmAuth.realm().requireManageRealm();
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            if (realmAttributesNames.contains(attr.getKey()))
                realm.setAttribute(attr.getKey(), attr.getValue());
        }
        return Response.noContent().build();
    }

    @Path("/group/{groupId}")
    public AdminGroups adminGroups(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        realmAuth.groups().requireView(group);
        AdminGroups service = new AdminGroups(session, realmAuth, group, realm, generalJpaService, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/group")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTopLevelGroup(GroupRepresentation rep) {
        GroupsResource groupsResource = new GroupsResource(realm, session, realmAuth,adminEvent);
        Response response = groupsResource.addTopLevelGroup(rep);
        logger.info("group have been created with status"+response.getStatus());
        if (response.getStatus() >= 400) {
            //error response from client creation
            return response;
        } else if (groupEnrollmentConfigurationRepository.getEntity(rep.getId()) == null) {
            //group creation - group configuration no exist
            logger.info("Create group with groupId === "+rep.getId());
            groupEnrollmentConfigurationRepository.createDefault(rep.getId(), rep.getName());
        }
        //if rep.getId() != null => mean that group has been moved( not created)
        logger.info("group configuration exists ==== "+rep.getId());

        return Response.noContent().build();
    }

    @DELETE
    @Path("/user/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AdminPermissionEvaluator realmAuth = authHelper.authenticateRealmAdminRequest();
        UserModel user = session.users().getUserById(realm, id);
        if (user == null) {
            throw new NotFoundException("Could not find user by id");
        }
        realmAuth.users().requireManage(user);

        boolean removed = generalJpaService.removeUser(user);

        if (removed) {
            adminEvent.resource(ResourceType.USER).operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }
}
