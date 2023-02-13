package org.keycloak.plugins.groups.jpa.repositories;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;

public class GroupRolesRepository extends GeneralRepository<GroupRolesEntity> {

    private GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private GroupInvitationRepository groupInvitationRepository;
    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;

    public GroupRolesRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    public GroupRolesRepository(KeycloakSession session, RealmModel realm, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupInvitationRepository groupInvitationRepository, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository) {
        super(session, realm);
        this.groupEnrollmentRequestRepository = groupEnrollmentRequestRepository;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupInvitationRepository = groupInvitationRepository;
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
    }

    @Override
    protected Class<GroupRolesEntity> getTClass() {
        return GroupRolesEntity.class;
    }

    public void create(String name, String groupId){
        GroupRolesEntity entity = new GroupRolesEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        create(entity);
    }

    public Stream<GroupRolesEntity> getGroupRolesByGroup(String groupId){
        return em.createNamedQuery("getGroupRolesByGroup").setParameter("groupId",groupId).getResultStream();
    }

    public GroupRolesEntity getGroupRolesByNameAndGroup(String name, String groupId){
        List<GroupRolesEntity>  roles = em.createNamedQuery("getGroupRolesByNameAndGroup").setParameter("name",name).setParameter("groupId",groupId).getResultList();
        return roles.isEmpty() ? null : roles.get(0);
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteRolesByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void delete(String id){
        GroupRolesEntity entity = getEntity(id);
        entity.getEnrollments().stream().forEach(x-> {
            x.getGroupRoles().removeIf(role -> id.equals(role.getId()));
            groupEnrollmentRequestRepository.update(x);
        });
        entity.getGroupExtensions().stream().forEach(x-> {
            x.getGroupRoles().removeIf(role -> id.equals(role.getId()));
            userGroupMembershipExtensionRepository.update(x);
        });
        entity.getGroupInvitations().stream().forEach(x-> {
            x.getGroupRoles().removeIf(role -> id.equals(role.getId()));
            groupInvitationRepository.update(x);
        });
        entity.getConfigurations().stream().forEach(x-> {
            x.getGroupRoles().removeIf(role -> id.equals(role.getId()));
            groupEnrollmentConfigurationRepository.update(x);
        });
        deleteEntity(id);
    }

}
