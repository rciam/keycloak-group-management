package org.rciam.plugins.groups.services;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.rciam.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

public class AccountService {

    protected KeycloakSession session;

    protected final ClientConnection clientConnection;

    private AuthenticationHelper authHelper;
    private RealmModel realm;
    private final AdminEventBuilder adminEvent;
    private final UserModel user;
    private final UserSessionModel userSession;

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
        userSession = authResult.getSession();
    }

    @Path("/user")
    public UserGroups userGroups() {
        return new UserGroups(session, realm, user, userSession, clientConnection);
    }

    @Path("/group-admin")
    public GroupAdminService groupAdminService() {
        return new GroupAdminService(session, realm, user, adminEvent, clientConnection);
    }




}
