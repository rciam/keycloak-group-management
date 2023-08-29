package org.keycloak.plugins.groups.jpa.entities;


import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GROUP_ENROLLMENT_REQUEST")
@NamedQueries({
        @NamedQuery(name = "getAllUserGroupEnrollments", query = "from GroupEnrollmentRequestEntity ge where ge.user.id = :userId"),
        @NamedQuery(name = "getRequestsByConfigurationAndStatus", query = "from GroupEnrollmentRequestEntity f where f.groupEnrollmentConfiguration.id = :configurationId and f.status in (:status)"),
        @NamedQuery(name = "countOngoingByUserAndGroup", query = "select count(f) from GroupEnrollmentRequestEntity f, GroupEnrollmentConfigurationEntity c  where f.groupEnrollmentConfiguration.id = c.id and f.user.id = :userId and c.group.id = :groupId and f.status in (:status)"),
        @NamedQuery(name = "deleteEnrollmentByGroup", query = "delete from GroupEnrollmentRequestEntity g where g.groupEnrollmentConfiguration.id in (select conf.id from GroupEnrollmentConfigurationEntity conf where conf.group.id = :groupId)"),
        @NamedQuery(name = "deleteEnrollmentByUser", query = "delete from GroupEnrollmentRequestEntity g where g.user.id = :userId"),
        @NamedQuery(name = "updateEnrollmentByAdminUser", query = "update GroupEnrollmentRequestEntity g set g.checkAdmin = null where g.checkAdmin.id = :userId")
})
public class GroupEnrollmentRequestEntity {

    @Id
    @Column(name = "ID")
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

    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private EnrollmentRequestStatusEnum status;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "ADMIN_JUSTIFICATION")
    private String adminJustification;

    @Column(name = "REVIEW_COMMENTS")
    private String reviewComments;

    @Column(name="SUBMITTED_DATE")
    private LocalDateTime submittedDate;

    @Column(name="APPROVED_DATE")
    private LocalDateTime approvedDate;
    @ManyToMany
    @JoinTable(name = "GROUP_ENROLLMENT_ROLES", joinColumns = @JoinColumn(name = "GROUP_ENROLLMENT_ID"), inverseJoinColumns = @JoinColumn(name = "GROUP_ROLES_ID"))
    private List<GroupRolesEntity> groupRoles;

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

    public EnrollmentRequestStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnrollmentRequestStatusEnum status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAdminJustification() {
        return adminJustification;
    }

    public void setAdminJustification(String adminJustification) {
        this.adminJustification = adminJustification;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    public void setComment(String reviewComment) {
        //add reviewComment to existing reviewComments after line separator
        this.reviewComments = this.reviewComments != null ? this.reviewComments + System.lineSeparator() + reviewComment : reviewComment;
    }

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public List<GroupRolesEntity> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRolesEntity> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
