/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.plugins.groups.providers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.services.AccountService;
import org.keycloak.plugins.groups.services.AdminService;
import org.keycloak.plugins.groups.ui.UserInterfaceService;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

public class ResourcesProvider implements RealmResourceProvider {

    private static final Logger logger = Logger.getLogger(ResourcesProvider.class);

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private final RealmModel realm;

    public ResourcesProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    @Path("account")
    public AccountService getAccountService() {
        AccountService service = new AccountService(session, realm, clientConnection);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("admin")
    public AdminService getAdminService() {
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AdminPermissionEvaluator realmAuth = authHelper.authenticateRealmAdminRequest();
        AdminService service = new AdminService(session, realm, clientConnection, realmAuth);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }




    //PLEASE REMOVE THIS FUNCTION
    @Deprecated
    @Path("ui")
    public UserInterfaceService getUserInterfaceService() {
        UserInterfaceService service = new UserInterfaceService(session);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
