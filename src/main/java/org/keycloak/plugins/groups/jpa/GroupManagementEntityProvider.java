package org.keycloak.plugins.groups.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.jpa.entities.GroupAupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentFlowEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentFlowPropsEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentStateEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupManagementEntityProvider implements JpaEntityProvider {

    private KeycloakSession session;

    public GroupManagementEntityProvider(KeycloakSession session){
        this.session = session;
    }

    @Override
    public List<Class<?>> getEntities() {
        System.out.println("GroupManagementEntityProvider: LOADING THE ENTITIES");
        return Arrays.asList(
                GroupAupEntity.class,
                GroupConfigurationEntity.class,
                GroupEnrollmentEntity.class,
                GroupEnrollmentFlowEntity.class,
                GroupEnrollmentFlowPropsEntity.class,
                GroupEnrollmentStateEntity.class
        );
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
