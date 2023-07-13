package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.plugins.groups.enums.FieldEnum;
import org.keycloak.plugins.groups.enums.GroupTypeEnum;

@Entity
@Table(name = "GROUP_ENROLLMENT_CONFIGURATION_RULES")
@NamedQueries({
        @NamedQuery(name = "getEnrollmentConfigurationRulesByRealm", query = "from GroupEnrollmentConfigurationRulesEntity f where f.realmEntity.id = :realmId"),
        @NamedQuery(name = "getEnrollmentConfigurationRulesByRealmAndType", query = "from GroupEnrollmentConfigurationRulesEntity f where f.realmEntity.id = :realmId and f.type = :type")
})
public class GroupEnrollmentConfigurationRulesEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realmEntity;

    @Column(name = "FIELD")
    @Enumerated(EnumType.STRING)
    private FieldEnum field;

    @Column(name = "TYPE")
    @Enumerated(EnumType.STRING)
    private GroupTypeEnum type;

    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

    @Column(name = "MAX")
    private String max;

    @Column(name = "REQUIRED")
    private Boolean required;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealmEntity() {
        return realmEntity;
    }

    public void setRealmEntity(RealmEntity realmEntity) {
        this.realmEntity = realmEntity;
    }

    public FieldEnum getField() {
        return field;
    }

    public void setField(FieldEnum field) {
        this.field = field;
    }

    public GroupTypeEnum getType() {
        return type;
    }

    public void setType(GroupTypeEnum type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
