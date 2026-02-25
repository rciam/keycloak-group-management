package org.rciam.plugins.groups.jpa.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.models.jpa.entities.GroupEntity;

@Entity
@Table(name = "GROUP_ROLES")
@NamedQueries({
       @NamedQuery(name = "getGroupRolesByGroup", query = "from GroupRolesEntity f where f.group.id = :groupId"),
       @NamedQuery(name = "getGroupRolesByNameAndGroup", query = "from GroupRolesEntity f where f.group.id = :groupId and f.name = :name")
})
public class GroupRolesEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    @Column(name="NAME")
    private String name;

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<GroupEnrollmentRequestEntity> enrollments = new ArrayList<>();

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<UserGroupMembershipExtensionEntity> groupExtensions = new ArrayList<>();

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<GroupInvitationEntity> groupInvitations = new ArrayList<>();

    @ManyToMany(mappedBy = "groupRoles", fetch = FetchType.LAZY)
    private List<GroupEnrollmentConfigurationEntity> configurations = new ArrayList<>();

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

    public List<GroupEnrollmentRequestEntity> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<GroupEnrollmentRequestEntity> enrollments) {
        this.enrollments = enrollments;
    }

    public List<UserGroupMembershipExtensionEntity> getGroupExtensions() {
        return groupExtensions;
    }

    public void setGroupExtensions(List<UserGroupMembershipExtensionEntity> groupExtensions) {
        this.groupExtensions = groupExtensions;
    }

    public List<GroupInvitationEntity> getGroupInvitations() {
        return groupInvitations;
    }

    public void setGroupInvitations(List<GroupInvitationEntity> groupInvitations) {
        this.groupInvitations = groupInvitations;
    }

    public List<GroupEnrollmentConfigurationEntity> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<GroupEnrollmentConfigurationEntity> configurations) {
        this.configurations = configurations;
    }
}
