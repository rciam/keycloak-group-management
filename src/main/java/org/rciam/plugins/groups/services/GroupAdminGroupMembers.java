package org.rciam.plugins.groups.services;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.ModelToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupInvitationInitialRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

public class GroupAdminGroupMembers {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;

    public GroupAdminGroupMembers(KeycloakSession session, RealmModel realm, UserModel groupAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider) {
        this.session = session;
        this.realm =  realm;
        this.groupAdmin = groupAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm, new GroupRolesRepository(session, realm));
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupAdminRepository= new GroupAdminRepository(session, realm);
    }

    // group invitation and process for accept it
    @POST
    @Path("/invitation")
    @Produces("application/json")
    @Consumes("application/json")
    public Response inviteUser(GroupInvitationInitialRepresentation groupInvitationInitialRep) {

        if (groupInvitationInitialRep.getEmail() == null || (groupInvitationInitialRep.isWithoutAcceptance() && groupInvitationInitialRep.getGroupEnrollmentConfiguration() == null))
            throw new ErrorResponseException("Wrong data","Wrong data", Response.Status.BAD_REQUEST);

        String emailId = group.getId();
        if (groupInvitationInitialRep.isWithoutAcceptance()) {
            GroupEnrollmentConfigurationEntity conf = groupEnrollmentConfigurationRepository.getEntity(groupInvitationInitialRep.getGroupEnrollmentConfiguration().getId());
            if (conf == null)
                throw new ErrorResponseException("Wrong group enrollment configuration", "Wrong group enrollment configuration", Response.Status.BAD_REQUEST);
            emailId = groupInvitationRepository.createForMember(groupInvitationInitialRep, groupAdmin.getId(),conf);
            //execute once delete invitation after "url-expiration-period" ( default 72 hours)
            AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
            long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
            long interval = invitationExpirationHour * 3600 * 1000;
            timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(emailId, realm.getId()), interval), interval, "DeleteExpiredInvitation_"+emailId);
        }

        try {
            UserAdapter user = Utils.getDummyUser(groupInvitationInitialRep.getEmail(), groupInvitationInitialRep.getFirstName(), groupInvitationInitialRep.getLastName());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendGroupInvitationEmail(groupAdmin, ModelToRepresentation.buildGroupPath(group), groupInvitationInitialRep.isWithoutAcceptance(), groupInvitationInitialRep.getGroupRoles(), emailId);

            if (groupInvitationInitialRep.isWithoutAcceptance()) {
                groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                    try {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendInvitionAdminInformationEmail(user.getEmail(), true, group.getName(), groupAdmin, groupInvitationInitialRep.getGroupRoles());
                    } catch (EmailException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }


    /**
     *
     * @param first
     * @param max
     * @param search user search
     * @param status status search
     * @return
     */
    @GET
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentationPager memberhipPager(@QueryParam("first") @DefaultValue("0") Integer first,
                                                                          @QueryParam("max") @DefaultValue("10") Integer max,
                                                                          @QueryParam("search") String search,
                                                                          @QueryParam("role") String role,
                                                                          @QueryParam("status") MemberStatusEnum status){
        return userGroupMembershipExtensionRepository.searchByGroup(group.getId(), search, status, role, first, max);
    }

}
