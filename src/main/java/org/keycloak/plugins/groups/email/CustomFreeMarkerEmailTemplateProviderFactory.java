package org.keycloak.plugins.groups.email;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FreeMarkerUtil;

public class CustomFreeMarkerEmailTemplateProviderFactory implements EmailTemplateProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new CustomFreeMarkerEmailTemplateProvider(session, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return "customFreemarker";
    }

}