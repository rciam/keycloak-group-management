package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.GroupAupTypeEnum;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="GROUP_AUP")
public class GroupAupEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="TYPE")
    @Enumerated(EnumType.STRING)
    protected GroupAupTypeEnum type;

    @Column(name="MIMETYPE")
    protected String mimeType;

    @Column(name="CONTENT")
    @Lob
    protected Object content;

    @Column(name="URL")
    protected String url;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupAupTypeEnum getType() {
        return type;
    }

    public void setType(GroupAupTypeEnum type) {
        this.type = type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
