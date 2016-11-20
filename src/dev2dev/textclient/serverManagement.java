package dev2dev.textclient;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ToHeader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.stream.Collectors;


class serverManagement {
    private ArrayList<MyAddress> servers;
    private String myip;
    private int myport;
    public Hashtable<String, MyAddress> clientCached = new Hashtable<>();

    static final String MyViaHeader = "MyViaHeader";

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
        return addServer(ip, Integer.parseInt(port), messageProcessor);
    }

    private boolean removeServer(String ip, int port, MessageProcessor messageProcessor) {
        MyAddress temp = getItem(ip, port);
        if (temp != null) {
            servers.remove(temp);
            messageProcessor.processServerDeReg(getServersArray());
            return true;
        }
        return false;
    }

    boolean removeServer(String ip, String port, MessageProcessor messageProcessor) {
        return removeServer(ip, Integer.parseInt(port), messageProcessor);
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

    void sendWithRouting(RequestEvent evt, SipLayer sip) throws Exception {
        String toName = ((ToHeader) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getDisplayName();

        if (clientCached.containsKey(toName)){
            System.out.println(clientCached.get(toName).toString());
            sip.forwardMessage(evt, clientCached.get(toName), false);
            return ;
        }

        ArrayList<MyAddress> MyViaHeaders = Helper.getMyViaHeaderValue(evt);

        for (MyAddress server : servers) {
            if (!hasItem(MyViaHeaders, server)) {
                System.out.println(server.toString());
                sip.forwardMessage(evt, server, false);
                return ;
            }
        }

        sip.createRequestFailToFindClient(evt.getRequest());
    }

//    void sendWithRouting(ResponseEvent ret, SipLayer sip) throws ParseException, SipException, InvalidArgumentException {
//        ArrayList<MyAddress> MyViaHeaders = Helper.getMyViaHeaderValue(ret);
//        for (MyAddress server : servers) {
//            if (!hasItem(MyViaHeaders, server)) {
//
//                sip.forwardMessage(evt, server, false);
//                break;
//            }
//        }
//
//        sip.createResponseForFailToFindClient(evt);
//    }

    private boolean hasItem(ArrayList<MyAddress> array, MyAddress item) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).equals(item))
                return true;
        }
        return false;
    }

    private HashSet<String> getServersArray() {
        return servers.stream().map(MyAddress::toString).collect(Collectors.toCollection(HashSet::new));
    }
}
