package org.rciam.plugins.groups.jpa.entities;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.*;
import org.keycloak.models.jpa.entities.GroupEntity;

@Entity
@Table(name = "GROUP_ENROLLMENT_CONFIGURATION")
@NamedQueries({
        @NamedQuery(name = "getAdminGroups", query = "select g from GroupEnrollmentConfigurationEntity g, UserGroupMembershipExtensionEntity m where m.group.id = g.id and m.user.id = :userId and m.isAdmin = true"),
        @NamedQuery(name = "getByGroup", query = "select g from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId"),
        @NamedQuery(name = "getAvailableByGroup", query = "select g from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId and g.active = true and g.visibleToNotMembers = true"),
        @NamedQuery(name = "deleteEnrollmentConfigurationByGroup", query = "delete from GroupEnrollmentConfigurationEntity g where g.group.id = :groupId"),
        @NamedQuery(name = "deleteGroupAupByEnrollmentConfiguration", query = "delete FROM GroupAupEntity g where g.id in (select ge.id FROM GroupEnrollmentConfigurationEntity ge WHERE ge.group.id = :groupId)"),
        @NamedQuery(name="countGroupIdsByNameContaining", query="select count(u) from GroupEntity u where u.realm = :realm and lower(u.name) like lower(concat('%',:search,'%'))"),
        @NamedQuery(name="countGroupIdsByName", query="select count(u) from GroupEntity u where u.realm = :realm and u.name = :search")
})
public class GroupEnrollmentConfigurationEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    @Column(name = "NAME")
    private String name;

    @Column(name = "ACTIVE")
    private Boolean active;

    @OneToOne(mappedBy = "groupEnrollmentConfiguration", cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private GroupAupEntity aupEntity;

    @Column(name = "REQUIRE_APPROVAL")
    private Boolean requireApproval;

    @Column(name = "REQUIRE_APPROVAL_FOR_EXTENSION")
    private Boolean requireApprovalForExtension;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "COMMENTS_NEEDED")
    private Boolean commentsNeeded;

    @Column(name = "COMMENTS_LABEL")
    private String commentsLabel;

    @Column(name = "COMMENTS_DESCRIPTION")
    private String commentsDescription;

    @Column(name = "MEMBERSHIP_EXPIRATION_DAYS")
    private Long membershipExpirationDays;

    @Column(name = "ENROLLMENT_INTRODUCTION")
    private String enrollmentIntroduction;

    @Column(name = "INVITATION_INTRODUCTION")
    private String invitationIntroduction;

    @Column(name = "ENROLLMENT_CONCLUSION")
    private String enrollmentConclusion;

    @Column(name = "INVITATION_CONCLUSION")
    private String invitationConclusion;

    @Column(name = "VISIBLE_TO_NOT_MEMBERS")
    private Boolean visibleToNotMembers;

    @Column(name = "MULTISELECT_ROLE")
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

    public Boolean getCommentsNeeded() {
        return commentsNeeded;
    }

    public void setCommentsNeeded(Boolean commentsNeeded) {
        this.commentsNeeded = commentsNeeded;
    }

    public String getCommentsLabel() {
        return commentsLabel;
    }

    public void setCommentsLabel(String commentsLabel) {
        this.commentsLabel = commentsLabel;
    }

    public String getCommentsDescription() {
        return commentsDescription;
    }

    public void setCommentsDescription(String commentsDescription) {
        this.commentsDescription = commentsDescription;
    }
}
