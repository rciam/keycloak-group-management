package org.keycloak.plugins.groups.representations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.representations.idm.UserRepresentation;

public class UserGroupMembershipExtensionRepresentation {

    protected String id;
    protected String groupId;
    protected UserRepresentation user;
    protected MemberStatusEnum status;
    protected String changedByUserId;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    protected LocalDate validFrom;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    protected LocalDate membershipExpiresAt;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    protected LocalDate aupExpiresAt;
    protected String justification;

    public UserGroupMembershipExtensionRepresentation(){}

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

    public MemberStatusEnum getStatus() {
        return status;
    }

    public void setStatus(MemberStatusEnum status) {
        this.status = status;
    }

    public String getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(String changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(LocalDate membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public LocalDate getAupExpiresAt() {
        return aupExpiresAt;
    }

    public void setAupExpiresAt(LocalDate aupExpiresAt) {
        this.aupExpiresAt = aupExpiresAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

}
