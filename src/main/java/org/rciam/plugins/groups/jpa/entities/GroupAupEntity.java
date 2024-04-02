package org.rciam.plugins.groups.jpa.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.rciam.plugins.groups.enums.GroupAupTypeEnum;

@Entity
@Table(name="GROUP_AUP")
public class GroupAupEntity {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name="TYPE")
    @Enumerated(EnumType.STRING)
    private GroupAupTypeEnum type;

    @Column(name="MIMETYPE")
    private String mimeType;

    @Column(name="CONTENT")
    //Lob
    private Object content;

    @Column(name="URL")
    private String url;


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
