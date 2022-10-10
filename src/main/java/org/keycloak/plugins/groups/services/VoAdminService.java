package org.keycloak.plugins.groups.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.GroupsPager;
import org.keycloak.services.ForbiddenException;

public class VoAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private final GroupAdminRepository groupAdminRepository;

    public VoAdminService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.voAdmin = authHelper.authenticateUserRequest().getUser();
        this.groupAdminRepository =  new GroupAdminRepository(session, session.getContext().getRealm());
    }


    @GET
    @Path("/groups")
    public GroupsPager getVoAdminGroups(@QueryParam("first") @DefaultValue("0") Integer first,
                                              @QueryParam("max") @DefaultValue("10") Integer max){
        return groupAdminRepository.getAdminGroups(voAdmin.getId(), first, max);
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

        VoAdminGroup service = new VoAdminGroup(session, realm, voAdmin, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
