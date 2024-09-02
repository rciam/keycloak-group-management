package org.rciam.plugins.groups.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class SubgroupsExpirationDateCalculationTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(SubgroupsExpirationDateCalculationTask.class);

    private final String realmId;
    private final String userId;
    private final String groupId;
    private final LocalDate expirationDate;


    public SubgroupsExpirationDateCalculationTask(String realmId, String userId, String groupId, LocalDate expirationDate) {
        this.realmId = realmId;
        this.userId = userId;
        this.groupId = groupId;
        this.expirationDate = expirationDate;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info("Strarting calculating subgroups membership expiration of group with id = " + groupId + " and user with id = " + userId);
        RealmModel realm = session.realms().getRealm(realmId);
        UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository (session, realm);
        GroupModel group = session.groups().getGroupById(realm, groupId);
        Set<String> subgroupIds = Utils.getAllSubgroupsIds(group);
        userGroupMembershipExtensionRepository.getByGroup(userId, subgroupIds).forEach(member -> {
            GroupModel childGroup = session.groups().getGroupById(realm, member.getGroup().getId());
            List<String> groupIds = Utils.findParentGroupIds(childGroup);
            groupIds.add(member.getGroup().getId());
            //find min expiration date based on database and current changes expirationDate
            UserGroupMembershipExtensionEntity effectiveMember = userGroupMembershipExtensionRepository.getMinExpirationDateForUserAndGroups(member.getUser().getId(), groupIds, expirationDate);
            //update child only if expiration date will change
            if (effectiveMember != null && ! effectiveMember.getMembershipExpiresAt().equals(member.getEffectiveMembershipExpiresAt())) {
                member.setEffectiveMembershipExpiresAt(effectiveMember.getMembershipExpiresAt());
                member.setEffectiveGroupId(member.getId().equals(effectiveMember.getId()) ? null : effectiveMember.getGroup().getId());
                userGroupMembershipExtensionRepository.update(member);
            } else if (effectiveMember == null && ((member.getMembershipExpiresAt() == null && expirationDate != null) || (member.getMembershipExpiresAt() != null && ! member.getMembershipExpiresAt().equals(expirationDate)) )) {
                member.setEffectiveMembershipExpiresAt(expirationDate);
                member.setEffectiveGroupId(groupId);
                userGroupMembershipExtensionRepository.update(member);
            }
        });

    }
}
