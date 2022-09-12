package org.keycloak.plugins.groups.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.theme.FreeMarkerUtil;

public class VoAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private final GroupConfigurationRepository groupConfigurationRepository;
    private final UserVoGroupMembershipRepository userVoGroupMembershipRepository;

    public VoAdminService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.voAdmin = authHelper.authenticateUserRequest().getUser();
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, session.getContext().getRealm());
        this.userVoGroupMembershipRepository =  new UserVoGroupMembershipRepository(session, session.getContext().getRealm());
    }

    @GET
    public List<GroupConfigurationRepresentation> getVoAdminGroups(){
        return groupConfigurationRepository.getVoAdminGroups(voAdmin.getId()).map(entity -> EntityToRepresentation.toRepresentation(entity,realm)).collect(Collectors.toList());
    }

    @Path("/group/{groupId}")
    public VoAdminGroup group(@PathParam("groupId") String groupId) {
        if (!userVoGroupMembershipRepository.isVoAdmin(groupId, voAdmin.getId())){
            throw new ForbiddenException();
        }
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        VoAdminGroup service = new VoAdminGroup(session, realm, voAdmin, groupConfigurationRepository, userVoGroupMembershipRepository, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
