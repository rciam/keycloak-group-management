package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.keycloak.plugins.groups.representations.GroupInvitationRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected final KeycloakSession session;
    private final RealmModel realm;

    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final UserModel user;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final AdminEventBuilder adminEvent;

    public UserGroups(KeycloakSession session, RealmModel realm, UserModel user, AdminEventBuilder adminEvent)  {
        this.session = session;
        this.realm =  realm;
        this.user = user;
        this.adminEvent = adminEvent;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentRequestRepository =  new GroupEnrollmentRequestRepository(session, realm, new GroupRolesRepository(session, realm));
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm, groupEnrollmentConfigurationRepository, new GroupRolesRepository(session, realm));
        this.groupAdminRepository =  new GroupAdminRepository(session, realm);
        this.groupInvitationRepository =  new GroupInvitationRepository(session, realm);
        this.memberUserAttributeConfigurationRepository =  new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
    }



    @GET
    @Path("/groups")
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentationPager getAllUserGroups(@QueryParam("search") String search,
                                                                            @QueryParam("first") @DefaultValue("0") Integer first,
                                                                            @QueryParam("max") @DefaultValue("10") Integer max,
                                                                            @QueryParam("order") @DefaultValue("group.name") String order,
                                                                            @QueryParam("asc") @DefaultValue("true") boolean asc) {
        return userGroupMembershipExtensionRepository.userpager(user.getId(), search, first, max, order, asc ? "asc" : "desc");
    }

    @Path("/group/{groupId}")
    public UserGroup userGroup(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        UserGroup service = new UserGroup(session, realm, groupEnrollmentConfigurationRepository, user, group, customFreeMarkerEmailTemplateProvider, groupAdminRepository);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/group/{groupId}/member")
    public UserGroupMember userGroupMember(@PathParam("groupId") String groupId) {
        UserGroupMembershipExtensionEntity entity = userGroupMembershipExtensionRepository.getByUserAndGroup(groupId, user.getId());
        if (entity == null) {
            throw new NotFoundException("You are not member of this group");
        }

        UserGroupMember service = new UserGroupMember(session, realm, user, entity, customFreeMarkerEmailTemplateProvider, userGroupMembershipExtensionRepository, groupEnrollmentConfigurationRepository);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }


    @GET
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentRequestPager getMyEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                        @QueryParam("max") @DefaultValue("10") Integer max,
                                                        @QueryParam("groupName") String groupName,
                                                        @QueryParam("status") EnrollmentRequestStatusEnum status) {
        return groupEnrollmentRequestRepository.groupEnrollmentPager(user.getId(), groupName, status, first, max);
    }

    @POST
    @Path("/enroll-request")
    @Consumes("application/json")
    public Response createEnrollmentRequest(GroupEnrollmentRequestRepresentation rep) {
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        if (configuration == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if (groupEnrollmentRequestRepository.countOngoingByUserAndGroup(user.getId(), configuration.getGroup().getId()) > 0)
            throw new BadRequestException("You have an ongoing request to become member of this group");

        if (configuration.getRequireApproval()) {
            GroupEnrollmentRequestEntity entity = groupEnrollmentRequestRepository.create(rep, user.getId(), configuration);
            //email to group admins if they must accept it
            //find thems based on group
            groupAdminRepository.getAllAdminIdsGroupUsers(configuration.getGroup().getId()).forEach(adminId -> {
                try {
                    UserModel admin = session.users().getUserById(realm, adminId);
                    if (admin != null) {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendGroupAdminEnrollmentCreationEmail(user, configuration.getGroup().getName(), rep.getGroupRoles(), rep.getReason(), entity.getId());
                    }
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }

            });
        } else {
            //user become immediately group member
            userGroupMembershipExtensionRepository.createOrUpdate(rep, session, user, adminEvent);
        }
        return Response.noContent().build();
    }

    @Path("/enroll-request/{id}")
    public UserGroupEnrollmentRequestAction groupEnrollment(@PathParam("id") String id) {

        GroupEnrollmentRequestEntity entity = groupEnrollmentRequestRepository.getEntity(id);
        if (entity == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if ( !entity.getUser().getId().equals(user.getId()))
            throw new ForbiddenException("You do not have access to this group enrollment");

        UserGroupEnrollmentRequestAction service = new UserGroupEnrollmentRequestAction(session, realm, groupEnrollmentConfigurationRepository, groupEnrollmentRequestRepository, user, entity);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/invitation/{id}")
    @Produces("application/json")
    public GroupInvitationRepresentation getInvitation(@PathParam("id") String id) {
        GroupInvitationEntity entity =  groupInvitationRepository.getEntity(id);
        if ( entity == null ) {
            throw new NotFoundException("This invitation does not exist or has been expired");
        }
        return EntityToRepresentation.toRepresentation(entity);
    }

    @POST
    @Path("/invitation/{id}/accept")
    @Produces("application/json")
    @Consumes("application/json")
    public Response acceptInvitation(@PathParam("id") String id) {
        GroupInvitationEntity invitationEntity =  groupInvitationRepository.getEntity(id);
        if ( invitationEntity == null ) {
            throw new NotFoundException("This invitation does not exist or has been expired");
        }
        if (invitationEntity.getForMember() && userGroupMembershipExtensionRepository.getByUserAndGroup(invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId(), user.getId()) != null){
            throw new BadRequestException("You are already member of this group");
        }
        if (!invitationEntity.getForMember() && groupAdminRepository.getGroupAdminByUserAndGroup(user.getId(), invitationEntity.getGroup().getId()) != null){
            throw new BadRequestException("You are already group admin for this group");
        }

        if (invitationEntity.getForMember() ) {
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            userGroupMembershipExtensionRepository.create(groupInvitationRepository, invitationEntity, user, adminEvent, session.getContext().getUri(), memberUserAttribute);
        } else {
            groupAdminRepository.addGroupAdmin(user.getId(), invitationEntity.getGroup().getId());
        }
        groupAdminRepository.getAllAdminIdsGroupUsers(invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId() : invitationEntity.getGroup().getId()).map(userId -> session.users().getUserById(realm, userId)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendAcceptInvitationEmail(user, invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getName() : invitationEntity.getGroup().getName(), invitationEntity.getForMember());
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        });
        return Response.noContent().build();
    }

    @POST
    @Path("/invitation/{id}/reject")
    @Produces("application/json")
    @Consumes("application/json")
    public Response rejectInvitation(@PathParam("id") String id) {
        GroupInvitationEntity invitationEntity =  groupInvitationRepository.getEntity(id);
        if ( invitationEntity == null ) {
            throw new NotFoundException("This invitation does not exist or has been expired");
        }

        groupInvitationRepository.deleteEntity(id);
            // rejection email
        groupAdminRepository.getAllAdminIdsGroupUsers(invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId() : invitationEntity.getGroup().getId()).map(userId -> session.users().getUserById(realm, userId)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendRejectionInvitationEmail(user, invitationEntity.getForMember() ? invitationEntity.getGroupEnrollmentConfiguration().getGroup().getName() : invitationEntity.getGroup().getName(), invitationEntity.getForMember());
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        });
        return Response.noContent().build();
    }

}
