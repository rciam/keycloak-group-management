package org.keycloak.plugins.groups.jpa.repositories;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.jpa.entities.EduPersonEntitlementConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;

public class EduPersonEntitlementConfigurationRepository extends GeneralRepository<EduPersonEntitlementConfigurationEntity> {

    public EduPersonEntitlementConfigurationRepository(KeycloakSession session) {
        super(session, null);
    }

    public EduPersonEntitlementConfigurationEntity getByRealm(String realmId){
        return em.createNamedQuery("getConfigurationByRealm", EduPersonEntitlementConfigurationEntity.class).setParameter("realmId",realmId).getResultStream().findAny().orElse(null);
    }

    @Override
    protected Class<EduPersonEntitlementConfigurationEntity> getTClass() {
        return EduPersonEntitlementConfigurationEntity.class;
    }

}