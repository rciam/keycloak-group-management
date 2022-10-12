package org.keycloak.plugins.groups.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.List;

public class GroupManagementEntityProvider implements JpaEntityProvider {

    private KeycloakSession session;

    public GroupManagementEntityProvider(KeycloakSession session){
        this.session = session;
    }

    /**
     * This is not called anymore in the quarkus based keycloak. Please, load the entities through the beans.xml and the persistence.xml instead
     * @return
     */
    @Deprecated
    @Override
    public List<Class<?>> getEntities() {
        return new ArrayList<>();
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
