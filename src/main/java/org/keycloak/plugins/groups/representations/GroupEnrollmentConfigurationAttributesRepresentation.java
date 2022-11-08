package org.keycloak.plugins.groups.representations;

import org.keycloak.plugins.groups.enums.GroupEnrollmentAttributeEnum;

public class GroupEnrollmentConfigurationAttributesRepresentation {

    private String id;
    private GroupEnrollmentAttributeEnum attribute;
    private String label;
    private Integer order;
    private String defaultValue;
    private Boolean hidden;
    private Boolean modifiable;

    public GroupEnrollmentConfigurationAttributesRepresentation(){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupEnrollmentAttributeEnum getAttribute() {
        return attribute;
    }

    public void setAttribute(GroupEnrollmentAttributeEnum attribute) {
        this.attribute = attribute;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean getModifiable() {
        return modifiable;
    }

    public void setModifiable(Boolean modifiable) {
        this.modifiable = modifiable;
    }
}
