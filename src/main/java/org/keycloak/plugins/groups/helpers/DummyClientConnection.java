package org.keycloak.plugins.groups.helpers;

import org.keycloak.common.ClientConnection;

public class DummyClientConnection  implements ClientConnection {

    private String remoteAddr;

    public DummyClientConnection(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }


    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return "remoteHost";
    }

    @Override
    public int getRemotePort() {
        return -1;
    }

    @Override
    public String getLocalAddr() {
        return "localAddr";
    }

    @Override
    public int getLocalPort() {
        return -2;
    }
}
