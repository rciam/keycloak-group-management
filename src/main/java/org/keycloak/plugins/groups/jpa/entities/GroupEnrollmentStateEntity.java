package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="KEYCLOAK_GROUP_ENROLLMENT_STATE")
public class GroupEnrollmentStateEntity {

//    @Id
    @Column(name="GROUP_ID")
    protected String groupId;

    @Column(name="STATE")
    protected String state;

    @Column(name="TIMESTAMP")
    protected Long timestamp;

    @Column(name="JUSTIFICATION")
    protected String justification;

    @Column(name="USER_ID")
    protected String userId;



}
