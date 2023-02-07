package org.keycloak.plugins.groups.representations;

import java.util.List;

public class GroupInvitationInitialRepresentation {

    private String email;
    private String firstName;
    private String lastName;
    private boolean withoutAcceptance;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;

    private List<String> groupRoles;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isWithoutAcceptance() {
        return withoutAcceptance;
    }

    public void setWithoutAcceptance(boolean withoutAcceptance) {
        this.withoutAcceptance = withoutAcceptance;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
