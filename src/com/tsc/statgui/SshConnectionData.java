package com.tsc.statgui;

public class SshConnectionData {
    private String ip;
    private String port;
    private String userName;
    private String password;

    public SshConnectionData(String ip, String port, String userName, String password) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}

