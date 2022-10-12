package org.keycloak.plugins.groups.services;

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
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.representations.GroupsPager;
import org.keycloak.services.ForbiddenException;

public class GroupAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private final GroupAdminRepository groupAdminRepository;

    public GroupAdminService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.voAdmin = authHelper.authenticateUserRequest().getUser();
        this.groupAdminRepository =  new GroupAdminRepository(session, session.getContext().getRealm());
    }


    @GET
    @Path("/groups")
    public GroupsPager getGroupAdminGroups(@QueryParam("first") @DefaultValue("0") Integer first,
                                           @QueryParam("max") @DefaultValue("10") Integer max){
        return groupAdminRepository.getAdminGroups(voAdmin.getId(), first, max);
    }

    @Path("/group/{groupId}")
    public GroupAdminGroup group(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        if (!groupAdminRepository.isGroupAdmin(voAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        GroupAdminGroup service = new GroupAdminGroup(session, realm, voAdmin, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
