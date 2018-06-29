package com.example.nameless.autoupdating.models;

import java.io.Serializable;
import java.net.InetAddress;

public class ClientToClient  implements Serializable {

    private String firstUser;
    private String secondUser;
    private InetAddress firstUserIP;
    private int firstUserPort;
    private InetAddress secondUserIP;
    private int secondUserPort;
    private boolean isConnected;

    public ClientToClient() {}

    public ClientToClient(String firstUser, String secondUser) {
        this.firstUser = firstUser;
        this.secondUser = secondUser;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public String getFirstUser() {
        return firstUser;
    }

    public void setFirstUser(String firstUser) {
        this.firstUser = firstUser;
    }

    public String getSecondUser() {
        return secondUser;
    }

    public void setSecondUser(String secondUser) {
        this.secondUser = secondUser;
    }

    public InetAddress getFirstUserIP() {
        return firstUserIP;
    }

    public void setFirstUserIP(InetAddress firstUserIP) {
        this.firstUserIP = firstUserIP;
    }

    public int getFirstUserPort() {
        return firstUserPort;
    }

    public void setFirstUserPort(int firstUserPort) {
        this.firstUserPort = firstUserPort;
    }

    public InetAddress getSecondUserIP() {
        return secondUserIP;
    }

    public void setSecondUserIP(InetAddress secondUserIP) {
        this.secondUserIP = secondUserIP;
    }

    public int getSecondUserPort() {
        return secondUserPort;
    }

    public void setSecondUserPort(int secondUserPort) {
        this.secondUserPort = secondUserPort;
    }
}
