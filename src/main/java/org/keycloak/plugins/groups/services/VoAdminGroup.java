package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
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
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.StatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.theme.FreeMarkerUtil;

public class VoAdminGroup {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final GroupConfigurationRepository groupConfigurationRepository;
    private final UserVoGroupMembershipRepository userVoGroupMembershipRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final GroupAdminRepository groupAdminRepository;

    public VoAdminGroup(KeycloakSession session, RealmModel realm, UserModel voAdmin, GroupConfigurationRepository groupConfigurationRepository, UserVoGroupMembershipRepository userVoGroupMembershipRepository, GroupModel group) {
        this.session = session;
        this.realm = realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.groupConfigurationRepository = groupConfigurationRepository;
        this.userVoGroupMembershipRepository = userVoGroupMembershipRepository;
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
    }

    @GET
    @Path("/configuration/all")
    @Produces("application/json")
    public List<GroupConfigurationRepresentation> getGroupConfigurationsByGroup() {
       return groupConfigurationRepository.getByGroup(group.getId()).map(conf -> EntityToRepresentation.toRepresentation(conf, realm)).collect(Collectors.toList());
    }

    @GET
    @Path("/configuration/{id}")
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration(@PathParam("id") String id) {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(id);
        //if not exist, group have only created from main Keycloak
        if (groupConfiguration == null) {
            throw new NotFoundException("Could not find this group configuration");
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, realm);
        }
    }

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        if (rep.getId() == null ) {
            groupConfigurationRepository.create(rep, group.getId(), voAdmin.getId());
        } else {
            GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(rep.getId());
            if (entity != null) {
                groupConfigurationRepository.update(entity, rep, voAdmin.getId());
            } else {
                throw new NotFoundException("Could not find this group configuration");
            }
        }
        //aup change action
        return Response.noContent().build();
    }

    @Path("/members")
    public VoAdminGroupMembers addGroupMember() {
        VoAdminGroupMembers service = new VoAdminGroupMembers(session, realm, voAdmin, userVoGroupMembershipRepository, group, customFreeMarkerEmailTemplateProvider);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/member/{memberId}")
    public VoAdminGroupMember addGroupMember(@PathParam("memberId") String memberId) {
        UserVoGroupMembershipEntity member = userVoGroupMembershipRepository.getEntity(memberId);
        if (member == null) {
            throw new NotFoundException("Could not find this group member");
        }
        VoAdminGroupMember service = new VoAdminGroupMember(session, realm, voAdmin, userVoGroupMembershipRepository, group, customFreeMarkerEmailTemplateProvider, member);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/admin/{userId}")
    public Response addVoAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            if (!groupAdminRepository.isVoAdmin(user.getId(), group)) {
                groupAdminRepository.addGroupAdmin(userId, group.getId());

                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), true);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(user.getUsername() + " is already group admin for the " + group.getName() + " group or one of its parent.").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ModelDuplicateException.class.equals(e.getClass()) ? "Admin has already been existed" : "Problem during admin save").build();
        }
    }

    @DELETE
    @Path("/admin/{userId}")
    public Response removeVoAdmin(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        GroupAdminEntity admin = groupAdminRepository.getGroupAdminByUserAndGroup(userId, group.getId());
        if (admin != null) {
            groupAdminRepository.deleteEntity(admin.getId());
            try {
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), false);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        } else {
            throw new NotFoundException("This admin does not exist");
        }
        return Response.noContent().build();
    }


}
