package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="KEYCLOAK_GROUP_AUP")
public class GroupAupEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="MIMETYPE")
    protected String mimeType;

    @Column(name="CONTENT")
    protected Object content;

    @Column(name="EDITOR")
    protected String editor;




}
