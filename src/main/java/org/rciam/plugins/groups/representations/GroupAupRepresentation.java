package org.rciam.plugins.groups.representations;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.rciam.plugins.groups.enums.GroupAupTypeEnum;

@Schema
public class GroupAupRepresentation {

    private GroupAupTypeEnum type;
    private String mimeType;
    private Object content;

    private String url;

    public GroupAupRepresentation(){}

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
