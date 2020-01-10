package com.tsc.statgui;

public class TCPConnectionData {
    private String host;
    private int port;

    public TCPConnectionData(String host, String port) {
        this.host = host;
        this.port = Integer.parseInt(port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
