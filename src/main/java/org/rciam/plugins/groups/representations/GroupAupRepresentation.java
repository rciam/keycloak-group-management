package org.rciam.plugins.groups.representations;

import org.rciam.plugins.groups.enums.GroupAupTypeEnum;

public class GroupAupRepresentation {

    private String id;
    private GroupAupTypeEnum type;
    private String mimeType;
    private Object content;

    private String url;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
