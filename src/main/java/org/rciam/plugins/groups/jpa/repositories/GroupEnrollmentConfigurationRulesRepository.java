package org.rciam.plugins.groups.jpa.repositories;

import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;

public class GroupEnrollmentConfigurationRulesRepository extends GeneralRepository<GroupEnrollmentConfigurationRulesEntity> {

    public GroupEnrollmentConfigurationRulesRepository(KeycloakSession session) {
        super(session, null);
    }

    public Stream<GroupEnrollmentConfigurationRulesEntity> getByRealm(String realmId){
        return em.createNamedQuery("getEnrollmentConfigurationRulesByRealm", GroupEnrollmentConfigurationRulesEntity.class).setParameter("realmId",realmId).getResultStream();
    }

    public Stream<GroupEnrollmentConfigurationRulesEntity> getByRealmAndType(String realmId, GroupTypeEnum type){
        return em.createNamedQuery("getEnrollmentConfigurationRulesByRealmAndType", GroupEnrollmentConfigurationRulesEntity.class).setParameter("realmId",realmId).setParameter("type",type).getResultStream();
    }

    public GroupEnrollmentConfigurationRulesEntity getByRealmAndTypeAndField(String realmId, GroupTypeEnum type, String field){
        return em.createNamedQuery("getEnrollmentConfigurationRulesByRealmAndTypeAndField", GroupEnrollmentConfigurationRulesEntity.class).setParameter("realmId",realmId).setParameter("type",type).setParameter("field",field).getResultStream().findAny().get();
    }

    @Override
    protected Class<GroupEnrollmentConfigurationRulesEntity> getTClass() {
        return GroupEnrollmentConfigurationRulesEntity.class;
    }

}