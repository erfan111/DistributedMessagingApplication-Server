package dev2dev.textclient;

/**
 * Created by hooman on 10/19/16.
 */
public class serverAddressItem {
    public String ip;
    public int port;

    public serverAddressItem(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String toString() {
        return ip + ":" + port;
    }

    public boolean equals(String ip, int port) {
        return this.ip.equals(ip) && this.port == port;
    }
}
