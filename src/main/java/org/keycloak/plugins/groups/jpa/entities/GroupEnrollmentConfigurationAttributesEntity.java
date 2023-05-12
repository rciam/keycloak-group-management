package org.keycloak.plugins.groups.jpa.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.engine.internal.Cascade;
import org.keycloak.plugins.groups.enums.GroupEnrollmentAttributeEnum;

@Entity
@Table(name="GROUP_ENROLLMENT_CONFIGURATION_ATTRIBUTES")
@NamedQueries({
       @NamedQuery(name="deleteEnrollmentConfigurationAttrByGroup", query="delete from GroupEnrollmentConfigurationAttributesEntity g where g.groupEnrollmentConfiguration.id in (select conf.id from GroupEnrollmentConfigurationEntity conf where conf.group.id = :groupId)")
})
public class GroupEnrollmentConfigurationAttributesEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_CONFIGURATION_ID")
    private GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration;

    @Column(name="ATTRIBUTE")
    @Enumerated(EnumType.STRING)
    private GroupEnrollmentAttributeEnum attribute;

    @Column(name="LABEL")
    private String label;

    @Column(name="ORDER_ATTR")
    private Integer order;

    @Column(name="DEFAULT_VALUE")
    private String defaultValue;

    @Column(name="HIDDEN")
    private Boolean hidden;

    @Column(name="MODIFIABLE")
    private Boolean modifiable;

    @OneToMany(mappedBy = "configurationAttribute", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<GroupEnrollmentRequestAttributesEntity> enrollmentAttributes = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupEnrollmentConfigurationEntity getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public GroupEnrollmentAttributeEnum getAttribute() {
        return attribute;
    }

    public void setAttribute(GroupEnrollmentAttributeEnum attribute) {
        this.attribute = attribute;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean getModifiable() {
        return modifiable;
    }

    public void setModifiable(Boolean modifiable) {
        this.modifiable = modifiable;
    }

    public List<GroupEnrollmentRequestAttributesEntity> getEnrollmentAttributes() {
        return enrollmentAttributes;
    }

    public void setEnrollmentAttributes(List<GroupEnrollmentRequestAttributesEntity> enrollmentAttributes) {
        this.enrollmentAttributes = enrollmentAttributes;
    }
}
