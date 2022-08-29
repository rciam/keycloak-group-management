package org.keycloak.plugins.groups.jpa.entities;

import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

    @Column(name="MIMETYPE")
    protected String mimeType;

    @Column(name="CONTENT")
    @Lob
    protected Object content;

    @ManyToOne()
    @JoinColumn(name = "EDITOR")
    protected UserEntity editor;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public UserEntity getEditor() {
        return editor;
    }

    public void setEditor(UserEntity editor) {
        this.editor = editor;
    }
}
