package org.rciam.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ForbiddenException;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.entities.*;
import org.rciam.plugins.groups.jpa.repositories.*;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.rciam.plugins.groups.representations.GroupRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

public class GroupAdminGroup {
    @Context
    private ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private GroupModel group;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final GeneralJpaService generalService;
    private final AdminEventBuilder adminEvent;
    //based on this boolean, do not allow manage-groups users to do specific actions
    private final boolean isGroupAdmin;


    public GroupAdminGroup(KeycloakSession session, RealmModel realm, UserModel groupAdmin, GroupModel group, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupAdminRepository groupAdminRepository, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository, AdminEventBuilder adminEvent, boolean isGroupAdmin) {
        this.session = session;
        this.realm = realm;
        this.groupAdmin = groupAdmin;
        this.group = group;
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.groupRolesRepository = new GroupRolesRepository(session, realm, new GroupEnrollmentRequestRepository(session, realm, null), userGroupMembershipExtensionRepository, new GroupInvitationRepository(session, realm), groupEnrollmentConfigurationRepository);
        this.groupEnrollmentConfigurationRepository.setGroupRolesRepository(this.groupRolesRepository);
        this.groupEnrollmentRequestRepository = groupEnrollmentRequestRepository;
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm);
        this.groupEnrollmentConfigurationRulesRepository = groupEnrollmentConfigurationRulesRepository;
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session);
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        MemberUserAttributeConfigurationEntity memberUserAttribute = (new MemberUserAttributeConfigurationRepository(session)).getByRealm(realm.getId());
        this.customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
        this.generalService = new GeneralJpaService(session, realm, groupEnrollmentConfigurationRepository);
        this.adminEvent = adminEvent;
        this.isGroupAdmin = isGroupAdmin;
    }

    @DELETE
    public void deleteGroup() {
        if (!isGroupAdmin)
            throw new ForbiddenException();
        if (group.getSubGroupsStream().count() > 0)
            throw new BadRequestException("You need firstly to delete child groups.");

        List<String> groupAdminsIds = groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).collect(Collectors.toList());
        generalService.removeGroup(group, groupAdmin, clientConnection, false);
        groupAdminsIds.stream().map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendDeleteGroupAdminInformationEmail(ModelToRepresentation.buildGroupPath(group), groupAdmin);
            } catch (EmailException e) {
                throw new RuntimeException(e);
            }
        });

        adminEvent.operation(OperationType.DELETE).representation(group.getName()).resourcePath(session.getContext().getUri()).success();
    }

    @GET
    @Path("/configuration/all")
    @Produces("application/json")
    public List<GroupEnrollmentConfigurationRepresentation> getGroupEnrollmentConfigurationsByGroup() {
        return groupEnrollmentConfigurationRepository.getByGroup(group.getId()).map(x -> EntityToRepresentation.toRepresentation(x, false, realm)).collect(Collectors.toList());
    }

    @GET
    @Path("/all")
    @Produces("application/json")
    public GroupRepresentation getAllGroupInfo() throws UnsupportedEncodingException {
        return generalService.getAllGroupInfo(group);
    }

    @POST
    @Path("/attributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveAttributes(Map<String, List<String>> attributes) {
        if (attributes != null) {
            Set<String> attrsToRemove = new HashSet<>(group.getAttributes().keySet());
            attrsToRemove.removeAll(attributes.keySet());
            for (Map.Entry<String, List<String>> attr : attributes.entrySet()) {
                group.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                group.removeAttribute(attr);
            }
        } else {
            Set<String> attrsToRemove = new HashSet<>(group.getAttributes().keySet());
            for (String attr : attrsToRemove) {
                group.removeAttribute(attr);
            }
        }
        return Response.noContent().build();
    }

    @POST
    @Path("children")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        return Utils.addGroupChild(rep, realm, group, session, adminEvent, groupEnrollmentConfigurationRepository, groupRolesRepository);
    }

    @GET
    @Path("/configuration/{id}")
    @Produces("application/json")
    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration(@PathParam("id") String id) {
        GroupEnrollmentConfigurationEntity groupConfiguration = groupEnrollmentConfigurationRepository.getEntity(id);
        //if not exist, group have only created from main Keycloak
        if (groupConfiguration == null) {
            throw new NotFoundException(Utils.NO_FOUND_GROUP_CONFIGURATION);
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, false, realm);
        }
    }

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation rep) {
        GroupEnrollmentConfigurationRulesEntity rule = groupEnrollmentConfigurationRulesRepository.getByRealmAndTypeAndField(realm.getId(), group.getParentId() != null ? GroupTypeEnum.SUBGROUP : GroupTypeEnum.TOP_LEVEL, "membershipExpirationDays");
        if (rule != null && rule.getRequired() && rep.getMembershipExpirationDays() == null) {
            throw new BadRequestException("Expiration date must not be empty");
        } else if (rule != null && rule.getMax() != null && (rep.getMembershipExpirationDays() == null || (rep.getMembershipExpirationDays() > Long.valueOf(rule.getMax())))) {
            throw new BadRequestException("Membership can not last more than "+ rule.getMax() + " days");
        }

        rule = groupEnrollmentConfigurationRulesRepository.getByRealmAndTypeAndField(realm.getId(), group.getParentId() != null ? GroupTypeEnum.SUBGROUP : GroupTypeEnum.TOP_LEVEL, "validFrom");
        if (rule != null && rule.getRequired() && rep.getValidFrom() == null) {
            throw new BadRequestException("Valid from must not be empty");
        }

        rule = groupEnrollmentConfigurationRulesRepository.getByRealmAndTypeAndField(realm.getId(), group.getParentId() != null ? GroupTypeEnum.SUBGROUP : GroupTypeEnum.TOP_LEVEL, "aup");
        if (rule != null && rule.getRequired() && rep.getAup() == null) {
            throw new BadRequestException("Aup must not be empty");
        }

        if (rep.getId() == null) {
            groupEnrollmentConfigurationRepository.create(rep, group.getId());
        } else {
            GroupEnrollmentConfigurationEntity entity = groupEnrollmentConfigurationRepository.getEntity(rep.getId());
            if (entity != null) {
                groupEnrollmentRequestRepository.getRequestsByConfigurationAndStatus(entity.getId(), Stream.of(EnrollmentRequestStatusEnum.WAITING_FOR_REPLY, EnrollmentRequestStatusEnum.PENDING_APPROVAL).collect(Collectors.toList())).forEach(request -> {
                    request.setStatus(EnrollmentRequestStatusEnum.ARCHIVED);
                    groupEnrollmentRequestRepository.update(request);
                });
                groupEnrollmentConfigurationRepository.update(entity, rep);
            } else {
                throw new NotFoundException(Utils.NO_FOUND_GROUP_CONFIGURATION);
            }
        }
        //aup change action
        return Response.noContent().build();
    }

    @DELETE
    @Path("/configuration/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteGroupEnrollmentConfiguration(@PathParam("id") String id) {
        GroupEnrollmentConfigurationEntity entity = groupEnrollmentConfigurationRepository.getEntity(id);
        if (entity != null && group.getFirstAttribute(Utils.DEFAULT_CONFIGURATION_NAME) != null) {
            groupEnrollmentRequestRepository.getRequestsByConfiguration(entity.getId()).forEach(request -> {
                groupEnrollmentRequestRepository.deleteEntity(request);
            });
            groupEnrollmentConfigurationRepository.deleteEntity(id);
        } else if (entity == null) {
            throw new NotFoundException(Utils.NO_FOUND_GROUP_CONFIGURATION);
        } else  {
            throw new BadRequestException("Could not delete default group configuration");
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/roles")
    @Produces("application/json")
    public List<String> getGroupRoles() {
        return groupRolesRepository.getGroupRolesByGroup(group.getId()).map(GroupRolesEntity::getName).collect(Collectors.toList());
    }

    @POST
    @Path("/roles")
    public Response saveGroupRole(@QueryParam("name") String name) {
        groupRolesRepository.create(name, group.getId());
        return Response.noContent().build();
    }

    @POST
    @Path("/default-configuration")
    public Response changeDefaultConfiguration(@QueryParam("configurationId") String configurationId) {
        if (groupEnrollmentConfigurationRepository.getEntity(configurationId) == null)
            throw new NotFoundException(Utils.NO_FOUND_GROUP_CONFIGURATION);
        group.setAttribute(Utils.DEFAULT_CONFIGURATION_NAME, Stream.of(configurationId).collect(Collectors.toList()));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) {
        if (!isGroupAdmin) {
            throw new ForbiddenException();
        }

        GroupRolesEntity entity = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (entity.getGroupExtensions() != null && entity.getGroupExtensions().size() > 0)
            throw new BadRequestException("You can not delete this role because it is assigned in a group membership");
        groupRolesRepository.delete(entity);
        return Response.noContent().build();
    }

    @PUT
    @Path("/role/{id}")
    public Response renameGroupRole(@PathParam("id") String id, @QueryParam("name") String name) {
        GroupRolesEntity entity = groupRolesRepository.getEntity(id);
        if (entity == null) {
            throw new NotFoundException("Could not find this group role");
        }
        entity.setName(name);
        groupRolesRepository.update(entity);
        return Response.noContent().build();
    }


    @Path("/members")
    public GroupAdminGroupMembers groupMember() {
        GroupAdminGroupMembers service = new GroupAdminGroupMembers(session, realm, groupAdmin, userGroupMembershipExtensionRepository, group, customFreeMarkerEmailTemplateProvider, isGroupAdmin);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/member/{memberId}")
    public GroupAdminGroupMember groupMember(@PathParam("memberId") String memberId) {
        UserGroupMembershipExtensionEntity member = userGroupMembershipExtensionRepository.getEntity(memberId);
        if (member == null) {
            throw new NotFoundException("Could not find this group member");
        }
        GroupAdminGroupMember service = new GroupAdminGroupMember(session, realm, groupAdmin, userGroupMembershipExtensionRepository, group, customFreeMarkerEmailTemplateProvider, member, groupRolesRepository, groupAdminRepository, isGroupAdmin);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/admin/invite")
    public Response inviteGroupAdmin(UserRepresentation userRep) throws EmailException {
        if (userRep.getEmail() == null)
            throw new ErrorResponseException("Wrong data", "Wrong data", Response.Status.BAD_REQUEST);

        String invitationId = groupInvitationRepository.createForAdmin(group.getId(), groupAdmin.getId());
        //execute once delete invitation after "url-expiration-period" ( default 72 hours)
        AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
        long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
        long interval = invitationExpirationHour * 3600 * 1000;
        timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(invitationId, realm.getId()), interval), interval, "DeleteExpiredInvitation_" + invitationId);

        try {
            UserAdapter user = Utils.getDummyUser(userRep.getEmail(), userRep.getFirstName(), userRep.getLastName());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendInviteGroupAdminEmail(invitationId, groupAdmin, group.getName(), org.rciam.plugins.groups.helpers.ModelToRepresentation.buildGroupPath(group), group.getFirstAttribute(Utils.DESCRIPTION), invitationExpirationHour);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendInvitionAdminInformationEmail(userRep.getEmail(), false, group.getName(), groupAdmin, null);
                } catch (EmailException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }


        return Response.noContent().build();

    }

    @POST
    @Path("/admin")
    public Response addAsGroupAdmin(@QueryParam("userId") String userId, @QueryParam("username") String username) {
        UserModel userAdded = userId != null ? session.users().getUserById(realm, userId) : session.users().getUserByUsername(realm, username);
        if (userAdded == null) {
            throw new NotFoundException("Could not find this User");
        }

        if (groupAdminRepository.getGroupAdminByUserAndGroup(userAdded.getId(), group.getId()) != null) {
            throw new BadRequestException("This user is already group admin of this group");
        }
        groupAdminRepository.addGroupAdmin(userAdded.getId(), group.getId());
        String groupPath = ModelToRepresentation.buildGroupPath(group);

        try {
            customFreeMarkerEmailTemplateProvider.setUser(userAdded);
            customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(true, groupPath, group.getId(), groupAdmin);
            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x) && !userAdded.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendAddRemoveAdminAdminInformationEmail(true, groupPath, group.getId(), userAdded, groupAdmin);
                } catch (EmailException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();

    }


    @DELETE
    @Path("/admin")
    public Response removeGroupAdmin(@QueryParam("userId") String userId, @QueryParam("username") String username) {
        UserModel user = userId != null ? session.users().getUserById(realm, userId) : session.users().getUserByUsername(realm, username);
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }

        GroupAdminEntity admin = groupAdminRepository.getGroupAdminByUserAndGroup(user.getId(), group.getId());
        if (admin != null) {
            groupAdminRepository.deleteEntity(admin.getId());
            String groupPath = ModelToRepresentation.buildGroupPath(group);
            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(false, groupPath, group.getId(), groupAdmin);
                groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(a -> {
                    try {
                        customFreeMarkerEmailTemplateProvider.setUser(a);
                        customFreeMarkerEmailTemplateProvider.sendAddRemoveAdminAdminInformationEmail(false, groupPath, group.getId(), user, groupAdmin);
                    } catch (EmailException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        } else {
            throw new NotFoundException("This admin does not exist");
        }
        return Response.noContent().build();
    }


}
