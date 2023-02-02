package org.keycloak.plugins.groups.jpa.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.GroupEntity;

@Entity
@Table(name = "GROUP_ROLES")
@NamedQueries({
       @NamedQuery(name = "getGroupRolesByGroup", query = "from GroupRolesEntity f where f.group.id = :groupId"),
       @NamedQuery(name = "getGroupRolesByNameAndGroup", query = "from GroupRolesEntity f where f.group.id = :groupId and f.name = :name"),
       @NamedQuery(name= "deleteRolesByGroup", query="delete from GroupRolesEntity m where m.group.id = :groupId")
})
public class GroupRolesEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    protected GroupEntity group;

    @Column(name="NAME")
    protected String name;

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<GroupEnrollmentEntity> enrollments = new ArrayList<>();

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<UserGroupMembershipExtensionEntity> groupExtensions = new ArrayList<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GroupEnrollmentEntity> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<GroupEnrollmentEntity> enrollments) {
        this.enrollments = enrollments;
    }

    public List<UserGroupMembershipExtensionEntity> getGroupExtensions() {
        return groupExtensions;
    }

    public void setGroupExtensions(List<UserGroupMembershipExtensionEntity> groupExtensions) {
        this.groupExtensions = groupExtensions;
    }
}
