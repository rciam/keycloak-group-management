package org.keycloak.plugins.groups.scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.helpers.Utils;
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
        repository.dailyExecutedActions(session);

        session.realms().getRealmsStream().forEach(realm -> {
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