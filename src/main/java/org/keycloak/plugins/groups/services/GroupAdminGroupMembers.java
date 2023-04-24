package org.keycloak.plugins.groups.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupInvitationInitialRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

public class GroupAdminGroupMembers {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;

    public GroupAdminGroupMembers(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm, new GroupRolesRepository(session, realm));
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
    }

    // group invitation and process for accept it
    @POST
    @Path("/invitation")
    @Produces("application/json")
    @Consumes("application/json")
    public Response inviteUser(GroupInvitationInitialRepresentation groupInvitationInitialRep) {

        if (groupInvitationInitialRep.getEmail() == null || (groupInvitationInitialRep.isWithoutAcceptance() && groupInvitationInitialRep.getGroupEnrollmentConfiguration() == null))
            return ErrorResponse.error("Wrong data", Response.Status.BAD_REQUEST);

        String emailId = group.getId();
        if (groupInvitationInitialRep.isWithoutAcceptance()) {
            GroupEnrollmentConfigurationEntity conf = groupEnrollmentConfigurationRepository.getEntity(groupInvitationInitialRep.getGroupEnrollmentConfiguration().getId());
            if ( conf == null)
                return ErrorResponse.error("Wrong group enrollment configuration", Response.Status.BAD_REQUEST);
            emailId = groupInvitationRepository.createForMember(groupInvitationInitialRep, voAdmin.getId(),conf);
            //execute once delete invitation after "url-expiration-period" ( default 72 hours)
            TimerProvider timer = session.getProvider(TimerProvider.class);
            long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
            long interval = invitationExpirationHour * 3600 * 1000;
            timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(emailId, realm.getId()), interval), interval, "DeleteExpiredInvitation_"+emailId);
        }

        try {
            UserAdapter user = Utils.getDummyUser(groupInvitationInitialRep.getEmail(), groupInvitationInitialRep.getFirstName(), groupInvitationInitialRep.getLastName());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendGroupInvitationEmail(voAdmin, group.getName(), groupInvitationInitialRep.isWithoutAcceptance(), groupInvitationInitialRep.getGroupRoles(), emailId);
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
