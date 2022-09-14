package org.keycloak.plugins.groups.services;

import javax.ws.rs.Consumes;
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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.StatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
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

    public VoAdminGroup(KeycloakSession session, RealmModel realm, UserModel voAdmin, GroupConfigurationRepository groupConfigurationRepository, UserVoGroupMembershipRepository userVoGroupMembershipRepository, GroupModel group) {
        this.session = session;
        this.realm = realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.groupConfigurationRepository = groupConfigurationRepository;
        this.userVoGroupMembershipRepository = userVoGroupMembershipRepository;
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
    }

    @GET
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration() {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(group.getId());
        //if not exist, group have only created from main Keycloak
        if (groupConfiguration == null) {
            return new GroupConfigurationRepresentation(group.getId());
        } else {
            return EntityToRepresentation.toRepresentation(groupConfiguration, realm);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(group.getId());
        if (entity != null) {
            groupConfigurationRepository.update(entity, rep, voAdmin.getId());
        } else {
            //only group exists
            groupConfigurationRepository.create(rep, group.getId(), voAdmin.getId());
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


}
