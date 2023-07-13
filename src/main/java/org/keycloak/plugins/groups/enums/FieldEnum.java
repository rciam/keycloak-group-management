package org.keycloak.plugins.groups.enums;

public enum FieldEnum {
    AUP("aup.type"), VALID_FROM("validFrom"), MEMBERSHIP_EXPIRATION_DAYS("membershipExpirationDays");

    private String value;

    FieldEnum(String value) {
        this.value = value;
    }

    public static FieldEnum of(String value) {
        FieldEnum field = null;
        switch (value) {
            case "aup.type": field = AUP; break;
            case "validFrom": field = VALID_FROM; break;
            case "membershipExpirationDays": field = MEMBERSHIP_EXPIRATION_DAYS; break;
        }
        return field;
    }


    @Override
    public String toString() {
        return String.valueOf(value);
    }



}
