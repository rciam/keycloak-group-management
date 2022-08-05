package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="KEYCLOAK_GROUP_ENROLLMENT_FLOW_PROPS")
public class GroupEnrollmentFlowPropsEntity {

    @Column(name="FLOW_ID")
    private String flowId;

    @Column(name="PROPERTY")
    private String property;

    @Column(name="VALUE")
    private String value;



    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
