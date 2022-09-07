package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

public class GroupsService {

    private static final Logger logger = Logger.getLogger(GroupsService.class);

    protected KeycloakSession session;

    private AuthenticationHelper authHelper;
    private RealmModel realm;

    public GroupsService(KeycloakSession session,RealmModel realm ) {
        this.session = session;
        this.authHelper = new AuthenticationHelper(session);
        this.realm = realm;
    }

    @Path("/user")
    public UserGroups userGroups() {
        UserGroups service = new UserGroups(session, realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/vo-admin")
    public VoAdminService voAdminService() {
        VoAdminService service = new VoAdminService(session, realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }




}
