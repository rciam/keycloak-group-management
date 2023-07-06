package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;

public class UserGroup {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected KeycloakSession session;
    private RealmModel realm;

    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final UserModel user;
    private final GroupModel group;

    public UserGroup(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, UserModel user, GroupModel group) {
        this.session = session;
        this.realm =  realm;
        this.groupEnrollmentConfigurationRepository =  groupEnrollmentConfigurationRepository;
        this.user = user;
        this.group = group;
    }

    @GET
    @Path("/configurations")
    @Produces("application/json")
    public List<GroupEnrollmentConfigurationRepresentation> getAvailableGroupEnrollmentConfigurationsByGroup() {
        return groupEnrollmentConfigurationRepository.getAvailableByGroup(group.getId()).map(conf -> EntityToRepresentation.toRepresentation(conf)).collect(Collectors.toList());
    }



}
