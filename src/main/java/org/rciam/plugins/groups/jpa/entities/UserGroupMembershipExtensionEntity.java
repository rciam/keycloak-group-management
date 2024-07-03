package org.rciam.plugins.groups.jpa.entities;

import java.time.LocalDate;
import java.util.List;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.rciam.plugins.groups.enums.MemberStatusEnum;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "USER_GROUP_MEMBERSHIP_EXTENSION")
@NamedQueries({
        @NamedQuery(name = "getByUserAndGroup", query = "from UserGroupMembershipExtensionEntity f where f.group.id = :groupId and f.user.id = :userId"),
        @NamedQuery(name = "getActiveByUser", query = "from UserGroupMembershipExtensionEntity f where f.user.id = :userId and f.status = 'ENABLED'"),
        @NamedQuery(name = "getMembersByGroup", query = "from UserGroupMembershipExtensionEntity f where f.group.id = :groupId"),
        @NamedQuery(name = "getByUserAndGroups", query = "from UserGroupMembershipExtensionEntity f where f.user.id = :userId and f.group.id in (:groupIds)"),
        @NamedQuery(name = "getByUserAndGroupsAndNotSuspended", query = "from UserGroupMembershipExtensionEntity f where f.user.id = :userId and f.group.id in (:groupIds) and f.status != 'SUSPENDED'"),
        @NamedQuery(name = "getByUserAndGroupsAndSuspended", query = "from UserGroupMembershipExtensionEntity f where f.user.id = :userId and f.group.id in (:groupIds) and f.status = 'SUSPENDED'"),
        @NamedQuery(name = "getExpiredMemberships", query = "from UserGroupMembershipExtensionEntity f where f.membershipExpiresAt < :date"),
        @NamedQuery(name = "getMembershipsByStatusAndValidFrom", query = "from UserGroupMembershipExtensionEntity f where f.status = :status and f.validFrom <= :date"),
        @NamedQuery(name = "getExpiredMembershipsByGroup", query = "from UserGroupMembershipExtensionEntity f where f.group.id = :groupId and f.membershipExpiresAt < :date"),
        @NamedQuery(name = "deleteMembershipExtensionByGroup", query = "delete from UserGroupMembershipExtensionEntity g where g.group.id = :groupId"),
        @NamedQuery(name = "deleteMembershipExtensionByUser", query = "delete from UserGroupMembershipExtensionEntity g where g.user.id = :userId")
})
public class UserGroupMembershipExtensionEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private MemberStatusEnum status;

    @ManyToOne()
    @JoinColumn(name = "CHANGED_BY")
    private UserEntity changedBy;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "MEMBERSHIP_EXPIRES_AT")
    private LocalDate membershipExpiresAt;
    @Column(name = "JUSTIFICATION")
    private String justification;

    @Column(name = "GROUP_ENROLLMENT_CONFIGURATION_ID")
    private String groupEnrollmentConfigurationId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "GROUP_MEMBERSHIP_ROLES", joinColumns = @JoinColumn(name = "USER_GROUP_MEMBERSHIP_EXTENSION_ID"), inverseJoinColumns = @JoinColumn(name = "GROUP_ROLES_ID"))
    private List<GroupRolesEntity> groupRoles;

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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public MemberStatusEnum getStatus() {
        return status;
    }

    public void setStatus(MemberStatusEnum status) {
        this.status = status;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UserEntity changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(LocalDate membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getGroupEnrollmentConfigurationId() {
        return groupEnrollmentConfigurationId;
    }

    public void setGroupEnrollmentConfigurationId(String groupEnrollmentConfigurationId) {
        this.groupEnrollmentConfigurationId = groupEnrollmentConfigurationId;
    }

    public List<GroupRolesEntity> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRolesEntity> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
