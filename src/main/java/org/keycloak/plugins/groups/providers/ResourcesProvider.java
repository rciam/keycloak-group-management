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
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.services.AdminGroups;
import org.keycloak.plugins.groups.services.AccountService;
import org.keycloak.plugins.groups.ui.UserInterfaceService;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ResourcesProvider implements RealmResourceProvider {

    private static final Logger logger = Logger.getLogger(ResourcesProvider.class);

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;

    public ResourcesProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, session.getContext().getRealm());
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
        AccountService service = new AccountService(session, realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @Path("/admin/group/{groupId}")
    public AdminGroups adminGroups(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AdminPermissionEvaluator realmAuth = authHelper.authenticateRealmAdminRequest();
        realmAuth.groups().requireView(group);
        AdminGroups service = new AdminGroups(session, realmAuth, group, realm);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @POST
    @Path("/admin/group")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTopLevelGroup(GroupRepresentation rep) {
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        AdminPermissionEvaluator realmAuth = authHelper.authenticateRealmAdminRequest();
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, realmAuth.adminAuth(), session, clientConnection);
        adminEvent.realm(realm).resource(ResourceType.REALM);
        GroupsResource groupsResource = new GroupsResource(realm, session, realmAuth,adminEvent);
        Response response = groupsResource.addTopLevelGroup(rep);
        logger.info("group have been created with status"+response.getStatus());
        if (response.getStatus() >= 400) {
            //error response from client creation
            return response;
        } else if (groupEnrollmentConfigurationRepository.getEntity(rep.getId()) == null) {
            //group creation - group configuration no exist
            logger.info("Create group with groupId === "+rep.getId());
            groupEnrollmentConfigurationRepository.createDefault(rep.getId(), rep.getName());
        }
        //if rep.getId() != null => mean that group has been moved( not created)
        logger.info("group configuration exists ==== "+rep.getId());

        return Response.noContent().build();
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
