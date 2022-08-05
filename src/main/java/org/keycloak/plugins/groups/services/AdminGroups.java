package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;

public class AdminGroups {

    private static final Logger logger = Logger.getLogger(AdminGroups.class);

    protected KeycloakSession session;

    private AuthenticationHelper authHelper;

    public AdminGroups(KeycloakSession session) {
        this.session = session;
        this.authHelper = new AuthenticationHelper(session);
    }




}
