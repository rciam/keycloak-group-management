package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;

import javax.ws.rs.Path;

public class AccountService {

    private static final Logger logger = Logger.getLogger(AccountService.class);

    protected KeycloakSession session;

    private AuthenticationHelper authHelper;
    private RealmModel realm;

    public AccountService(KeycloakSession session, RealmModel realm ) {
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

    @Path("/group-admin")
    public GroupAdminService voAdminService() {
        GroupAdminService service = new GroupAdminService(session, realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }




}
