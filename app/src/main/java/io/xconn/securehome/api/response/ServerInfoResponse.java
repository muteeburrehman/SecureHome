package io.xconn.securehome.api.response;

public class ServerInfoResponse {
    private String server_hostname;
    private String server_ip_address;
    private int server_port;

    public ServerInfoResponse() {
    }

    public String getServer_hostname() {
        return server_hostname;
    }

    public void setServer_hostname(String server_hostname) {
        this.server_hostname = server_hostname;
    }

    public String getServer_ip_address() {
        return server_ip_address;
    }

    public void setServer_ip_address(String server_ip_address) {
        this.server_ip_address = server_ip_address;
    }

    public int getServer_port() {
        return server_port;
    }

    public void setServer_port(int server_port) {
        this.server_port = server_port;
    }
}