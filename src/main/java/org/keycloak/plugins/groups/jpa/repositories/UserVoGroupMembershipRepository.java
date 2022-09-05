package org.keycloak.plugins.groups.jpa.repositories;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;

public class UserVoGroupMembershipRepository extends GeneralRepository<UserVoGroupMembershipEntity> {

    public UserVoGroupMembershipRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<UserVoGroupMembershipEntity> getTClass() {
        return UserVoGroupMembershipEntity.class;
    }

    public UserVoGroupMembershipEntity getByUserAndGroup(String groupId, String userId){
        List<UserVoGroupMembershipEntity> results = em.createNamedQuery("getByUserAndGroup").setParameter("groupId",groupId).setParameter("userId",userId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

}
