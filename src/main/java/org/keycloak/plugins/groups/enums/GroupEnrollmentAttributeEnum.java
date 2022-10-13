package org.keycloak.plugins.groups.enums;

public enum GroupEnrollmentAttributeEnum {
    COMMENT("Comment"), VALID_FROM("Valid from"), VALID_THROUGH("Valid through"), ROLE("Role"), AFFILIATION("Affiliation");

    private String value;

    GroupEnrollmentAttributeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
