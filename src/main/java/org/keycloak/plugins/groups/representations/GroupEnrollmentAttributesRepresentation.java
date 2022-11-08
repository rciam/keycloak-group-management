package org.keycloak.plugins.groups.representations;

public class GroupEnrollmentAttributesRepresentation {

    private String id;
    private String value;
    private GroupEnrollmentConfigurationAttributesRepresentation configurationAttribute;

    public GroupEnrollmentAttributesRepresentation(){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public GroupEnrollmentConfigurationAttributesRepresentation getConfigurationAttribute() {
        return configurationAttribute;
    }

    public void setConfigurationAttribute(GroupEnrollmentConfigurationAttributesRepresentation configurationAttribute) {
        this.configurationAttribute = configurationAttribute;
    }
}
