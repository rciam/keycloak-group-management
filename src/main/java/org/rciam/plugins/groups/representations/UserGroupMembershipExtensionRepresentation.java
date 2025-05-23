package org.rciam.plugins.groups.representations;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.Utils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;

public class UserGroupMembershipExtensionRepresentation {

    private String id;
    private GroupRepresentation group;
    private UserRepresentation user;
    private MemberStatusEnum status;
    private String changedByUserId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.dateToStringFormat)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate validFrom;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.dateToStringFormat)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate membershipExpiresAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.dateToStringFormat)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate effectiveMembershipExpiresAt;
    private String effectiveGroupId;
    private String justification;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;
    private Set<String> groupRoles;

    private boolean direct;

    public UserGroupMembershipExtensionRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(GroupRepresentation group) {
        this.group = group;
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

    public LocalDate getEffectiveMembershipExpiresAt() {
        return effectiveMembershipExpiresAt;
    }

    public void setEffectiveMembershipExpiresAt(LocalDate effectiveMembershipExpiresAt) {
        this.effectiveMembershipExpiresAt = effectiveMembershipExpiresAt;
    }

    public String getEffectiveGroupId() {
        return effectiveGroupId;
    }

    public void setEffectiveGroupId(String effectiveGroupId) {
        this.effectiveGroupId = effectiveGroupId;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public Set<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(Set<String> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }
}
