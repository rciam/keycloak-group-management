package org.rciam.plugins.groups.representations;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class MemberUserAttributeConfigurationRepresentation {

    private String id;
    private String userAttribute;
    private String urnNamespace;
    private String authority;
    private String signatureMessage;

    public MemberUserAttributeConfigurationRepresentation() {

    }

    public MemberUserAttributeConfigurationRepresentation(String userAttribute, String urnNamespace, String authority, String signatureMessage) {
        this.userAttribute = userAttribute;
        this.urnNamespace = urnNamespace;
        this.signatureMessage = signatureMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserAttribute() {
        return userAttribute;
    }

    public void setUserAttribute(String userAttribute) {
        this.userAttribute = userAttribute;
    }

    public String getUrnNamespace() {
        return urnNamespace;
    }

    public void setUrnNamespace(String urnNamespace) {
        this.urnNamespace = urnNamespace;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getSignatureMessage() {
        return signatureMessage;
    }

    public void setSignatureMessage(String signatureMessage) {
        this.signatureMessage = signatureMessage;
    }
}
