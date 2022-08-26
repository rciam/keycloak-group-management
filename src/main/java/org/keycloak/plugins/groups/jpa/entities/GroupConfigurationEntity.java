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
@Table(name="GROUP_CONFIGURATION")
public class GroupConfigurationEntity {

//    @Id
    @Column(name="GROUP_ID")
    @OneToOne(mappedBy="group")
    protected GroupEntity group;

    @Column(name="DESCRIPTION")
    protected String description;

//    @Column(name="AUP_ID")
    @ManyToOne()
    @JoinColumn(name = "AUP_ID")
    protected GroupAupEntity aupEntity;

    @Column(name="REQUIRE_AUP_ACCEPTANCE")
    protected Boolean requireAupAcceptance;

    @Column(name="REQUIRE_APPROVAL")
    protected Boolean requireApproval;

    @Column(name="AUP_EXPIRY_SEC")
    protected Long aupExpirySec;


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

    public GroupAupEntity getAupEntity() {
        return aupEntity;
    }

    public void setAupEntity(GroupAupEntity aupEntity) {
        this.aupEntity = aupEntity;
    }

    public Boolean getRequireAupAcceptance() {
        return requireAupAcceptance;
    }

    public void setRequireAupAcceptance(Boolean requireAupAcceptance) {
        this.requireAupAcceptance = requireAupAcceptance;
    }

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Long getAupExpiry() {
        return aupExpirySec;
    }

    public void setAupExpiry(Long aupExpirySec) {
        this.aupExpirySec = aupExpirySec;
    }
}
