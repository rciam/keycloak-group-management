package org.keycloak.plugins.groups.representations;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.keycloak.plugins.groups.enums.StatusEnum;
import org.keycloak.representations.idm.UserRepresentation;

public class UserGroupMembershipRepresentation {

    protected String id;
    protected String groupId;
    protected UserRepresentation user;
    protected StatusEnum status;
    protected String changedByUserId;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    protected Date membershipExpiresAt;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    protected Date aupExpiresAt;
    protected String justification;

    public UserGroupMembershipRepresentation(){}

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

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public String getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(String changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public Date getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(Date membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public Date getAupExpiresAt() {
        return aupExpiresAt;
    }

    public void setAupExpiresAt(Date aupExpiresAt) {
        this.aupExpiresAt = aupExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

}
