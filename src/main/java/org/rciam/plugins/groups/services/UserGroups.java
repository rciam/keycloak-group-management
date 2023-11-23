package org.rciam.plugins.groups.services;

import jakarta.ws.rs.*;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.utils.ModelToRepresentation;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.rciam.plugins.groups.representations.GroupInvitationRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    @Context
    private ClientConnection clientConnection;

    private static final String INVITATION_NOT_EXISTS = "This invitation does not exist or has been expired";

    protected final KeycloakSession session;
    private final RealmModel realm;

    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final GeneralJpaService generalJpaService;
    private final UserModel user;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public UserGroups(KeycloakSession session, RealmModel realm, UserModel user) {
        this.session = session;
        this.realm = realm;
        this.user = user;
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentRequestRepository = new GroupEnrollmentRequestRepository(session, realm, new GroupRolesRepository(session, realm));
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm, groupEnrollmentConfigurationRepository, new GroupRolesRepository(session, realm));
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm);
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session);
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        this.customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
        this.generalJpaService = new GeneralJpaService(session, realm, groupEnrollmentConfigurationRepository);
    }


    @GET
    @Path("/groups")
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentationPager getAllUserGroups(@QueryParam("search") String search,
                                                                            @QueryParam("first") @DefaultValue("0") Integer first,
                                                                            @QueryParam("max") @DefaultValue("10") Integer max,
                                                                            @QueryParam("order") @DefaultValue("group.name") String order,
                                                                            @QueryParam("asc") @DefaultValue("true") boolean asc) {
        return userGroupMembershipExtensionRepository.userpager(user.getId(), search, new PagerParameters(first, max, order, asc ? "asc" : "desc"));
    }

    @Path("/group/{groupId}")
    public UserGroup userGroup(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        UserGroup service = new UserGroup(session, realm, groupEnrollmentConfigurationRepository, user, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("groups/configurations")
    @Produces("application/json")
    public List<GroupEnrollmentConfigurationRepresentation> getAvailableGroupEnrollmentConfigurationsByGroup(@QueryParam("groupPath") String groupPath) {
        String[] groupNames = groupPath.split("/");
        List<GroupEntity> groups = generalJpaService.getGroupByName(groupNames[groupNames.length - 1]);
        String groupId = null;
        for (GroupEntity x : groups) {
            GroupModel group = realm.getGroupById(x.getId());
            String xPath = ModelToRepresentation.buildGroupPath(group);
            if (groupPath.equals(xPath)) {
                groupId = x.getId();
                break;
            }
        }

        if (groupId == null) {
            throw new NotFoundException("This group does not exist");
        }
        return groupEnrollmentConfigurationRepository.getAvailableByGroup(groupId).map(x -> EntityToRepresentation.toRepresentation(x, false, realm)).collect(Collectors.toList());
    }

    @Path("/group/{groupId}/member")
    public UserGroupMember userGroupMember(@PathParam("groupId") String groupId) {
        UserGroupMembershipExtensionEntity entity = userGroupMembershipExtensionRepository.getByUserAndGroup(groupId, user.getId());
        if (entity == null) {
            throw new NotFoundException("You are not member of this group");
        }

        UserGroupMember service = new UserGroupMember(session, realm, user, entity, userGroupMembershipExtensionRepository);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }


    @GET
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentRequestPager getMyEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                        @QueryParam("max") @DefaultValue("10") Integer max,
                                                        @QueryParam("groupId") String groupId,
                                                        @QueryParam("groupName") String groupName,
                                                        @QueryParam("status") EnrollmentRequestStatusEnum status,
                                                        @QueryParam("order") @DefaultValue("submittedDate") String order,
                                                        @QueryParam("asc") @DefaultValue("false") boolean asc) {
        return groupEnrollmentRequestRepository.groupEnrollmentPager(user.getId(), groupId, groupName, status, new PagerParameters(first, max, order, asc ? "asc" : "desc"));
    }

    @POST
    @Path("/enroll-request")
    @Consumes("application/json")
    public Response createEnrollmentRequest(GroupEnrollmentRequestRepresentation rep) throws UnsupportedEncodingException {
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        if (configuration == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if (groupEnrollmentRequestRepository.countOngoingByUserAndGroup(user.getId(), configuration.getGroup().getId()) > 0)
            throw new BadRequestException("You have an ongoing request to become member of this group");

        if (configuration.getRequireApproval()) {
            GroupEnrollmentRequestEntity entity = groupEnrollmentRequestRepository.create(rep, user.getId(), configuration, true);
            //email to group admins if they must accept it
            //find thems based on group
            groupAdminRepository.getAllAdminIdsGroupUsers(configuration.getGroup().getId()).forEach(adminId -> {
                try {
                    UserModel admin = session.users().getUserById(realm, adminId);
                    if (admin != null) {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendGroupAdminEnrollmentCreationEmail(user, configuration.getGroup().getName(), rep.getGroupRoles(), rep.getComments(), entity.getId());
                    }
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }

            });
        } else {
            //user become immediately group member
            userGroupMembershipExtensionRepository.createOrUpdate(rep, session, user, clientConnection);
            groupEnrollmentRequestRepository.create(rep, user.getId(), configuration, false);
        }
        return Response.noContent().build();
    }

    @Path("/enroll-request/{id}")
    public UserGroupEnrollmentRequestAction groupEnrollment(@PathParam("id") String id) {

        GroupEnrollmentRequestEntity entity = groupEnrollmentRequestRepository.getEntity(id);
        if (entity == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if (!entity.getUser().getId().equals(user.getId()))
            throw new ForbiddenException("You do not have access to this group enrollment");

        UserGroupEnrollmentRequestAction service = new UserGroupEnrollmentRequestAction(session, realm, groupEnrollmentConfigurationRepository, groupEnrollmentRequestRepository, user, entity);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/invitation/{id}")
    @Produces("application/json")
    public GroupInvitationRepresentation getInvitation(@PathParam("id") String id) {
        GroupInvitationEntity entity = groupInvitationRepository.getEntity(id);
        if (entity == null) {
            throw new NotFoundException(INVITATION_NOT_EXISTS);
        }
        return EntityToRepresentation.toRepresentation(entity, realm);
    }

    @POST
    @Path("/invitation/{id}/accept")
    @Produces("application/json")
    @Consumes("application/json")
    public Response acceptInvitation(@PathParam("id") String id) {
        GroupInvitationEntity invitationEntity = groupInvitationRepository.getEntity(id);
        if (invitationEntity == null) {
            throw new NotFoundException(INVITATION_NOT_EXISTS);
        }
        if (invitationEntity.getForMember() && userGroupMembershipExtensionRepository.getByUserAndGroup(invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId(), user.getId()) != null) {
            throw new BadRequestException("You are already member of this group");
        }
        if (!invitationEntity.getForMember() && groupAdminRepository.getGroupAdminByUserAndGroup(user.getId(), invitationEntity.getGroup().getId()) != null) {
            throw new BadRequestException("You are already group admin for this group");
        }

        if (invitationEntity.getForMember()) {
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            userGroupMembershipExtensionRepository.create(invitationEntity, user, session.getContext().getUri(), memberUserAttribute, clientConnection);
        } else {
            groupAdminRepository.addGroupAdmin(user.getId(), invitationEntity.getGroup().getId());
        }
        List<String> groupRoles = invitationEntity.getGroupRoles() != null ? invitationEntity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()) : new ArrayList<>();
        groupAdminRepository.getAllAdminIdsGroupUsers(invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId() : invitationEntity.getGroup().getId()).map(userId -> session.users().getUserById(realm, userId)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendAcceptInvitationEmail(user, invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getName() : invitationEntity.getGroup().getName(), invitationEntity.getForMember(), groupRoles);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        });
        groupInvitationRepository.deleteEntity(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/invitation/{id}/reject")
    @Produces("application/json")
    @Consumes("application/json")
    public Response rejectInvitation(@PathParam("id") String id) {
        GroupInvitationEntity invitationEntity = groupInvitationRepository.getEntity(id);
        if (invitationEntity == null) {
            throw new NotFoundException(INVITATION_NOT_EXISTS);
        }

        List<String> groupRoles = invitationEntity.getGroupRoles() != null ? invitationEntity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()) : new ArrayList<>();
        groupInvitationRepository.deleteEntity(id);
        // rejection email
        groupAdminRepository.getAllAdminIdsGroupUsers(invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId() : invitationEntity.getGroup().getId()).map(userId -> session.users().getUserById(realm, userId)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendRejectionInvitationEmail(user, invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getName() : invitationEntity.getGroup().getName(), invitationEntity.getForMember(), groupRoles);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        });
        return Response.noContent().build();
    }

    @GET
    @Path("/configuration/{id}")
    @Produces("application/json")
    public GroupEnrollmentConfigurationRepresentation geGroupEnrollmentConfiguration(@PathParam("id") String id) {
        GroupEnrollmentConfigurationEntity entity = groupEnrollmentConfigurationRepository.getEntity(id);
        if (entity == null || !entity.isActive())
            throw new NotFoundException("This configuration does not exists or is disabled");
        return EntityToRepresentation.toRepresentation(entity, true, realm);
    }

}
