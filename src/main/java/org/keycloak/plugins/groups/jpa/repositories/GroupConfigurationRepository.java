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

    public void create(GroupConfigurationRepresentation rep, String groupId){
        GroupConfigurationEntity entity = new GroupConfigurationEntity();
        entity.setId(groupId);
        toEntity(entity, rep);
        create(entity);
    }

    public void update( GroupConfigurationEntity entity, GroupConfigurationRepresentation rep){
        toEntity(entity, rep);
        update(entity);
    }

    private void toEntity(GroupConfigurationEntity entity, GroupConfigurationRepresentation rep) {
        entity.setDescription(rep.getDescription());
        entity.setRequireApproval(rep.getRequireApproval());
        entity.setRequireAupAcceptance(rep.getRequireAupAcceptance());
        entity.setAupExpirySec(rep.getAupExpirySec());
        entity.setMembershipExpirationSec(rep.getMembershipExpirationSec());
    }

}
