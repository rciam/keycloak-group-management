package org.keycloak.plugins.groups.jpa.repositories;

import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;

public class MemberUserAttributeConfigurationRepository extends GeneralRepository<MemberUserAttributeConfigurationEntity> {

    public MemberUserAttributeConfigurationRepository(KeycloakSession session) {
        super(session, null);
    }

    public MemberUserAttributeConfigurationEntity getByRealm(String realmId){
        return em.createNamedQuery("getConfigurationByRealm", MemberUserAttributeConfigurationEntity.class).setParameter("realmId",realmId).getResultStream().findAny().orElse(null);
    }

    @Override
    protected Class<MemberUserAttributeConfigurationEntity> getTClass() {
        return MemberUserAttributeConfigurationEntity.class;
    }

}