package dev2dev.textclient;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;


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

    private boolean addServer(String ip, int port, MessageProcessor messageProcessor) {
        if (hasItem(ip, port) || (ip.equals(myip) && port == myport))
            return false;
        MyAddress temp = new MyAddress(ip, port);
        servers.add(temp);
        messageProcessor.processServerReg(temp.toString());
        return true;
    }

    boolean addServer(String ip, String port, MessageProcessor messageProcessor) {
        return addServer(ip , Integer.parseInt(port), messageProcessor);
    }

    private boolean removeServer(String ip, int port, MessageProcessor messageProcessor) {
        MyAddress temp = getItem(ip, port);
        if (temp != null){
            servers.remove(temp);
            messageProcessor.processServerDeReg(getServersArray());
            return true;
        }
        return false;
    }

    boolean removeServer(String ip, String port, MessageProcessor messageProcessor) {
        return removeServer(ip , Integer.parseInt(port), messageProcessor);
    }


    boolean hasItem(String ip, int port) {
        for (MyAddress server : servers)
            if (server.equals(ip, port))
                return true;
        return false;
    }

    private MyAddress getItem(String ip, int port) {
        for (MyAddress server : servers)
            if (server.equals(ip, port))
                return server;
        return null;
    }


    void sendToAll(Request req, FromHeader sender, String receiver, String content, SipLayer sip) throws ParseException, SipException, InvalidArgumentException {
        for (MyAddress server : servers)
            sip.sendMessage(req, sender, "sip:" + receiver + '@' + server.toString(), content);
    }

    private HashSet<String> getServersArray(){
        return servers.stream().map(MyAddress::toString).collect(Collectors.toCollection(HashSet::new));
    }

}
