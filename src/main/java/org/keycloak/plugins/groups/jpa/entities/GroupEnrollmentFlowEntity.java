package org.keycloak.plugins.groups.jpa.entities;

import org.hibernate.annotations.BatchSize;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;

@Entity
@Table(name="KEYCLOAK_GROUP_ENROLLMENT_FLOW")
public class GroupEnrollmentFlowEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY)
    protected String id;

    @Column(name="GROUP_ID")
    protected String groupId;

    @Column(name="DESCRIPTION")
    protected String description;

    @BatchSize(size = 50)
    @OneToMany(
            cascade = CascadeType.REMOVE,
            orphanRemoval = true, mappedBy="group")
    protected Collection<GroupEnrollmentFlowPropsEntity> props;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<GroupEnrollmentFlowPropsEntity> getProps() {
        return props;
    }

    public void setProps(Collection<GroupEnrollmentFlowPropsEntity> props) {
        this.props = props;
    }
}
