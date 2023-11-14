package org.rciam.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRulesRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class AdminEnrollmentConfigurationRules {

    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final AdminEventBuilder adminEvent;

    public AdminEnrollmentConfigurationRules(RealmModel realm, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
        this.adminEvent = adminEvent;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupEnrollmentConfigurationRulesRepresentation> getEnrollmentConfigurationRules() {
        return groupEnrollmentConfigurationRulesRepository.getByRealm(realm.getId()).map(EntityToRepresentation::toRepresentation).collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configureRule(GroupEnrollmentConfigurationRulesRepresentation rep) {
        GroupEnrollmentConfigurationRulesEntity entity = new GroupEnrollmentConfigurationRulesEntity();
        entity.setId(KeycloakModelUtils.generateId());
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setId(realm.getId());
        entity.setRealmEntity(realmEntity);
        entity.setType(rep.getType());
        entity.setField(rep.getField());
        entity.setDefaultValue(rep.getDefaultValue());
        entity.setMax(rep.getMax());
        entity.setRequired(rep.getRequired());
        groupEnrollmentConfigurationRulesRepository.create(entity);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfigureRule(GroupEnrollmentConfigurationRulesRepresentation rep, @PathParam("id") String id) {
        GroupEnrollmentConfigurationRulesEntity entity = groupEnrollmentConfigurationRulesRepository.getEntity(id);
        if (entity == null) {
            throw new NotFoundException("Could not find GroupEnrollmentConfigurationRules by id");
        }
        entity.setType(rep.getType());
        entity.setField(rep.getField());
        entity.setDefaultValue(rep.getDefaultValue());
        entity.setMax(rep.getMax());
        entity.setRequired(rep.getRequired());
        groupEnrollmentConfigurationRulesRepository.update(entity);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GroupEnrollmentConfigurationRulesRepresentation getConfigureRule(@PathParam("id") String id) {
        GroupEnrollmentConfigurationRulesEntity entity = groupEnrollmentConfigurationRulesRepository.getEntity(id);
        if (entity == null) {
            throw new NotFoundException("Could not find GroupEnrollmentConfigurationRules by id");
        }
        return EntityToRepresentation.toRepresentation(entity);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteConfigureRule(@PathParam("id") String id) {
        groupEnrollmentConfigurationRulesRepository.deleteEntity(id);
        return Response.noContent().build();
    }
}
