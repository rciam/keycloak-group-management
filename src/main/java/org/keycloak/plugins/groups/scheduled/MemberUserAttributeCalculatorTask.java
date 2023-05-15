package org.keycloak.plugins.groups.scheduled;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.timer.ScheduledTask;

public class MemberUserAttributeCalculatorTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(MemberUserAttributeCalculatorTask.class);

    private final String realmId;

    public MemberUserAttributeCalculatorTask(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm, new GroupEnrollmentConfigurationRepository(session, realm), new GroupRolesRepository(session, realm));
        MemberUserAttributeConfigurationEntity memberUserAttributeEntity = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());

        logger.info("Strarting calculating " + memberUserAttributeEntity.getUserAttribute() + " for all " + realm.getName() + " realm users.");

        session.users().getUsersStream(realm).forEach(user -> {
            if (user.getGroupsCount() > 0) {
                List<String> attributeValues = new ArrayList<>();
                userGroupMembershipExtensionRepository.getActiveByUser(user.getId()).forEach(member -> {
                    try {
                        String groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
                        if (member.getGroupRoles() == null || member.getGroupRoles().isEmpty()) {
                            attributeValues.add(Utils.createMemberUserAttribute(groupName, null, memberUserAttributeEntity.getUrnNamespace(), memberUserAttributeEntity.getAuthority()));
                        } else {
                            attributeValues.addAll(member.getGroupRoles().stream().map(role -> {
                                try {
                                    return Utils.createMemberUserAttribute(groupName, role.getName(), memberUserAttributeEntity.getUrnNamespace(), memberUserAttributeEntity.getAuthority());
                                } catch (UnsupportedEncodingException e) {
                                    throw new RuntimeException(e);
                                }
                            }).collect(Collectors.toList()));
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                });
                user.setAttribute(memberUserAttributeEntity.getUserAttribute(), attributeValues);
            } else {
                user.removeAttribute(memberUserAttributeEntity.getUserAttribute());
            }
        });

        logger.info("Finish calculating " + memberUserAttributeEntity.getUserAttribute() + " for all " + realm.getName() + " realm users.");
    }
}
