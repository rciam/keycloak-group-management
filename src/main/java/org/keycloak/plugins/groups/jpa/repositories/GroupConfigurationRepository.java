package org.keycloak.plugins.groups.jpa.repositories;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;

public class GroupConfigurationRepository extends GeneralRepository<GroupConfigurationEntity> {

    public GroupConfigurationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupConfigurationEntity> getTClass() {
        return GroupConfigurationEntity.class;
    }

    public void create(GroupConfigurationRepresentation rep){
        GroupConfigurationEntity entity = new GroupConfigurationEntity();
        GroupEntity group = new GroupEntity();
        group.setId(rep.getGroupId());
        entity.setGroup(group);
        entity.setDescription(rep.getDescription());
        create(entity);
    }

}
