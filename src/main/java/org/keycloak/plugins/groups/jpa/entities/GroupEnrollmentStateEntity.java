package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_ENROLLMENT_STATE")
public class GroupEnrollmentStateEntity {

//    @Id
    @ManyToOne()
    @JoinColumn(name = "ENROLLMENT_ID")
    protected GroupEnrollmentEntity enrollmentEntity;

    @Column(name="STATE")
    protected String state;

    @Column(name="TIMESTAMP")
    protected Long timestamp;

    @Column(name="JUSTIFICATION")
    protected String justification;

    @ManyToOne()
    @JoinColumn(name = "USER_ID")
    protected UserEntity editor;


    public GroupEnrollmentEntity getEnrollmentEntity() {
        return enrollmentEntity;
    }

    public void setEnrollmentEntity(GroupEnrollmentEntity enrollmentEntity) {
        this.enrollmentEntity = enrollmentEntity;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public UserEntity getEditor() {
        return editor;
    }

    public void setEditor(UserEntity editor) {
        this.editor = editor;
    }
}
