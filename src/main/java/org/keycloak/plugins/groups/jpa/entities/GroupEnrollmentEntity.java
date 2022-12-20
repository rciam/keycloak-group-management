package org.keycloak.plugins.groups.jpa.entities;


import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name="GROUP_ENROLLMENT")
@NamedQueries({
        @NamedQuery(name="getAllUserGroupEnrollments", query="from GroupEnrollmentEntity ge where ge.user.id = :userId"),
        @NamedQuery(name="countOngoingByUserAndGroup", query="select count(f) from GroupEnrollmentEntity f, GroupEnrollmentConfigurationEntity c  where f.groupEnrollmentConfiguration.id = c.id and f.user.id = :userId and c.group.id = :groupId and f.status in (:status)")
})
public class GroupEnrollmentEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @ManyToOne()
    @JoinColumn(name = "CHECK_ADMIN_ID")
    private UserEntity checkAdmin;

    @ManyToOne()
    @JoinColumn(name = "GROUP_ENROLLMENT_CONFIGURATION_ID")
    private GroupEnrollmentConfigurationEntity groupEnrollmentConfiguration;

    @Column(name="STATE")
    @Enumerated(EnumType.STRING)
    private EnrollmentStatusEnum status;

    @Column(name="REASON")
    private String reason;

    @Column(name="ADMIN_JUSTIFICATION")
    private String adminJustification;

    @Column(name="COMMENTS")
    private String comments;

    @OneToMany(cascade =CascadeType.ALL, orphanRemoval = true, mappedBy = "enrollment")
    private List<GroupEnrollmentAttributesEntity> attributes;

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

    public EnrollmentStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatusEnum status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdminJustification() {
        return adminJustification;
    }

    public void setAdminJustification(String adminJustification) {
        this.adminJustification = adminJustification;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setComment(String comment) {
        //add comment to existing comments after line separator
        this.comments = this.comments != null ? this.comments + System.lineSeparator()+comment : comment;
    }

    public List<GroupEnrollmentAttributesEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupEnrollmentAttributesEntity> attributes) {
        this.attributes = attributes;
    }
}
