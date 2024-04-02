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

package org.rciam.plugins.groups.providers;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.GroupManagementTasks;
import org.rciam.plugins.groups.scheduled.StartUpTasks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ResourcesProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "agm";
    private static final Logger logger = Logger.getLogger(ResourcesProviderFactory.class);
    private boolean executeStartupTasks = true;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new ResourcesProvider(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

        if(executeStartupTasks) {
            logger.info("GroupManagement event listener is starting...");
            try (KeycloakSession session = factory.create()) {
                AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
                //schedule task once a day at 02.00
                long interval = 24 * 3600 * 1000;
                long delay = (LocalDate.now().plusDays(1).atTime(2, 0).atZone(ZoneId.systemDefault()).toEpochSecond() - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()) * 1000;
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new StartUpTasks(), interval), 3 * 60 * 1000, "GroupManagementOnceActions");
                timer.schedule(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new GroupManagementTasks(), interval), delay, interval, "GroupManagementActions");
            }
            executeStartupTasks = false;
        }

    }

    @Override
    public void close() {

    }

}
