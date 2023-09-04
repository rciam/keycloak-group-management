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
import javax.ws.rs.Produces;
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
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.GeneralJpaService;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupManagementEventRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.representations.MemberUserAttributeConfigurationRepresentation;
import org.keycloak.plugins.groups.scheduled.AgmTimerProvider;
import org.keycloak.plugins.groups.scheduled.MemberUserAttributeCalculatorTask;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

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
    private final GroupRolesRepository groupRolesRepository;
    private final GeneralJpaService generalJpaService;
    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;

    public AdminService(KeycloakSession session, RealmModel realm, ClientConnection clientConnection, AdminPermissionEvaluator realmAuth) {
        this.session = session;
        this.realm = realm;
        this.clientConnection = clientConnection;
        this.realmAuth =  realmAuth;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentConfigurationRepository.setGroupRolesRepository(new GroupRolesRepository(session, realm));
        this.generalJpaService =  new GeneralJpaService(session, realm, groupEnrollmentConfigurationRepository);
        this.groupRolesRepository = new GroupRolesRepository(session, realm);
        this.adminEvent =  new AdminEventBuilder(realm, realmAuth.adminAuth(), session, clientConnection);
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        adminEvent.realm(realm);
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
        adminEvent.resource(ResourceType.REALM).operation(OperationType.UPDATE).representation(attributes).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    @GET
    @Path("/member-user-attribute/configuration")
    @Produces(MediaType.APPLICATION_JSON)
    public MemberUserAttributeConfigurationRepresentation memberUserAttributeConfiguration() {
        MemberUserAttributeConfigurationEntity memberUserAttributeEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        return memberUserAttributeEntity != null ? EntityToRepresentation.toRepresentation(memberUserAttributeEntity) : new MemberUserAttributeConfigurationRepresentation();
    }

    @POST
    @Path("/member-user-attribute/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configureMemberUserAttribute(MemberUserAttributeConfigurationRepresentation rep) {
        MemberUserAttributeConfigurationEntity memberUserAttributeEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        memberUserAttributeEntity.setUserAttribute(rep.getUserAttribute());
        memberUserAttributeEntity.setUrnNamespace(rep.getUrnNamespace());
        memberUserAttributeEntity.setAuthority(rep.getAuthority());
        memberUserAttributeConfigurationRepository.update(memberUserAttributeEntity);
        return Response.noContent().build();
    }

    @POST
    @Path("/memberUserAttribute/calculation")
    public Response calculateMemberUserAttribute(){
        AgmTimerProvider timer = (AgmTimerProvider) session.getProvider(TimerProvider.class, "agm");
        timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new MemberUserAttributeCalculatorTask(realm.getId()), 1000),  1000, "MemberUserAttributeCalculator_"+ KeycloakModelUtils.generateId());
        return Response.noContent().build();
    }


    @Path("/configuration-rules")
    public AdminEnrollmentConfigurationRules adminEnrollmentConfigurationRules() {
        AdminEnrollmentConfigurationRules service = new AdminEnrollmentConfigurationRules(realm, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }
    @Path("/group/{groupId}")
    public AdminGroups adminGroups(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        realmAuth.groups().requireView(group);
        AdminGroups service = new AdminGroups(session, realmAuth, group, realm, generalJpaService, adminEvent, groupEnrollmentConfigurationRepository, groupRolesRepository);
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
        } else if (groupEnrollmentConfigurationRepository.getByGroup(rep.getId()).collect(Collectors.toList()).isEmpty()) {
            //group creation - group configuration no exist
            logger.info("Create group with groupId === "+rep.getId());
            groupEnrollmentConfigurationRepository.createDefault(realm.getGroupById(rep.getId()), rep.getName());
            groupRolesRepository.create(Utils.defaultGroupRole,rep.getId());
        }
        //if rep.getId() != null => mean that group has been moved( not created)
        logger.info("group configuration exists ==== "+rep.getId());

        return Response.noContent().build();
    }

    @DELETE
    @Path("/user/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        UserModel user = session.users().getUserById(realm, id);
        if (user == null) {
            throw new NotFoundException("Could not find user by id");
        }
        realmAuth.users().requireManage(user);

        String username = user.getUsername();
        boolean removed = generalJpaService.removeUser(user);

        if (removed) {
            adminEvent.resource(ResourceType.USER).operation(OperationType.DELETE).representation(username).resourcePath(session.getContext().getUri()).success();
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }
}
