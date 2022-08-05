package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;

import javax.ws.rs.Path;

public class GroupsService {

    private static final Logger logger = Logger.getLogger(GroupsService.class);

    protected KeycloakSession session;

    private AuthenticationHelper authHelper;

    public GroupsService(KeycloakSession session) {
        this.session = session;
        this.authHelper = new AuthenticationHelper(session);
    }

    @Path("/user")
    public UserGroups userGroups() {
        UserGroups service = new UserGroups(session);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/admin")
    public AdminGroups adminGroups() {
        AdminGroups service = new AdminGroups(session);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }


}
