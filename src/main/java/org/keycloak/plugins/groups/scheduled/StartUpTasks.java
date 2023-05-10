package org.keycloak.plugins.groups.scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.EduPersonEntitlementConfigurationEntity;
import org.keycloak.plugins.groups.jpa.repositories.EduPersonEntitlementConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class StartUpTasks implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        //same as daily task (only if not executed before this day)
        UserGroupMembershipExtensionRepository repository = new UserGroupMembershipExtensionRepository(session, null);
        EduPersonEntitlementConfigurationRepository eduPersonEntitlementConfigurationRepository = new EduPersonEntitlementConfigurationRepository(session);
        repository.dailyExecutedActions(session);

        session.realms().getRealmsStream().forEach(realm -> {
            //create default eduPersonEntitlement configuration entity if not exist
            if (eduPersonEntitlementConfigurationRepository.getByRealm(realm.getId()) == null) {
                EduPersonEntitlementConfigurationEntity configurationEntity = new EduPersonEntitlementConfigurationEntity();
                configurationEntity.setId(KeycloakModelUtils.generateId());
                configurationEntity.setUserAttribute("eduPersonEntitlement");
                configurationEntity.setUrnNamespace("urn%3Ageant%3Aeosc-portal.eu");
                RealmEntity realmEntity = new RealmEntity();
                realmEntity.setId(realm.getId());
                configurationEntity.setRealmEntity(realmEntity);
                eduPersonEntitlementConfigurationRepository.create(configurationEntity);
            }

            GroupInvitationRepository groupInvitationRepository = new GroupInvitationRepository(session, realm);
            groupInvitationRepository.getAllByRealm().forEach(entity -> {
                TimerProvider timer = session.getProvider(TimerProvider.class);
                long invitationExpirationHour = realm.getAttribute(Utils.invitationExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.invitationExpirationPeriod)) : 72;
                long interval = entity.getCreationDate().atZone(ZoneId.systemDefault()).toEpochSecond() + (invitationExpirationHour * 3600 * 1000) - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
                if (interval <=  60 * 1000)
                    interval = 60 * 1000;
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(entity.getId(), realm.getId()), interval), interval, "DeleteExpiredInvitation_"+entity.getId());
            });
        });
    }
}