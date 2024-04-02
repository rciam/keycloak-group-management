package org.rciam.plugins.groups.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.rciam.plugins.groups.jpa.entities.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupManagementEntityProvider implements JpaEntityProvider {

    /**
     * This is not called anymore in the quarkus based keycloak. Please, load the entities through the beans.xml and the persistence.xml instead
     * @return
     */
    @Deprecated
    @Override
    public List<Class<?>> getEntities() {
        return Stream.of(GroupEnrollmentRequestEntity.class, GroupEnrollmentConfigurationEntity.class, GroupAupEntity.class,
                UserGroupMembershipExtensionEntity.class, GroupAdminEntity.class, GroupManagementEventEntity.class, GroupInvitationEntity.class,
                GroupRolesEntity.class, MemberUserAttributeConfigurationEntity.class, GroupEnrollmentConfigurationRulesEntity.class).collect(Collectors.toList());
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/group-management-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return GroupManagementEntityProviderFactory.ID;
    }

    @Override
    public void close() {

    }
}
