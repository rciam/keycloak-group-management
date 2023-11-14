package org.rciam.plugins.groups.services;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.rciam.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

public class AccountService {

    protected KeycloakSession session;

    @Context
    protected ClientConnection clientConnection;

    private AuthenticationHelper authHelper;
    private RealmModel realm;
    private final AdminEventBuilder adminEvent;
    private final UserModel user;

    public AccountService(KeycloakSession session, RealmModel realm,ClientConnection clientConnection ) {
        this.session = session;
        this.authHelper = new AuthenticationHelper(session);
        this.realm = realm;
        this.clientConnection = clientConnection;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AuthenticationManager.AuthResult authResult = authHelper.authenticateUserRequest();
        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient());
        this.adminEvent =  new AdminEventBuilder(realm, adminAuth, session, clientConnection);
        adminEvent.realm(realm);
        user = authResult.getUser();
    }

    @Path("/user")
    public UserGroups userGroups() {
        UserGroups service = new UserGroups(session, realm, user);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/group-admin")
    public GroupAdminService groupAdminService() {
        GroupAdminService service = new GroupAdminService(session, realm, user, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }




}
