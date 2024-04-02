package org.rciam.plugins.groups.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.keycloak.models.jpa.entities.RealmEntity;

@Entity
@Table(name = "MEMBER_USER_ATTRIBUTE_CONFIGURATION")
@NamedQueries({
        @NamedQuery(name = "getConfigurationByRealm", query = "from MemberUserAttributeConfigurationEntity f where f.realmEntity.id = :realmId")
})
public class MemberUserAttributeConfigurationEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "USER_ATTRIBUTE")
    private String userAttribute;

    @Column(name = "URN_NAMESPACE")
    private String urnNamespace;

    @Column(name = "AUTHORITY")
    private String authority ;

    @Column(name="SIGNATURE_MESSAGE")
    private String signatureMessage;

    @OneToOne()
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realmEntity;

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

    public RealmEntity getRealmEntity() {
        return realmEntity;
    }

    public void setRealmEntity(RealmEntity realmEntity) {
        this.realmEntity = realmEntity;
    }
}
