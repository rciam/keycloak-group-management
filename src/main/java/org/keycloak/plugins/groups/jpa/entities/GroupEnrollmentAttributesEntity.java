package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_ENROLLMENT_ATTRIBUTES")
@NamedQueries({
        @NamedQuery(name="deleteEnrollmentAttrByGroup", query="delete from GroupEnrollmentAttributesEntity g where g.enrollment.id in (select e.id from GroupEnrollmentEntity e join GroupEnrollmentConfigurationEntity conf on e.groupEnrollmentConfiguration.id = conf.id where conf.group.id = :groupId)")
})
public class GroupEnrollmentAttributesEntity {
    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_ID")
    private GroupEnrollmentEntity enrollment;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_CONFIGURATION_ATTRIBUTES_ID")
    private GroupEnrollmentConfigurationAttributesEntity configurationAttribute;

    @Column(name="VALUE")
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupEnrollmentEntity getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(GroupEnrollmentEntity enrollment) {
        this.enrollment = enrollment;
    }

    public GroupEnrollmentConfigurationAttributesEntity getConfigurationAttribute() {
        return configurationAttribute;
    }

    public void setConfigurationAttribute(GroupEnrollmentConfigurationAttributesEntity configurationAttribute) {
        this.configurationAttribute = configurationAttribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
