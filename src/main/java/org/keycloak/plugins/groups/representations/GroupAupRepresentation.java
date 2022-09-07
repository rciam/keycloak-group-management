package org.keycloak.plugins.groups.representations;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.GroupAupTypeEnum;
import org.keycloak.representations.idm.UserRepresentation;

public class GroupAupRepresentation {

    private String id;
    private GroupAupTypeEnum type;
    private String mimeType;
    private Object content;

    private String url;
    private UserRepresentation editor;

    public GroupAupRepresentation(){}

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

    public UserRepresentation getEditor() {
        return editor;
    }
    public void setEditor(UserRepresentation editor) {
        this.editor = editor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
