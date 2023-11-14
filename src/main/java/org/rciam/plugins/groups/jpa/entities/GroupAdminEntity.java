package org.rciam.plugins.groups.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;

@Entity
@Table(name="GROUP_ADMIN")
@NamedQueries({
        @NamedQuery(name="countByUserAndGroups", query="select count(f) from GroupAdminEntity f where f.user.id = :userId and f.group.id in (:groupIds)"),
        @NamedQuery(name="getAdminByUserAndGroup", query="from GroupAdminEntity f where f.user.id = :userId and f.group.id = :groupId"),
        @NamedQuery(name="getAdminsIdsForGroupIds", query="select distinct(f.user.id) from GroupAdminEntity f where f.group.id in (:groupIds)"),
        @NamedQuery(name="getAdminsForGroupIds", query="select distinct(g) from UserEntity g join GroupAdminEntity f on f.user.id = g.id where f.group.id in (:groupIds)"),
        @NamedQuery(name="getGroupsForAdmin", query="select f.group.id from GroupAdminEntity f where f.user.id = :userId"),
        @NamedQuery(name="countGroupsForAdmin", query="select count(f) from GroupAdminEntity f where f.user.id = :userId"),
        @NamedQuery(name="searchGroupsForAdmin", query="select f.group.id from GroupAdminEntity f join GroupEntity g on f.group.id = g.id where f.user.id = :userId and lower(g.name) like :search"),
        @NamedQuery(name="countSearchGroupsForAdmin", query="select count(f) from GroupAdminEntity f join GroupEntity g on f.group.id = g.id where f.user.id = :userId and lower(g.name) like :search"),
        @NamedQuery(name="getAdminsForGroup", query="from GroupAdminEntity g where g.group.id = :groupId"),
        @NamedQuery(name="deleteAdminByGroup", query="delete from GroupAdminEntity g where g.group.id = :groupId"),
        @NamedQuery(name="deleteAdminByUser", query="delete from GroupAdminEntity g where g.user.id = :userId")
})
public class GroupAdminEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }
}
