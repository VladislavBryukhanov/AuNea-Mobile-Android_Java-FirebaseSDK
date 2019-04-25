package com.example.nameless.autoupdating.models;

public class AppData {
    private Double currentVersion;
    private Double lastSupportedVersion;
    private String server;
    private boolean serverAvailable;

    AppData() {}

    public Double getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Double currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Double getLastSupportedVersion() {
        return lastSupportedVersion;
    }

    public void setLastSupportedVersion(Double lastSupportedVersion) {
        this.lastSupportedVersion = lastSupportedVersion;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public boolean isServerAvailable() {
        return serverAvailable;
    }

    public void setServerAvailable(boolean serverAvailable) {
        this.serverAvailable = serverAvailable;
    }
}
