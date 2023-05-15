package org.keycloak.plugins.groups.representations;

public class MemberUserAttributeConfigurationRepresentation {

    private String id;
    private String userAttribute;
    private String urnNamespace;
    private String authority;

    public MemberUserAttributeConfigurationRepresentation() {

    }

    public MemberUserAttributeConfigurationRepresentation(String userAttribute, String urnNamespace, String authority) {
        this.userAttribute = userAttribute;
        this.urnNamespace = urnNamespace;
        this.authority = authority;
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
}
