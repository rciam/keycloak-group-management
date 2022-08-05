package org.keycloak.plugins.groups.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.jpa.entities.DummyEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupManagementEntityProvider implements JpaEntityProvider {

    private KeycloakSession session;

    public GroupManagementEntityProvider(KeycloakSession session){
        this.session = session;
    }

    @Override
    public List<Class<?>> getEntities() {
        return Collections.<Class<?>>singletonList(DummyEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/group-management-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return GroupManagementEntityProviderFactory.ID;
    }

    @Override
    public void close() {

    }
}
