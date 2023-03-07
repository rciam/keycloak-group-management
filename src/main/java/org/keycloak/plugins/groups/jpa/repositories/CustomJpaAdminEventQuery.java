package org.keycloak.plugins.groups.jpa.repositories;

import javax.persistence.EntityManager;

import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.jpa.JpaAdminEventQuery;

public class CustomJpaAdminEventQuery extends JpaAdminEventQuery {

    public CustomJpaAdminEventQuery(EntityManager em) {
        super(em);
    }

    public AdminEventQuery forUser(String username) {
        return this;
    }
}
