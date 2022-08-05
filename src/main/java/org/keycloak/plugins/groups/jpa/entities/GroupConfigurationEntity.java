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


    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GroupEnrollmentFlowEntity getEnrollmentFlow() {
        return enrollmentFlow;
    }

    public void setEnrollmentFlow(GroupEnrollmentFlowEntity enrollmentFlow) {
        this.enrollmentFlow = enrollmentFlow;
    }

    public GroupAupEntity getAupEntity() {
        return aupEntity;
    }

    public void setAupEntity(GroupAupEntity aupEntity) {
        this.aupEntity = aupEntity;
    }
}
