package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;

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
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.theme.FreeMarkerUtil;

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
    //TODO Add real url
    private static final String ADD_ADMIN_URL = "http://localhost:8080/realms/master/agm/dummy";

    public GroupAdminGroup(KeycloakSession session, RealmModel realm, UserModel voAdmin, GroupModel group) {
        this.session = session;
        this.realm = realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
        this.groupRolesRepository = new GroupRolesRepository(session, realm, new GroupEnrollmentRequestRepository(session, realm, null), userGroupMembershipExtensionRepository, new GroupInvitationRepository(session, realm), groupEnrollmentConfigurationRepository);
        this.groupEnrollmentConfigurationRepository.setGroupRolesRepository(this.groupRolesRepository);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
    }

    @GET
    @Path("/configuration/all")
    @Produces("application/json")
    public List<GroupEnrollmentConfigurationRepresentation> getGroupEnrollmentConfigurationsByGroup() {
       return groupEnrollmentConfigurationRepository.getByGroup(group.getId()).map(conf -> EntityToRepresentation.toRepresentation(conf,false)).collect(Collectors.toList());
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
    @Path("/role/{id}")
    public Response deleteGroupRole(@PathParam("id") String id) {
        groupRolesRepository.delete(id);
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
        GroupAdminGroupMember service = new GroupAdminGroupMember(session, realm, voAdmin, userGroupMembershipExtensionRepository, group, customFreeMarkerEmailTemplateProvider, member);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/admin")
    public Response inviteGroupAdmin(UserRepresentation userRep) throws EmailException {
        if (userRep.getEmail() == null || userRep.getFirstName() == null || userRep.getLastName() == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("Required user fields have not submitted").build();

        UserAdapter user = Utils.getDummyUser(userRep);

        customFreeMarkerEmailTemplateProvider.setUser(user);
        customFreeMarkerEmailTemplateProvider.sendInviteGroupAdminEmail(user.getFirstName()+" "+user.getLastName(),group.getName(), ADD_ADMIN_URL);

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
