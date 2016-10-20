package dev2dev.textclient;

/**
 * Created by hooman on 10/19/16.
 */
class serverAddressItem {
    private String ip;
    private int port;

    serverAddressItem(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String toString() {
        return ip + ":" + port;
    }

    boolean equals(String ip, int port) {
        return this.ip.equals(ip) && this.port == port;
    }
}
