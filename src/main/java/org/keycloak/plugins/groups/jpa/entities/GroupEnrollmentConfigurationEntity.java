package org.keycloak.plugins.groups.jpa.entities;

import java.time.LocalDate;
import java.util.List;

import org.keycloak.models.jpa.entities.GroupEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_ENROLLMENT_CONFIGURATION")
@NamedQueries({
        @NamedQuery(name="getAdminGroups", query="select g from GroupEnrollmentConfigurationEntity g, UserGroupMembershipExtensionEntity m where m.group.id = g.id and m.user.id = :userId and m.isAdmin = true"),
        @NamedQuery(name="getByGroup", query="select g from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId"),
        @NamedQuery(name="deleteEnrollmentConfigurationByGroup", query="delete from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId")
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

    @Column(name="REQUIRE_APPROVAL")
    protected Boolean requireApproval;

    @Column(name="REQUIRE_APPROVAL_FOR_EXTENSION")
    protected Boolean requireApprovalForExtension;

    @Column(name="VALID_FROM")
    protected LocalDate validFrom;

    @Column(name="MEMBERSHIP_EXPIRATION_DAYS")
    protected Long membershipExpirationDays;

    @Column(name="ENROLLMENT_INTRODUCTION")
    protected String enrollmentIntroduction;

    @Column(name="INVITATION_INTRODUCTION")
    protected String invitationIntroduction;

    @Column(name="ENROLLMENT_CONCLUSION")
    protected String enrollmentConclusion;

    @Column(name="INVITATION_CONCLUSION")
    protected String invitationConclusion;

    @Column(name="VISIBLE_TO_NOT_MEMBERS")
    protected Boolean visibleToNotMembers;

    @Column(name="MULTISELECT_ROLE")
    private Boolean multiselectRole;

    @ManyToMany
    @JoinTable(name = "GROUP_ENROLLMENT_CONFIGURATION_ROLES", joinColumns = @JoinColumn(name = "GROUP_ENROLLMENT_CONFIGURATION_ID"), inverseJoinColumns = @JoinColumn(name = "GROUP_ROLES_ID"))
    private List<GroupRolesEntity> groupRoles;

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

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Boolean getRequireApprovalForExtension() {
        return requireApprovalForExtension;
    }

    public void setRequireApprovalForExtension(Boolean requireApprovalForExtension) {
        this.requireApprovalForExtension = requireApprovalForExtension;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public Long getMembershipExpirationDays() {
        return membershipExpirationDays;
    }

    public void setMembershipExpirationDays(Long membershipExpirationDays) {
        this.membershipExpirationDays = membershipExpirationDays;
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

    public Boolean isVisibleToNotMembers() {
        return visibleToNotMembers;
    }

    public void setVisibleToNotMembers(Boolean visibleToNotMembers) {
        this.visibleToNotMembers = visibleToNotMembers;
    }

    public List<GroupRolesEntity> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRolesEntity> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public Boolean isMultiselectRole() {
        return multiselectRole;
    }

    public void setMultiselectRole(Boolean multiselectRole) {
        this.multiselectRole = multiselectRole;
    }
}
