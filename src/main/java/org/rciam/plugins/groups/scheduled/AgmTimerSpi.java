package org.rciam.plugins.groups.scheduled;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AgmTimerSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "agm-timer";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AgmTimerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AgmTimerProviderFactory.class;
    }
}
