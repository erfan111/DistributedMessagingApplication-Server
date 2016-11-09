package dev2dev.textclient;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;


class serverManagement {
    private ArrayList<MyAddress> servers;
    private String myip;
    private int myport;

    serverManagement(String ip, int port) {
        myip = ip;
        myport = port;
        servers = new ArrayList<>();
    }

    boolean eqaulMyAddress(String ip, int port) {
        return ip.equals(myip) && port == myport;
    }

    boolean addServer(String ip, int port) {
        if (hasItem(ip, port) || (ip.equals(myip) && port == myport))
            return false;
        servers.add(new MyAddress(ip, port));
        return true;
    }

    boolean addServer(String ip, String port) {
        return addServer(ip , Integer.parseInt(port));
    }

    boolean removeServer(String ip, int port) {
        MyAddress temp = getItem(ip, port);
        if (temp != null){
            servers.remove(temp);

            return true;
        } else{
            return false;
        }
    }

    boolean removeServer(String ip, String port) {
        return removeServer(ip , Integer.parseInt(port));
    }


    boolean hasItem(String ip, int port) {
        for (MyAddress server : servers)
            if (server.equals(ip, port))
                return true;
        return false;
    }

    MyAddress getItem(String ip, int port) {
        for (MyAddress server : servers)
            if (server.equals(ip, port))
                return server;
        return null;
    }


    void sendToAll(Request req, FromHeader sender, String receiver, String content, SipLayer sip) throws ParseException, SipException, InvalidArgumentException {
        for (MyAddress server : servers)
            sip.sendMessage(req, sender, "sip:" + receiver + '@' + server.toString(), content);
    }

}
