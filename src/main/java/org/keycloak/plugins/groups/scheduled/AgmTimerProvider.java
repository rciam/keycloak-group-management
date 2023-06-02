package org.keycloak.plugins.groups.scheduled;

import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class AgmTimerProvider implements TimerProvider {

    private static final Logger logger = Logger.getLogger(AgmTimerProvider.class);

    private final KeycloakSession session;
    private final Timer timer;
    private final int transactionTimeout;
    private final AgmTimerProviderFactory factory;

    public AgmTimerProvider(KeycloakSession session, Timer timer, int transactionTimeout, AgmTimerProviderFactory factory) {
        this.session = session;
        this.timer = timer;
        this.transactionTimeout = transactionTimeout;
        this.factory = factory;
    }

    public void schedule(final Runnable runnable,final long delay,final long intervalMillis, String taskName) {

        logger.debugf("Starting task '%s' with dalay '%d' and interval '%d'", taskName, delay, intervalMillis);
        timer.schedule(createTimerTask (runnable, intervalMillis, taskName),delay, intervalMillis);
    }


    @Override
    public void schedule(final Runnable runnable, final long intervalMillis, String taskName) {

        logger.debugf("Starting task '%s' with interval '%d'", taskName, intervalMillis);
        timer.schedule(createTimerTask (runnable, intervalMillis, taskName), intervalMillis, intervalMillis);
    }

    public void scheduleOnce(final Runnable runnable, final long delay, String taskName) {

        logger.debugf("Task '%s' will be executed with delay '%d'", taskName, delay);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
    }

    private TimerTask createTimerTask (final Runnable runnable, final long intervalMillis, String taskName) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };

        AgmTimerTaskContextImpl taskContext = new AgmTimerTaskContextImpl(runnable, task, intervalMillis);
        AgmTimerTaskContextImpl existingTask = factory.putTask(taskName, taskContext);
        if (existingTask != null) {
            logger.debugf("Existing timer task '%s' found. Cancelling it", taskName);
            existingTask.timerTask.cancel();
        }
        return task;
    }

    @Override
    public void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName) {
        ScheduledTaskRunner scheduledTaskRunner = new ScheduledTaskRunner(session.getKeycloakSessionFactory(), scheduledTask, transactionTimeout);
        this.schedule(scheduledTaskRunner, intervalMillis, taskName);
    }

    @Override
    public TimerTaskContext cancelTask(String taskName) {
        AgmTimerTaskContextImpl existingTask = factory.removeTask(taskName);
        if (existingTask != null) {
            logger.debugf("Cancelling task '%s'", taskName);
            existingTask.timerTask.cancel();
        }

        return existingTask;
    }

    @Override
    public void close() {
        // do nothing
    }

}
