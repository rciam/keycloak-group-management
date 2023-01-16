package org.keycloak.plugins.groups.representations;

import java.util.List;

public class GroupEnrollmentConfigurationRepresentation {

    private String id;
    private org.keycloak.representations.idm.GroupRepresentation group;
    private String name;
    private Boolean active;
    private Boolean requireAupAcceptance;
    private Boolean requireApproval;
    private Long aupExpirySec;
    private Long membershipExpirationSec;
    private Integer expirationNotificationPeriod;
    private GroupAupRepresentation aup;
    private String enrollmentIntroduction;
    private String invitationIntroduction;
    private String enrollmentConclusion;
    private String invitationConclusion;
    private Boolean hideConfiguration;

    private List<GroupEnrollmentConfigurationAttributesRepresentation> attributes;

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

    public Boolean getRequireAupAcceptance() {
        return requireAupAcceptance;
    }

    public void setRequireAupAcceptance(Boolean requireAupAcceptance) {
        this.requireAupAcceptance = requireAupAcceptance;
    }

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Long getAupExpirySec() {
        return aupExpirySec;
    }

    public void setAupExpirySec(Long aupExpirySec) {
        this.aupExpirySec = aupExpirySec;
    }

    public Long getMembershipExpirationSec() {
        return membershipExpirationSec;
    }

    public void setMembershipExpirationSec(Long membershipExpirationSec) {
        this.membershipExpirationSec = membershipExpirationSec;
    }

    public Integer getExpirationNotificationPeriod() {
        return expirationNotificationPeriod;
    }

    public void setExpirationNotificationPeriod(Integer expirationNotificationPeriod) {
        this.expirationNotificationPeriod = expirationNotificationPeriod;
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

    public Boolean isHideConfiguration() {
        return hideConfiguration;
    }

    public void setHideConfiguration(Boolean hideConfiguration) {
        this.hideConfiguration = hideConfiguration;
    }

    public List<GroupEnrollmentConfigurationAttributesRepresentation> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupEnrollmentConfigurationAttributesRepresentation> attributes) {
        this.attributes = attributes;
    }
}
