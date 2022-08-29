package org.keycloak.plugins.groups.jpa.repositories;

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

}
