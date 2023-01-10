package org.keycloak.plugins.groups.jpa;

import javax.transaction.Transactional;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;

public class GeneralJpaService {

    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupEnrollmentRepository groupEnrollmentRepository;

    public GeneralJpaService (KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupAdminRepository groupAdminRepository){
        this.realm =  realm;
        this.groupEnrollmentConfigurationRepository =  groupEnrollmentConfigurationRepository;
        this.groupAdminRepository =  groupAdminRepository;
        this.userGroupMembershipExtensionRepository =  new UserGroupMembershipExtensionRepository(session, session.getContext().getRealm());
        this.groupEnrollmentRepository =  new GroupEnrollmentRepository(session, session.getContext().getRealm());
    }

    @Transactional
    public void removeGroup(GroupModel group){
        //extra delete UserGroupMembershipExtensionEntity, GroupEnrollmentConfigurationEntity, GroupAdminEntity, GroupEnrollmentEntity
        groupEnrollmentRepository.deleteByGroup(group.getId());
        userGroupMembershipExtensionRepository.deleteByGroup(group.getId());
        groupEnrollmentConfigurationRepository.deleteByGroup(group.getId());
        groupAdminRepository.deleteByGroup(group.getId());

        realm.removeGroup(group);
    }
}
