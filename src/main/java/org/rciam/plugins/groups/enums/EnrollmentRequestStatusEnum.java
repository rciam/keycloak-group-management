package org.rciam.plugins.groups.enums;

public enum EnrollmentRequestStatusEnum {
    PENDING_APPROVAL("Pending approval"), WAITING_FOR_REPLY("Waiting for reply"), ACCEPTED("Accepted"), REJECTED("Rejected"), ARCHIVED("Archived"), SELF_APPROVED("Self-approved");

    private String value;

    EnrollmentRequestStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }



}
