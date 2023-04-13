package org.keycloak.plugins.groups.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.GeneralJpaService;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.keycloak.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.keycloak.plugins.groups.representations.GroupRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.timer.TimerProvider;

public class GroupAdminGroup {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GeneralJpaService generalService;
    private final AdminEventBuilder adminEvent;
    //TODO Add real url
    private static final String ADD_ADMIN_URL = "http://localhost:8080/realms/master/agm/dummy";

    public GroupAdminGroup(KeycloakSession session, RealmModel realm, UserModel voAdmin, GroupModel group, AdminEventBuilder adminEvent, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupAdminRepository groupAdminRepository) {
        this.session = session;
        this.realm = realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.groupRolesRepository = new GroupRolesRepository(session, realm, new GroupEnrollmentRequestRepository(session, realm, null), userGroupMembershipExtensionRepository, new GroupInvitationRepository(session, realm), groupEnrollmentConfigurationRepository);
        this.groupEnrollmentConfigurationRepository.setGroupRolesRepository(this.groupRolesRepository);
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        this.generalService =  new GeneralJpaService(session, realm, groupEnrollmentConfigurationRepository);
        this.adminEvent = adminEvent;
    }

    @GET
    @Path("/configuration/all")
    @Produces("application/json")
    public List<GroupEnrollmentConfigurationRepresentation> getGroupEnrollmentConfigurationsByGroup() {
       return groupEnrollmentConfigurationRepository.getByGroup(group.getId()).map(conf -> EntityToRepresentation.toRepresentation(conf,false)).collect(Collectors.toList());
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

    @GET
    @Path("/configuration/{id}")
    @Produces("application/json")
    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration(@PathParam("id") String id) {
        GroupEnrollmentConfigurationEntity groupConfiguration = groupEnrollmentConfigurationRepository.getEntity(id);
        //if not exist, group have only created from main Keycloak
        if (groupConfiguration == null) {
            throw new NotFoundException("Could not find this group configuration");
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, true);
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
                groupEnrollmentConfigurationRepository.update(entity, rep);
            } else {
                throw new NotFoundException("Could not find this group configuration");
            }
        }
        //aup change action
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

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) {
        GroupRolesEntity entity = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (entity.getGroupExtensions() != null && entity.getGroupExtensions().size() > 0 )
            throw new BadRequestException(" You can not delete this role because it is assigned in a group membership");
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
        GroupAdminGroupMembers service = new GroupAdminGroupMembers(session, realm, voAdmin, userGroupMembershipExtensionRepository, group, customFreeMarkerEmailTemplateProvider);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/member/{memberId}")
    public GroupAdminGroupMember groupMember(@PathParam("memberId") String memberId) {
        UserGroupMembershipExtensionEntity member = userGroupMembershipExtensionRepository.getEntity(memberId);
        if (member == null) {
            throw new NotFoundException("Could not find this group member");
        }
        GroupAdminGroupMember service = new GroupAdminGroupMember(session, realm, voAdmin, userGroupMembershipExtensionRepository, group, customFreeMarkerEmailTemplateProvider, member, groupRolesRepository, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/admin/invite")
    public Response inviteGroupAdmin(UserRepresentation userRep) throws EmailException {
        if (userRep.getEmail() == null)
            return ErrorResponse.error("Wrong data", Response.Status.BAD_REQUEST);

        String invitationId = groupInvitationRepository.createForAdmin(group.getId(), voAdmin.getId());
        //execute once delete invitation after "url-expiration-period" ( default 72 hours)
        TimerProvider timer = session.getProvider(TimerProvider.class);
        long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
        long interval = invitationExpirationHour * 3600 * 1000;
        timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(invitationId, realm.getId()), interval), interval, "DeleteExpiredInvitation_" + invitationId);

        try {
            UserAdapter user = Utils.getDummyUser(userRep.getEmail(), userRep.getFirstName(), userRep.getLastName());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendInviteGroupAdminEmail(invitationId, voAdmin, group.getName());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }


        return Response.noContent().build();

    }

    @POST
    @Path("/admin/{userId}")
    public Response addAsGroupAdmin(@PathParam("userId") String userId){
        groupAdminRepository.addGroupAdmin(userId, group.getId());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm, userId));
            customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), true);
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
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        } else {
            throw new NotFoundException("This admin does not exist");
        }
        return Response.noContent().build();
    }


}
