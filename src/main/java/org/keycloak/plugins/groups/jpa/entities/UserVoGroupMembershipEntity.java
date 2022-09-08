package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="USER_VO_GROUP_MEMBERSHIP")
@NamedQueries({
        @NamedQuery(name="getByUserAndGroup", query="from UserVoGroupMembershipEntity f where f.group.id = :groupId and f.user.id = :userId"),
        @NamedQuery(name="countVoAdmin", query="select count(f) from UserVoGroupMembershipEntity f where f.group.id = :groupId and f.user.id = :userId and f.isAdmin = true")
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
    protected String status;

    @ManyToOne()
    @JoinColumn(name = "CHANGED_BY")
    protected UserEntity changedBy;

    @Column(name="MEMBERSHIP_EXPIRES_AT")
    protected Long membershipExpiresAt;

    @Column(name="AUP_EXPIRES_AT")
    protected Long aupExpiresAt;

    @Column(name="JUSTIFICATION")
    protected String justification;

    @Column(name="IS_ADMIN")
    protected Boolean isAdmin;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UserEntity changedBy) {
        this.changedBy = changedBy;
    }

    public Long getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(Long membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public Long getAupExpiresAt() {
        return aupExpiresAt;
    }

    public void setAupExpiresAt(Long aupExpiresAt) {
        this.aupExpiresAt = aupExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
