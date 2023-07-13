package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.FieldEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRulesRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class AdminEnrollmentConfigurationRules {

    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final AdminEventBuilder adminEvent;

    public AdminEnrollmentConfigurationRules(RealmModel realm, KeycloakSession session, AdminEventBuilder adminEvent){
        this.realm = realm;
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
        this.adminEvent =  adminEvent;
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
        entity.setField(FieldEnum.of(rep.getField()));
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
        entity.setField(FieldEnum.of(rep.getField()));
        entity.setDefaultValue(rep.getDefaultValue());
        entity.setMax(rep.getMax());
        entity.setRequired(rep.getRequired());
        groupEnrollmentConfigurationRulesRepository.update(entity);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GroupEnrollmentConfigurationRulesRepresentation getConfigureRule( @PathParam("id") String id) {
        GroupEnrollmentConfigurationRulesEntity entity = groupEnrollmentConfigurationRulesRepository.getEntity(id);
        if (entity == null) {
            throw new NotFoundException("Could not find GroupEnrollmentConfigurationRules by id");
        }
        return EntityToRepresentation.toRepresentation(entity);
    }
}
