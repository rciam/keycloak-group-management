package org.keycloak.plugins.groups.services;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class AdminGroups {

    private static final Logger logger = Logger.getLogger(AdminGroups.class);

    private KeycloakSession session;
    private final RealmModel realm;
    private AdminPermissionEvaluator realmAuth;
    private GroupModel group;
    private GroupConfigurationRepository groupConfigurationRepository;

    public AdminGroups(KeycloakSession session, AdminPermissionEvaluator realmAuth, GroupModel group,  RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.realmAuth = realmAuth;
        this.group = group;
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, session.getContext().getRealm());
  }

    @GET
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration() {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(group.getId());
        //if not exist, group have only created from main Keycloak
        if(groupConfiguration == null) {
            GroupConfigurationRepresentation rep = new GroupConfigurationRepresentation(group.getId());
            return rep;
        } else {
            GroupConfigurationRepresentation rep = EntityToRepresentation.toRepresentation(groupConfiguration);
            return rep;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        realmAuth.groups().requireManage(group);
        GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(group.getId());
        if ( entity != null) {
            entity.setDescription(rep.getDescription());
            groupConfigurationRepository.update(entity);
        } else {
            //only group exists
            groupConfigurationRepository.create(rep, group.getId());
        }
        return Response.noContent().build();
    }




}
