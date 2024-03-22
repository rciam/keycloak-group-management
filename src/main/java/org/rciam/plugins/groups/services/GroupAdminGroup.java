package org.rciam.plugins.groups.services;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
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
    private final GeneralJpaService generalService;
    private final AdminEventBuilder adminEvent;
    //based on this boolean, do not allow manage-groups users to do specific actions
    private final boolean isGroupAdmin;

    public GroupAdminGroup(KeycloakSession session, RealmModel realm, UserModel groupAdmin, GroupModel group, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupAdminRepository groupAdminRepository, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, AdminEventBuilder adminEvent, boolean isGroupAdmin) {
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
        if (!isGroupAdmin){
            throw new ForbiddenException();
        }
        List<String> groupAdminsIds = groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).collect(Collectors.toList());
        generalService.removeGroup(group);
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
        return groupEnrollmentConfigurationRepository.getByGroup(group.getId()).map(x-> EntityToRepresentation.toRepresentation(x, false, realm)).collect(Collectors.toList());
    }

    @GET
    @Path("/all")
    @Produces("application/json")
    public GroupRepresentation getAllGroupInfo() {
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
        } else if (entity == null ) {
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
        if (!isGroupAdmin){
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
            customFreeMarkerEmailTemplateProvider.sendInviteGroupAdminEmail(invitationId, groupAdmin, group.getName(), org.rciam.plugins.groups.helpers.ModelToRepresentation.buildGroupPath(group), group.getFirstAttribute(Utils.DESCRIPTION));

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
    @Path("/admin/{userId}")
    public Response addAsGroupAdmin(@PathParam("userId") String userId) {
        if (groupAdminRepository.getGroupAdminByUserAndGroup(userId, group.getId()) != null) {
            throw new BadRequestException("You are already group admin for this group");
        }
        groupAdminRepository.addGroupAdmin(userId, group.getId());

        try {
            UserModel userAdded = session.users().getUserById(realm, userId);
            customFreeMarkerEmailTemplateProvider.setUser(userAdded);
            customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), true);
            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x) && !userId.equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendAddRemoveAdminAdminInformationEmail(true, group.getName(), userAdded, groupAdmin);
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
    @Path("/admin/{userId}")
    public Response removeGroupAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        GroupAdminEntity admin = groupAdminRepository.getGroupAdminByUserAndGroup(userId, group.getId());
        if (admin != null) {
            groupAdminRepository.deleteEntity(admin.getId());
            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), false);
                groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(a -> {
                    try {
                        customFreeMarkerEmailTemplateProvider.setUser(a);
                        customFreeMarkerEmailTemplateProvider.sendAddRemoveAdminAdminInformationEmail(false, group.getName(), user, groupAdmin);
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
