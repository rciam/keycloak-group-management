package org.rciam.plugins.groups.representations;

import org.rciam.plugins.groups.enums.GroupTypeEnum;

public class GroupEnrollmentConfigurationRulesRepresentation {

    private String id;
    private String field;
    private GroupTypeEnum type;
    private String defaultValue;
    private String max;
    private Boolean required;

    public GroupEnrollmentConfigurationRulesRepresentation (){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public GroupTypeEnum getType() {
        return type;
    }

    public void setType(GroupTypeEnum type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
