package org.rciam.plugins.groups.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.models.jpa.entities.RealmEntity;
import org.rciam.plugins.groups.enums.GroupTypeEnum;

import java.util.Objects;

@Entity
@Table(name = "GROUP_ENROLLMENT_CONFIGURATION_RULES")
@NamedQueries({
        @NamedQuery(name = "getEnrollmentConfigurationRulesByRealm", query = "from GroupEnrollmentConfigurationRulesEntity f where f.realmEntity.id = :realmId"),
        @NamedQuery(name = "getEnrollmentConfigurationRulesByRealmAndType", query = "from GroupEnrollmentConfigurationRulesEntity f where f.realmEntity.id = :realmId and f.type = :type"),
        @NamedQuery(name = "getEnrollmentConfigurationRulesByRealmAndTypeAndField", query = "from GroupEnrollmentConfigurationRulesEntity f where f.realmEntity.id = :realmId and f.type = :type  and f.field = :field")
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
    private String field;

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

    public String getField() {
        return field;
    }

    public void setField(String field) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupEnrollmentConfigurationRulesEntity that = (GroupEnrollmentConfigurationRulesEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
