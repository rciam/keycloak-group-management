package org.keycloak.plugins.groups;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.scheduled.GroupManagementTasks;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

@ApplicationScoped
public class GroupManagementEvent {

    private static final Logger logger = Logger.getLogger(GroupManagementEvent.class);

    void onStart(@Observes StartupEvent ev) {
        logger.info("GroupManagement extension is starting...");
        //work only for quarkus
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        instance.init();
        KeycloakSession session= instance.create();
        TimerProvider timer = session.getProvider(TimerProvider.class);
        //schedule task once a day at 02.00
        long interval = 24 * 3600 * 1000;
        long delay = (LocalDate.now().plusDays(1).atTime(2, 0).atZone(ZoneId.systemDefault()).toEpochSecond() - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()) * 1000;
        timer.schedule(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new GroupManagementTasks(), interval),delay, interval, "GroupManagementActions");
        //execute also task once now if not executed???
    }
}
