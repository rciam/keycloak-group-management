package org.keycloak.plugins.groups.jpa.repositories;

import javax.persistence.EntityManager;
import javax.ws.rs.NotFoundException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

public abstract class GeneralRepository<T> {

    protected final RealmModel realm;
    protected final EntityManager em;

    protected abstract Class<T> getTClass();

    public GeneralRepository(KeycloakSession session, RealmModel realm) {
        this.realm = realm;
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

    public UserModel getUserModel(KeycloakSession session, UserEntity user){
        return new UserAdapter(session, realm, em, user);
    }
}
