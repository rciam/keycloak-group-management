package org.keycloak.plugins.groups.representations;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.Column;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.keycloak.plugins.groups.helpers.Utils;

public class GroupEnrollmentConfigurationRepresentation {

    private String id;
    private org.keycloak.representations.idm.GroupRepresentation group;
    private String name;
    private Boolean active;
    private Boolean requireApprovalForExtension;
    private Boolean requireApproval;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= Utils.dateToStringFormat)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate validFrom;
    private Long membershipExpirationDays;
    private GroupAupRepresentation aup;
    private String enrollmentIntroduction;
    private String invitationIntroduction;
    private String enrollmentConclusion;
    private String invitationConclusion;
    private Boolean visibleToNotMembers;
    private Boolean multiselectRole;
    private List<String> groupRoles;

    private Boolean commentsNeeded;

    private String commentsLabel;

    private String commentsDescription;

    public GroupEnrollmentConfigurationRepresentation(String id){
        this.id = id;
    }

    public GroupEnrollmentConfigurationRepresentation(){  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public org.keycloak.representations.idm.GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(org.keycloak.representations.idm.GroupRepresentation group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Long getMembershipExpirationDays() {
        return membershipExpirationDays;
    }

    public void setMembershipExpirationDays(Long membershipExpirationDays) {
        this.membershipExpirationDays = membershipExpirationDays;
    }

    public GroupAupRepresentation getAup() {
        return aup;
    }

    public void setAup(GroupAupRepresentation aup) {
        this.aup = aup;
    }

    public String getEnrollmentIntroduction() {
        return enrollmentIntroduction;
    }

    public void setEnrollmentIntroduction(String enrollmentIntroduction) {
        this.enrollmentIntroduction = enrollmentIntroduction;
    }

    public String getInvitationIntroduction() {
        return invitationIntroduction;
    }

    public void setInvitationIntroduction(String invitationIntroduction) {
        this.invitationIntroduction = invitationIntroduction;
    }

    public String getEnrollmentConclusion() {
        return enrollmentConclusion;
    }

    public void setEnrollmentConclusion(String enrollmentConclusion) {
        this.enrollmentConclusion = enrollmentConclusion;
    }

    public String getInvitationConclusion() {
        return invitationConclusion;
    }

    public void setInvitationConclusion(String invitationConclusion) {
        this.invitationConclusion = invitationConclusion;
    }

    public Boolean isVisibleToNotMembers() {
        return visibleToNotMembers;
    }

    public void setVisibleToNotMembers(Boolean visibleToNotMembers) {
        this.visibleToNotMembers = visibleToNotMembers;
    }

    public Boolean getRequireApprovalForExtension() {
        return requireApprovalForExtension;
    }

    public void setRequireApprovalForExtension(Boolean requireApprovalForExtension) {
        this.requireApprovalForExtension = requireApprovalForExtension;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public Boolean getMultiselectRole() {
        return multiselectRole;
    }

    public void setMultiselectRole(Boolean multiselectRole) {
        this.multiselectRole = multiselectRole;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public Boolean getCommentsNeeded() {
        return commentsNeeded;
    }

    public void setCommentsNeeded(Boolean commentsNeeded) {
        this.commentsNeeded = commentsNeeded;
    }

    public String getCommentsLabel() {
        return commentsLabel;
    }

    public void setCommentsLabel(String commentsLabel) {
        this.commentsLabel = commentsLabel;
    }

    public String getCommentsDescription() {
        return commentsDescription;
    }

    public void setCommentsDescription(String commentsDescription) {
        this.commentsDescription = commentsDescription;
    }
}
