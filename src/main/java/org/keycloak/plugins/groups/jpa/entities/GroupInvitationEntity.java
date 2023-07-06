package org.keycloak.plugins.groups.jpa.entities;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;

@Entity
@Table(name = "GROUP_INVITATION")
@NamedQueries({
        @NamedQuery(name = "getAllGroupInvitations", query = "from GroupInvitationEntity f where f.realmId= :realmId"),
        @NamedQuery(name="deleteInvitationByGroup", query="delete from GroupInvitationEntity g where g.group.id = :groupId or g.groupEnrollmentConfiguration.id in (select conf.id from GroupEnrollmentConfigurationEntity conf where conf.group.id = :groupId)")
})
public class GroupInvitationEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name="CREATION_DATE")
    private LocalDateTime creationDate;

    @ManyToOne()
    @JoinColumn(name = "CHECK_ADMIN_ID")
    private UserEntity checkAdmin;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_CONFIGURATION_ID")
    private GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    @Column(name = "FOR_MEMBER")
    private Boolean forMember;

    @Column(name = "REALM_ID")
    private String realmId;

    @ManyToMany
    @JoinTable(name = "GROUP_INVITATION_ROLES", joinColumns = @JoinColumn(name = "GROUP_INVITATION_ID"), inverseJoinColumns = @JoinColumn(name = "GROUP_ROLES_ID"))
    private List<GroupRolesEntity> groupRoles;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public UserEntity getCheckAdmin() {
        return checkAdmin;
    }

    public void setCheckAdmin(UserEntity checkAdmin) {
        this.checkAdmin = checkAdmin;
    }

    public GroupEnrollmentConfigurationEntity getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public Boolean getForMember() {
        return forMember;
    }

    public void setForMember(Boolean forMember) {
        this.forMember = forMember;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public List<GroupRolesEntity> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRolesEntity> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
