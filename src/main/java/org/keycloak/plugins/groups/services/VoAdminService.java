package org.keycloak.plugins.groups.services;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.services.ForbiddenException;

public class VoAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final UserVoGroupMembershipRepository userVoGroupMembershipRepository;
    private final GroupAdminRepository groupAdminRepository;

    public VoAdminService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.voAdmin = authHelper.authenticateUserRequest().getUser();
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, session.getContext().getRealm());
        this.userVoGroupMembershipRepository =  new UserVoGroupMembershipRepository(session, session.getContext().getRealm());
        this.groupAdminRepository =  new GroupAdminRepository(session, session.getContext().getRealm());
    }

    @Path("/group/{groupId}")
    public VoAdminGroup group(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        if (!groupAdminRepository.isVoAdmin(voAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        VoAdminGroup service = new VoAdminGroup(session, realm, voAdmin, groupEnrollmentConfigurationRepository, userVoGroupMembershipRepository, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
