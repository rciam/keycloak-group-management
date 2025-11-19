package org.rciam.plugins.groups.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.MemberUserAttributeConfigurationRepresentation;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.MemberUserAttributeCalculatorTask;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

public class AdminService {

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
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;

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
        this.groupEnrollmentConfigurationRulesRepository =  new GroupEnrollmentConfigurationRulesRepository(session);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        adminEvent.realm(realm);
    }

    @DELETE
    public void deleteRealm() {
        realmAuth.realm().requireManageRealm();
        groupEnrollmentConfigurationRulesRepository.getByRealm(realm.getId()).forEach(groupEnrollmentConfigurationRulesRepository::deleteEntity);
        MemberUserAttributeConfigurationEntity memberUserAttributeConfigurationEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        if (memberUserAttributeConfigurationEntity != null)
            memberUserAttributeConfigurationRepository.deleteEntity(memberUserAttributeConfigurationEntity);

        realm.getGroupsStream().forEach(group -> generalJpaService.removeGroup(group, realmAuth.adminAuth().getUser(),clientConnection, true));

        //Keycloak code for realm remove
        if (!new RealmManager(session).removeRealm(realm)) {
            throw new ErrorResponseException("Realm doesn't exist", "Realm doesn't exist", Response.Status.NOT_FOUND);
        }

        // The delete event is associated with the realm of the user executing the operation,
        // instead of the realm being deleted.
       adminEvent.operation(OperationType.DELETE).resource(ResourceType.REALM)
                .realm(realmAuth.adminAuth().getRealm()).resourcePath(realm.getName()).success();
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
        realmAuth.realm().requireManageRealm();
        MemberUserAttributeConfigurationEntity memberUserAttributeEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        return memberUserAttributeEntity != null ? EntityToRepresentation.toRepresentation(memberUserAttributeEntity) : new MemberUserAttributeConfigurationRepresentation();
    }

    @POST
    @Path("/member-user-attribute/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configureMemberUserAttribute(MemberUserAttributeConfigurationRepresentation rep) {
        realmAuth.realm().requireManageRealm();
        MemberUserAttributeConfigurationEntity memberUserAttributeEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        if (memberUserAttributeEntity.getId() == null)
            memberUserAttributeEntity.setId(KeycloakModelUtils.generateId());
        memberUserAttributeEntity.setUserAttribute(rep.getUserAttribute());
        memberUserAttributeEntity.setUrnNamespace(rep.getUrnNamespace());
        memberUserAttributeEntity.setAuthority(rep.getAuthority());
        memberUserAttributeEntity.setSignatureMessage(rep.getSignatureMessage());
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setId(realm.getId());
        memberUserAttributeEntity.setRealmEntity(realmEntity);
        memberUserAttributeConfigurationRepository.update(memberUserAttributeEntity);
        return Response.noContent().build();
    }

    @POST
    @Path("/memberUserAttribute/calculation")
    public Response calculateMemberUserAttribute(){
        AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
        timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new MemberUserAttributeCalculatorTask(realm.getId()), 1000),  1000, "MemberUserAttributeCalculator_"+ KeycloakModelUtils.generateId());
        return Response.noContent().build();
    }


    @Path("/configuration-rules")
    public AdminEnrollmentConfigurationRules adminEnrollmentConfigurationRules() {
        return new AdminEnrollmentConfigurationRules(realm, session, adminEvent, realmAuth);
    }
    @Path("/group/{groupId}")
    public AdminGroups adminGroups(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new ErrorResponseException("Could not find group by id", "Could not find group by id", Response.Status.NOT_FOUND);
        }
        realmAuth.groups().requireView(group);
        return new AdminGroups(session, realmAuth, group, realm, generalJpaService, adminEvent, groupEnrollmentConfigurationRepository, groupRolesRepository);
    }

    @POST
    @Path("/group")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTopLevelGroup(GroupRepresentation rep) {
        realmAuth.groups().requireManage();
        return generalJpaService.addTopLevelGroup(rep, adminEvent);
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
            throw new ErrorResponseException("User couldn't be deleted","User couldn't be deleted", Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @Path("/effective-expiration-date/calculation")
    public Response calculateEffectiveExpirationDate() {
        userGroupMembershipExtensionRepository.migrateEffectiveExpiresAt();
        return Response.noContent().build();
    }
}
