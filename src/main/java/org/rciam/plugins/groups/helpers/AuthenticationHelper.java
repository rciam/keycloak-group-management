package org.rciam.plugins.groups.helpers;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;

public class AuthenticationHelper {

    private static final Logger logger = Logger.getLogger(AuthenticationHelper.class);

    private KeycloakSession session;
    private ClientConnection clientConnection;
    private RealmModel realm;

    public AuthenticationHelper(KeycloakSession session){
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
    }



    /**
     * This snippet is a modification of AdminRoot.authenticateRealmAdminRequest()
     */
    public AuthenticationManager.AuthResult authenticateUserRequest() {
        HttpHeaders headers = session.getContext().getRequestHeaders();
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }

        return authResult;
    }


    /**
     * This snippet is from AdminRoot.authenticateRealmAdminRequest()
     */
    public AdminPermissionEvaluator authenticateRealmAdminRequest() {

        AuthenticationManager.AuthResult authResult = authenticateUserRequest();
        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient());
        var realmAuth = AdminPermissions.evaluator(session, realm, adminAuth);
        return realmAuth;
    }

}
