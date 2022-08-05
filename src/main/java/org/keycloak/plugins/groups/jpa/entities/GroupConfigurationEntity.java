package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.GroupEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="KEYCLOAK_GROUP_CONFIGURATION")
public class GroupConfigurationEntity {

//    @Id
    @Column(name="GROUP_ID")
    @OneToOne(mappedBy="group")
    protected GroupEntity group;

    @Column(name="DESCRIPTION")
    protected String description;

//    @Column(name="ENROLLMENT_FLOW")
    @ManyToOne()
    @JoinColumn(name = "ENROLLMENT_FLOW")
    protected GroupEnrollmentFlowEntity enrollmentFlow;

//    @Column(name="AUP_ID")
    @ManyToOne()
    @JoinColumn(name = "AUP_ID")
    protected GroupAupEntity aupEntity;




}
