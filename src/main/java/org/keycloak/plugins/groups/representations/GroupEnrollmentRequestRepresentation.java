package org.keycloak.plugins.groups.representations;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.representations.idm.UserRepresentation;

import java.time.LocalDateTime;
import java.util.List;

public class GroupEnrollmentRequestRepresentation {

    private String id;
    private UserRepresentation user;
    private UserRepresentation checkAdmin;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;
    private EnrollmentRequestStatusEnum status;
    private String comments;
    private String adminJustification;
    private String reviewComments;
    private String reason;
    private List<String> groupRoles;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= Utils.dateTimeToStringFormat)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime submittedDate;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= Utils.dateTimeToStringFormat)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime approvedDate;

    public GroupEnrollmentRequestRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public UserRepresentation getCheckAdmin() {
        return checkAdmin;
    }

    public void setCheckAdmin(UserRepresentation checkAdmin) {
        this.checkAdmin = checkAdmin;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
