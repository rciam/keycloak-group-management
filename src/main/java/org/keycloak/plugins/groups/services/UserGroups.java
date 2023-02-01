package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.email.EmailException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentPager;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupInvitationRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected final KeycloakSession session;
    private final RealmModel realm;

    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRepository groupEnrollmentRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final UserModel user;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final AdminEventBuilder adminEvent;

    public UserGroups(KeycloakSession session, RealmModel realm, UserModel user, AdminEventBuilder adminEvent)  {
        this.session = session;
        this.realm =  realm;
        this.user = user;
        this.adminEvent = adminEvent;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentRepository =  new GroupEnrollmentRepository(session, realm);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.groupAdminRepository =  new GroupAdminRepository(session, realm);
        this.groupInvitationRepository =  new GroupInvitationRepository(session, realm);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
    }



    @GET
    @Path("/groups")
    @Produces("application/json")
    public Response getAllUserGroups() {
        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g-> ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(userGroups).build();
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
    public GroupEnrollmentPager getMyEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                 @QueryParam("max") @DefaultValue("10") Integer max,
                                                 @QueryParam("groupName") String groupName,
                                                 @QueryParam("status") EnrollmentStatusEnum status) {
        return groupEnrollmentRepository.groupEnrollmentPager(user.getId(), groupName, status, first, max);
    }

    @POST
    @Path("/enroll-request")
    @Consumes("application/json")
    public Response createEnrollmentRequest(GroupEnrollmentRepresentation rep) {
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        if (configuration == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if (groupEnrollmentRepository.countOngoingByUserAndGroup(user.getId(), configuration.getGroup().getId()) > 0)
            throw new BadRequestException("You have an ongoing request to become member of this group");

        if (configuration.getRequireApproval()) {
            GroupEnrollmentEntity entity = groupEnrollmentRepository.create(rep, user.getId());
            //email to group admins if they must accept it
            //find thems based on group
            groupAdminRepository.getAllAdminGroupUsers(configuration.getGroup().getId()).forEach(adminId -> {
                try {
                    UserModel admin = session.users().getUserById(realm, adminId);
                    if (admin != null) {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendGroupAdminEnrollmentCreationEmail(user, configuration.getGroup().getName(), rep.getReason(), entity.getId());
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
    public UserGroupEnrollmentAction groupEnrollment(@PathParam("id") String id) {

        GroupEnrollmentEntity entity = groupEnrollmentRepository.getEntity(id);
        if (entity == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        if ( !entity.getUser().getId().equals(user.getId()))
            throw new ForbiddenException("You do not have access to this group enrollment");

        UserGroupEnrollmentAction service = new UserGroupEnrollmentAction(session, realm, groupEnrollmentConfigurationRepository, groupEnrollmentRepository, user, entity);
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
        if (userGroupMembershipExtensionRepository.getByUserAndGroup(invitationEntity.getGroupEnrollmentConfiguration().getGroup().getId(), user.getId()) != null){
            throw new BadRequestException("You are already member of this group");
        }

        userGroupMembershipExtensionRepository.create(groupInvitationRepository, invitationEntity, user, adminEvent, session.getContext().getUri());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm,invitationEntity.getCheckAdmin().getId()));
            customFreeMarkerEmailTemplateProvider.sendAcceptInvitationEmail(user,invitationEntity.getGroupEnrollmentConfiguration().getGroup().getName());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

}
