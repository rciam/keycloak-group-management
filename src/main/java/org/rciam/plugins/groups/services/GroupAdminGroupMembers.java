package org.rciam.plugins.groups.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.services.ForbiddenException;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.ModelToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupInvitationInitialRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupAdminGroupMembers {

    @Context
    private ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final boolean isGroupAdmin;

    public GroupAdminGroupMembers(KeycloakSession session, RealmModel realm, UserModel groupAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider,boolean isGroupAdmin) {
        this.session = session;
        this.realm =  realm;
        this.groupAdmin = groupAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm, new GroupRolesRepository(session, realm));
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupAdminRepository= new GroupAdminRepository(session, realm);
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
        this.groupRolesRepository = new GroupRolesRepository(session, realm);
        this.isGroupAdmin = isGroupAdmin;
    }

    // group invitation and process for accept it
    @POST
    @Path("/invitation")
    @Produces("application/json")
    @Consumes("application/json")
    public Response inviteUser(GroupInvitationInitialRepresentation groupInvitationInitialRep) {

        if (!isGroupAdmin) {
            throw new ForbiddenException();
        }

        if (groupInvitationInitialRep.getEmail() == null || (groupInvitationInitialRep.isWithoutAcceptance() && groupInvitationInitialRep.getGroupEnrollmentConfiguration() == null))
            throw new ErrorResponseException("Wrong data", "Wrong data", Response.Status.BAD_REQUEST);

        GroupEnrollmentConfigurationEntity conf = groupEnrollmentConfigurationRepository.getEntity(groupInvitationInitialRep.getGroupEnrollmentConfiguration().getId());
        if (conf == null)
            throw new ErrorResponseException("Wrong group enrollment configuration", "Wrong group enrollment configuration", Response.Status.BAD_REQUEST);
        String emailId = groupInvitationRepository.createForMember(groupInvitationInitialRep, groupAdmin.getId(), conf);
        //execute once delete invitation after "url-expiration-period" ( default 72 hours)
        AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
        Long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
        long interval = invitationExpirationHour * 3600 * 1000;
        timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(emailId, realm.getId()), interval), interval, "DeleteExpiredInvitation_" + emailId);


        try {
            UserAdapter user = Utils.getDummyUser(groupInvitationInitialRep.getEmail(), groupInvitationInitialRep.getFirstName(), groupInvitationInitialRep.getLastName());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendGroupInvitationEmail(groupAdmin, group.getName(), ModelToRepresentation.buildGroupPath(group), group.getFirstAttribute(Utils.DESCRIPTION), groupInvitationInitialRep.getGroupRoles(), emailId, invitationExpirationHour);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendInvitionAdminInformationEmail(user.getEmail(), true, ModelToRepresentation.buildGroupPath(group), groupAdmin, groupInvitationInitialRep.getGroupRoles());
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });

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
                                                                          @QueryParam("status") MemberStatusEnum status,
                                                                          @QueryParam("direct") @DefaultValue("true") boolean direct,
                                                                          @QueryParam("order") @DefaultValue("f.user.lastName, f.user.firstName, f.user.email") String order,
                                                                          @QueryParam("asc") @DefaultValue("true") boolean asc){
        Set<String> groupIdsList = new HashSet<>();
        groupIdsList.add(group.getId());
        if (!direct)
            groupIdsList.addAll(Utils.getAllSubgroupsIds(group));
        return userGroupMembershipExtensionRepository.searchByGroupAndSubGroups(group.getId(), groupIdsList, search, status, role, new PagerParameters(first, max, Arrays.asList(order.split(",")), asc ? "asc" : "desc"));
    }

    @POST
    @Consumes("application/json")
    public Response createMember(UserGroupMembershipExtensionRepresentation rep) throws UnsupportedEncodingException {
        boolean extendedRole = Utils.hasManageExtendedGroupsAccountRole(realm, groupAdmin);
        //validation tasks
        //1.at least one role
        //2. valid from: date cannot be in the past (unless migration)
        //3. Expiration date: date cannot be in the past
        //4. username not empty and user exist
        if (rep.getGroupRoles() == null || rep.getGroupRoles().isEmpty()) {
            throw new BadRequestException("At least one role must be existed");
        }  else if (!extendedRole && rep.getValidFrom() != null && LocalDate.now().isAfter(rep.getValidFrom())) {
            throw new BadRequestException("Valid from must not be in the past");
        } else if (rep.getMembershipExpiresAt() != null && LocalDate.now().isAfter(rep.getMembershipExpiresAt())) {
            throw new BadRequestException("Expiration date must not be in the past");
        } else  if (rep.getUser() == null || rep.getUser().getUsername() == null) {
            throw new BadRequestException("User is required");
        }

        UserModel user = session.users().getUserByUsername(realm, rep.getUser().getUsername());
        if (user == null) {
            throw new BadRequestException("User with provider username does not exist");
        }
        UserGroupMembershipExtensionEntity oldMember = userGroupMembershipExtensionRepository.getByUserAndGroup(group.getId(), user.getId());
        if (oldMember != null) {
            throw new BadRequestException("User is already member of this group");
        }

        if (rep.getValidFrom() == null) {
            rep.setValidFrom(LocalDate.now());
        }
        GroupEnrollmentConfigurationRulesEntity configurationRule = groupEnrollmentConfigurationRulesRepository.getByRealmAndTypeAndField(realm.getId(), group.getParentId() == null ? GroupTypeEnum.TOP_LEVEL : GroupTypeEnum.SUBGROUP, "membershipExpirationDays");
        if (configurationRule != null && configurationRule.getRequired() && rep.getMembershipExpiresAt() == null) {
            throw new BadRequestException("Expiration date must not be empty");
        } else if (!extendedRole && configurationRule != null && configurationRule.getMax() != null && rep.getMembershipExpiresAt().isAfter(rep.getValidFrom().plusDays(Long.valueOf(configurationRule.getMax())))) {
            throw new BadRequestException("Group membership can not be more than {} days"+configurationRule.getMax()+" days");
        }

        List<String> groupRoles = groupRolesRepository.getGroupRolesByGroup(group.getId()).map(x -> x.getName()).collect(Collectors.toList());
        if (!rep.getGroupRoles().stream().allMatch(groupRoles::contains)) {
            throw new BadRequestException("All roles must be existed in group");
        }

        String defaultConfigurationId = group.getFirstAttribute(Utils.DEFAULT_CONFIGURATION_NAME);
        GroupEnrollmentConfigurationEntity configurationEntity= groupEnrollmentConfigurationRepository.getEntity(defaultConfigurationId);
        if (configurationEntity == null) {
            throw new BadRequestException("Group default group enrollment configuration does not exist");
        } else  if (!extendedRole && configurationEntity.getAupEntity() != null) {
            throw new BadRequestException("Could not add group member with group enrollment configuration with aup");
        }

        UserGroupMembershipExtensionEntity member = userGroupMembershipExtensionRepository.create(rep, user, groupAdmin, group, defaultConfigurationId, session, clientConnection);
        String groupPath = ModelToRepresentation.buildGroupPath(group);

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendMemberCreateUserInformEmail(group.getId(), groupPath, groupAdmin, member.getValidFrom(), member.getMembershipExpiresAt(), rep.getGroupRoles());

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendMemberCreateAdminInformEmail(group.getId(), groupPath, user, groupAdmin);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().location(session.getContext().getUri().getAbsolutePathBuilder().path(member.getId().replace("members", "member")).build()).build();
    }

}
