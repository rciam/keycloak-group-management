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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_ENROLLMENT_CONFIGURATION")
@NamedQueries({
        @NamedQuery(name="getAdminGroups", query="select g from GroupEnrollmentConfigurationEntity g, UserGroupMembershipEntity m where m.group.id = g.id and m.user.id = :userId and m.isAdmin = true"),
        @NamedQuery(name="getByGroup", query="select g from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId")
})
public class GroupEnrollmentConfigurationEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    protected GroupEntity group;

    @Column(name="NAME")
    protected String name;

    @Column(name="ACTIVE")
    protected Boolean active;

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

    @Column(name="ENROLLMENT_INTRODUCTION")
    protected String enrollmentIntroduction;

    @Column(name="INVITATION_INTRODUCTION")
    protected String invitationIntroduction;

    @Column(name="ENROLLMENT_CONCLUSION")
    protected String enrollmentConclusion;

    @Column(name="INVITATION_CONCLUSION")
    protected String invitationConclusion;

    @Column(name="HIDE_CONFIGURATION")
    protected Boolean hideConfiguration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public String getEnrollmentIntroduction() {
        return enrollmentIntroduction;
    }

    public void setEnrollmentIntroduction(String enrollmentIntroduction) {
        this.enrollmentIntroduction = enrollmentIntroduction;
    }

    public String getInvitationIntroduction() {
        return invitationIntroduction;
    }

    public void setInvitationIntroduction(String invitationIntroduction) {
        this.invitationIntroduction = invitationIntroduction;
    }

    public String getEnrollmentConclusion() {
        return enrollmentConclusion;
    }

    public void setEnrollmentConclusion(String enrollmentConclusion) {
        this.enrollmentConclusion = enrollmentConclusion;
    }

    public String getInvitationConclusion() {
        return invitationConclusion;
    }

    public void setInvitationConclusion(String invitationConclusion) {
        this.invitationConclusion = invitationConclusion;
    }

    public Boolean isHideConfiguration() {
        return hideConfiguration;
    }

    public void setHideConfiguration(Boolean hideConfiguration) {
        this.hideConfiguration = hideConfiguration;
    }
}
