package org.rciam.plugins.groups.services;


import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.entities.GroupAdminEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class AdminGroups {

    private static final Logger logger = Logger.getLogger(AdminGroups.class);

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator realmAuth;
    private final GroupModel group;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final GeneralJpaService generalJpaService;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final AdminEventBuilder adminEvent;

    public AdminGroups(KeycloakSession session, AdminPermissionEvaluator realmAuth, GroupModel group, RealmModel realm, GeneralJpaService generalJpaService, AdminEventBuilder adminEvent, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupRolesRepository groupRolesRepository) {
        this.session = session;
        this.realm = realm;
        this.realmAuth = realmAuth;
        this.group = group;
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
        this.groupEnrollmentConfigurationRepository.setGroupRolesRepository(new GroupRolesRepository(session, realm));
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
        this.groupRolesRepository = groupRolesRepository;
        this.generalJpaService = generalJpaService;
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session);
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        MemberUserAttributeConfigurationEntity memberUserAttribute = (new MemberUserAttributeConfigurationRepository(session)).getByRealm(realm.getId());
        this.customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
    }

    @DELETE
    public void deleteGroup() {
        this.realmAuth.groups().requireManage(group);
        if (group.getSubGroupsStream().count()>0) {
            throw new ErrorResponseException("You need firstly to delete child groups.", "You need firstly to delete child groups.", Response.Status.BAD_REQUEST);
        }

        generalJpaService.removeGroup(group, realmAuth.adminAuth().getUser(),clientConnection, false);

        adminEvent.operation(OperationType.DELETE).representation(group.getName()).resourcePath(session.getContext().getUri()).success();
    }

    @GET
    @Path("/configuration/{id}}")
    @Produces("application/json")
    public GroupEnrollmentConfigurationRepresentation getGroupConfiguration(@PathParam("id") String id) {
        GroupEnrollmentConfigurationEntity groupConfiguration = groupEnrollmentConfigurationRepository.getEntity(id);
        //if not exist, group have only created from main Keycloak
        if (groupConfiguration == null) {
            throw new ErrorResponseException(Utils.NO_FOUND_GROUP_CONFIGURATION, Utils.NO_FOUND_GROUP_CONFIGURATION, Response.Status.NOT_FOUND);
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, false, realm);
        }
    }

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation rep) {
        realmAuth.groups().requireManage(group);
        if (rep.getId() == null) {
            groupEnrollmentConfigurationRepository.create(rep, group.getId());
        } else {
            GroupEnrollmentConfigurationEntity entity = groupEnrollmentConfigurationRepository.getEntity(rep.getId());
            if (entity != null) {
                groupEnrollmentConfigurationRepository.update(entity, rep);
            } else {
                throw new ErrorResponseException(Utils.NO_FOUND_GROUP_CONFIGURATION, Utils.NO_FOUND_GROUP_CONFIGURATION, Response.Status.NOT_FOUND);
            }
        }
        //aup change action
        return Response.noContent().build();
    }

    @POST
    @Path("/admin/{userId}")
    public Response addGroupAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new ErrorResponseException(Utils.NO_USER_FOUND, Utils.NO_USER_FOUND, Response.Status.NOT_FOUND);
        }
        realmAuth.users().requireManageGroupMembership(user);
        try {
            if (!groupAdminRepository.isGroupAdmin(user.getId(), group)) {
                groupAdminRepository.addGroupAdmin(userId, group.getId());

                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(true, ModelToRepresentation.buildGroupPath(group), group.getId(), realmAuth.adminAuth().getUser());
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(user.getUsername() + " is already group admin for the " + group.getName() + " group or one of its parent.").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ModelDuplicateException.class.equals(e.getClass()) ? "Admin has already been existed" : "Problem during admin save").build();
        }
    }

    @DELETE
    @Path("/admin/{userId}")
    public Response removeGroupAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new ErrorResponseException(Utils.NO_USER_FOUND, Utils.NO_USER_FOUND, Response.Status.NOT_FOUND);
        }
        realmAuth.users().requireManageGroupMembership(user);
        GroupAdminEntity admin = groupAdminRepository.getGroupAdminByUserAndGroup(userId, group.getId());
        if (admin != null) {
            groupAdminRepository.deleteEntity(admin.getId());
            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(false, ModelToRepresentation.buildGroupPath(group), group.getId(), realmAuth.adminAuth().getUser());
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        } else {
            throw new ErrorResponseException("This admin does not exist", "This admin does not exist", Response.Status.NOT_FOUND);
        }
        return Response.noContent().build();
    }


    @POST
    @Path("children")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        this.realmAuth.groups().requireManage(group);
        return Utils.addGroupChild(rep, realm, group, session, adminEvent, groupEnrollmentConfigurationRepository, groupRolesRepository);
    }


}
