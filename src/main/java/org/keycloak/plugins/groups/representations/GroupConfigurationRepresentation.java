package org.keycloak.plugins.groups.representations;

import javax.persistence.Column;

public class GroupConfigurationRepresentation {

    private String id;
    private String groupId;
    private String description;
    private Boolean requireAupAcceptance;
    private Boolean requireApproval;
    private Long aupExpirySec;
    private Long membershipExpirationSec;
    private GroupAupRepresentation aup;

    public GroupConfigurationRepresentation(String id){
        this.id = id;
    }

    public GroupConfigurationRepresentation(){  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public GroupAupRepresentation getAup() {
        return aup;
    }

    public void setAup(GroupAupRepresentation aup) {
        this.aup = aup;
    }
}
