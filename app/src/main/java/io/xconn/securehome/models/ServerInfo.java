package io.xconn.securehome.models;

public class ServerInfo {
    private String hostname;
    private String ipAddress;
    private int port;

    public ServerInfo(String hostname, String ipAddress, int port) {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}