package dev2dev.textclient;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Created by hooman on 10/19/16.
 */

class serverManagement {
    private ArrayList<serverAddressItem> servers;
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
        servers.add(new serverAddressItem(ip, port));
        return true;
    }

    boolean hasItem(String ip, int port) {
        for (serverAddressItem server : servers)
            if (server.equals(ip, port))
                return true;
        return false;
    }

    void sendToAll(Request req, FromHeader sender, String receiver, String content, SipLayer sip) throws ParseException, SipException, InvalidArgumentException {
        for (serverAddressItem server : servers)
            sip.sendMessage(req, sender, "sip:" + receiver + '@' + server.toString(), content);
    }

}
