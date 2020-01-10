package com.tsc.statgui;

public class SiebelConnectionData {
    private String ip;
    private String port;
    private String enterprise;
    private String objmgr;
    private String userName;
    private String server;
    private String password;
    private String locale;

    public SiebelConnectionData(String ip, String port, String enterprise, String objmgr, String userName, String server, String password, String locale) {
        this.ip = ip;
        this.port = port;
        this.enterprise = enterprise;
        this.objmgr = objmgr;
        this.userName = userName;
        this.server = server;
        this.password = password;
        this.locale = locale;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(String enterprise) {
        this.enterprise = enterprise;
    }

    public String getObjmgr() {
        return objmgr;
    }

    public void setObjmgr(String objmgr) {
        this.objmgr = objmgr;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
