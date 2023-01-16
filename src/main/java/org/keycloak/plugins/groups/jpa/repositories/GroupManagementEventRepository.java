package org.keycloak.plugins.groups.jpa.repositories;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;

public class GroupManagementEventRepository extends GeneralRepository<GroupManagementEventEntity> {

    public GroupManagementEventRepository(KeycloakSession session) {
        super(session, null);
    }

    @Override
    protected Class<GroupManagementEventEntity> getTClass() {
        return GroupManagementEventEntity.class;
    }

}