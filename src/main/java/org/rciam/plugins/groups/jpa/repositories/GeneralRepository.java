package org.rciam.plugins.groups.jpa.repositories;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

public abstract class GeneralRepository<T> {

    protected RealmModel realm;
    protected final EntityManager em;
    protected final KeycloakSession session;

    protected abstract Class<T> getTClass();

    public GeneralRepository(KeycloakSession session, RealmModel realm) {
        this.realm = realm;
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public T getEntity(String id) {
        return em.find(getTClass(), id);
    }

    public void create(T entity) {
        em.persist(entity);
        em.flush();
    }

    public void update(T entity) {
        em.merge(entity);
        em.flush();
    }

    public void deleteEntity(String id) throws NotFoundException {
        T entity = getEntity(id);
        if (entity == null)
            throw new NotFoundException(String.format("Realm with name %s does not have %s with id equal to %", realm.getName(),getTClass().getSimpleName(),id));
        em.remove(entity);
        em.flush();
    }

    public void deleteEntity(T entity)  {
        em.remove(entity);
        em.flush();
    }

    protected void setRealm(RealmModel realm) {
        this.realm = realm;
    }
}
