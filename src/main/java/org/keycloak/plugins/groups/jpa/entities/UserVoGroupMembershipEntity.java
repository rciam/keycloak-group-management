package org.keycloak.plugins.groups.jpa.entities;

import java.util.Date;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.StatusEnum;

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
@Table(name="USER_VO_GROUP_MEMBERSHIP")
@NamedQueries({
        @NamedQuery(name="getByUserAndGroup", query="from UserVoGroupMembershipEntity f where f.group.id = :groupId and f.user.id = :userId")
})
public class UserVoGroupMembershipEntity {

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
    protected StatusEnum status;

    @ManyToOne()
    @JoinColumn(name = "CHANGED_BY")
    protected UserEntity changedBy;

    @Column(name="MEMBERSHIP_EXPIRES_AT")
    protected Date membershipExpiresAt;

    @Column(name="AUP_EXPIRES_AT")
    protected Date aupExpiresAt;

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

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UserEntity changedBy) {
        this.changedBy = changedBy;
    }

    public Date getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(Date membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public Date getAupExpiresAt() {
        return aupExpiresAt;
    }

    public void setAupExpiresAt(Date aupExpiresAt) {
        this.aupExpiresAt = aupExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

}
