package org.keycloak.plugins.groups.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;

@Entity
@Table(name = "EDU_PERSON_ENTITLEMENT_CONFIGURATION")
@NamedQueries({
        @NamedQuery(name = "getConfigurationByRealm", query = "from EduPersonEntitlementConfigurationEntity f where f.realmEntity.id = :realmId")
})
public class EduPersonEntitlementConfigurationEntity {

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

    public RealmEntity getRealmEntity() {
        return realmEntity;
    }

    public void setRealmEntity(RealmEntity realmEntity) {
        this.realmEntity = realmEntity;
    }
}
