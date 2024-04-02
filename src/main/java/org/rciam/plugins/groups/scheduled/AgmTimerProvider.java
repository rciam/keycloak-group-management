package org.rciam.plugins.groups.scheduled;

import org.keycloak.provider.Provider;
import org.keycloak.timer.ScheduledTask;

public interface AgmTimerProvider extends Provider {

    public void schedule(Runnable runnable,long delay, long intervalMillis, String taskName);

    public void schedule(Runnable runnable, long intervalMillis, String taskName);

    public void scheduleOnce(final Runnable runnable, final long delay, String taskName);

    public void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName);


    /**
     * Cancel task and return the details about it, so it can be eventually restored later
     *
     * @param taskName
     * @return existing task or null if task under this name doesn't exist
     */
    public AgmTimerProvider.AgmTimerTaskContext cancelTask(String taskName);


    interface AgmTimerTaskContext {

        Runnable getRunnable();

        long getIntervalMillis();
    }

}

