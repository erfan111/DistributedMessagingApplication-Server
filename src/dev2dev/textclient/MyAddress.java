package dev2dev.textclient;

class MyAddress {
    private String ip;
    private int port;

    MyAddress(String address) {
        this(Helper.getIpFromAddress(address), Helper.getPortFromAddress(address));
    }

    MyAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private MyAddress(String ip, String port) {
        this(ip, Integer.parseInt(port));
    }

    int getPort() {
        return port;
    }

    String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return ip + ":" + String.valueOf(port);
    }

    boolean equals(String ip, int port) {
        return this.ip.equals(ip) && this.port == port;
    }

    boolean equals(MyAddress myAddress) {
        return this.ip.equals(myAddress.ip) && this.port == myAddress.port;
    }
}
