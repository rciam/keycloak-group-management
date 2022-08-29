package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.GroupEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_CONFIGURATION")
public class GroupConfigurationEntity {

    @Id
    @Column(name="GROUP_ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

//    @PrimaryKeyJoinColumn
//    @OneToOne(mappedBy="group")
//    protected GroupEntity group;

    @Column(name="DESCRIPTION")
    protected String description;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "AUP_ID")
    protected GroupAupEntity aupEntity;

    @Column(name="REQUIRE_AUP_ACCEPTANCE")
    protected Boolean requireAupAcceptance;

    @Column(name="REQUIRE_APPROVAL")
    protected Boolean requireApproval;

    @Column(name="AUP_EXPIRY_SEC")
    protected Long aupExpirySec;

    @Column(name="MEMBERSHIP_EXPIRATION_SEC")
    protected Long membershipExpirationSec;
//
//    public GroupEntity getGroup() {
//        return group;
//    }
//
//    public void setGroup(GroupEntity group) {
//        this.group = group;
//    }

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

    public Long getAupExpirySec() {
        return aupExpirySec;
    }

    public void setAupExpirySec(Long aupExpirySec) {
        this.aupExpirySec = aupExpirySec;
    }

    public Long getMembershipExpirationSec() {
        return membershipExpirationSec;
    }

    public void setMembershipExpirationSec(Long membershipExpirationSec) {
        this.membershipExpirationSec = membershipExpirationSec;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
