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

import org.keycloak.models.jpa.entities.GroupEntity;

@Entity
@Table(name="GROUP_ENROLLMENT_ATTRIBUTES")
@NamedQueries({})
public class GroupEnrollmentAttributesEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_ID")
    protected GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration;

    @Column(name="ATTRIBUTE")
    @Enumerated(EnumType.STRING)
    protected String attribute;

    @Column(name="ACTIVE")
    protected Boolean active;
}
