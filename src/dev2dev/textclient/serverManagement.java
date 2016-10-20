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
public class serverManagement {
    public ArrayList<serverAddressItem> servers;
    private String myip;
    private int myport;

    public serverManagement(String ip, int port) {
        myip = ip;
        myport = port;
        servers = new ArrayList<>();
    }

    public boolean eqaulMyAddress(String ip, int port) {
        return ip.equals(myip) && port == myport;
    }

    public boolean addServer(String ip, int port) {
        if (hasItem(ip, port) || (ip.equals(myip) && port == myport))
            return false;
        servers.add(new serverAddressItem(ip, port));
        return true;
    }

    public boolean hasItem(String ip, int port) {
        for (int i = 0; i < servers.size(); ++i)
            if (servers.get(i).equals(ip, port))
                return true;
        return false;
    }

    public void sendToAll (Request req, FromHeader sender, String receiver, String content, SipLayer sip) throws ParseException, SipException, InvalidArgumentException {
        for (int i = 0; i < servers.size(); ++i)
            sip.sendMessage(req, sender, "sip:" + receiver + '@' + servers.get(i).toString(), content);
    }

}
