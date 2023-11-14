package org.rciam.plugins.groups.enums;

public enum GroupEnrollmentAttributeEnum {
    COMMENT("Comment"), VALID_FROM("Valid from");

    private String value;

    GroupEnrollmentAttributeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
