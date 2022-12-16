package org.keycloak.plugins.groups.enums;

public enum EnrollmentStatusEnum {
    PENDING_APPROVAL("Pending approval"), WAITING_FOR_REPLY("Waiting for reply"), ACCEPTED("Accepted"), REJECTED("Rejected");

    private String value;

    EnrollmentStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }



}
