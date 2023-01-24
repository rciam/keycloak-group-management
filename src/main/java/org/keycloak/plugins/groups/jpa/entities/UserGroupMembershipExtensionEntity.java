package org.keycloak.plugins.groups.jpa.entities;

import java.time.LocalDate;
import java.util.Date;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;

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

@Entity
@Table(name="USER_GROUP_MEMBERSHIP_EXTENSION")
@NamedQueries({
        @NamedQuery(name="getByUserAndGroup", query="from UserGroupMembershipExtensionEntity f where f.group.id = :groupId and f.user.id = :userId"),
        @NamedQuery(name="getExpiredMemberships", query="from UserGroupMembershipExtensionEntity f where f.membershipExpiresAt < :date or f.aupExpiresAt < :date"),
        @NamedQuery(name="getExpiredMembershipsByGroup", query="from UserGroupMembershipExtensionEntity f where f.group.id = :groupId and f.membershipExpiresAt < :date or f.aupExpiresAt < :date"),
        @NamedQuery(name="deleteMembershipExtensionByGroup", query="delete from UserGroupMembershipExtensionEntity g where g.group.id = :groupId"),
        @NamedQuery(name="deleteMembershipExtensionByUser", query="delete from UserGroupMembershipExtensionEntity g where g.user.id = :userId")
})
public class UserGroupMembershipExtensionEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    protected GroupEntity group;

    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    protected UserEntity user;

    @Column(name="STATUS")
    @Enumerated(EnumType.STRING)
    protected MemberStatusEnum status;

    @ManyToOne()
    @JoinColumn(name = "CHANGED_BY")
    protected UserEntity changedBy;

    @Column(name="VALID_FROM")
    protected LocalDate validFrom;

    @Column(name="MEMBERSHIP_EXPIRES_AT")
    protected LocalDate membershipExpiresAt;

    @Column(name="AUP_EXPIRES_AT")
    protected LocalDate aupExpiresAt;

    @Column(name="JUSTIFICATION")
    protected String justification;

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

    public LocalDate getAupExpiresAt() {
        return aupExpiresAt;
    }

    public void setAupExpiresAt(LocalDate aupExpiresAt) {
        this.aupExpiresAt = aupExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

}
