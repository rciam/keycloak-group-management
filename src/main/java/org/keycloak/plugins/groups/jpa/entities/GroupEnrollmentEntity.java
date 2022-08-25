package org.keycloak.plugins.groups.jpa.entities;

import org.hibernate.annotations.BatchSize;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;

@Entity
@Table(name="GROUP_ENROLLMENT")
@NamedQueries({
        @NamedQuery(name="getAllUserGroupEnrollments", query="from GroupEnrollmentEntity ge where ge.user.id = :userId")
})
public class GroupEnrollmentEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

//    @Column(name="USER_ID")
    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    protected UserEntity user;

//    @Column(name="GROUP_ID")
    @ManyToOne()
    @JoinColumn(name = "GROUP_ID")
    protected GroupEntity group;

    @BatchSize(size = 50)
    @OneToMany(
//            cascade = CascadeType.REMOVE,
//            orphanRemoval = true,
            mappedBy="fb")

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
