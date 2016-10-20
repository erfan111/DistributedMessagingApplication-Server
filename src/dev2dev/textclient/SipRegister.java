package dev2dev.textclient;

import gov.nist.javax.sip.header.From;

import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class SipRegister {

    private HashMap<String, String> childs = new HashMap<>();
    private ArrayList viaHeaders;
    private ViaHeader viaHeader;

    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private CallIdHeader callIdHeader;
    private CSeqHeader cSeqHeader;
    private FromHeader fromHeader;
    private Address fromAddress, contactAddress;
    private ToHeader toHeader;
    private MaxForwardsHeader maxForwardsHeader;
    private ContactHeader contactHeader;
    private Request request;

    private String username, server, tag = "Tag", protocol, ip;
    private int port;
    private long cseq = 0;

    public SipRegister(SipProvider sp, SipFactory sf, String username, String server,
                       String ip, int port, String protocol) throws Exception {
        sipProvider = sp;
        addressFactory = sf.createAddressFactory();
        headerFactory = sf.createHeaderFactory();
        messageFactory = sf.createMessageFactory();
        this.username = username;
        this.server = server;
        this.protocol = protocol;
        this.ip = ip;
        this.port = port;
    }

    public void sendRequest() throws Exception {
        callIdHeader = sipProvider.getNewCallId();
        cSeqHeader = headerFactory.createCSeqHeader(cseq, "REGISTER");
        fromAddress = addressFactory.createAddress("sip:" + username + '@' + server);
        fromHeader = headerFactory.createFromHeader(fromAddress, String.valueOf(tag));
        toHeader = headerFactory.createToHeader(fromAddress, null);
        maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
        viaHeaders = new ArrayList();
        viaHeader = headerFactory.createViaHeader(ip, port, protocol, null);
        viaHeaders.add(viaHeader);
        contactAddress = addressFactory.createAddress("sip:" + username + '@' + ip + "transport=" + protocol);
        contactHeader = headerFactory.createContactHeader(contactAddress);
        request = messageFactory.createRequest("REGISTER sip:" + server + "SIP/2.0\r\n\r\n");
        request.addHeader(callIdHeader);
        request.addHeader(cSeqHeader);
        request.addHeader(fromHeader);
        request.addHeader(toHeader);
        request.addHeader(maxForwardsHeader);
        request.addHeader(viaHeader);
        request.addHeader(contactHeader);
        cseq++;
        sipProvider.sendRequest(request);
    }

    public boolean processResponse(Response response) {
        switch (response.getStatusCode()) {
            case Response.ACCEPTED:
            case Response.OK:
                return true;
            case Response.BAD_REQUEST:
            case Response.FORBIDDEN:
            case Response.UNAUTHORIZED:
                return false;
            default:
                return false;
        }
    }

    public static boolean processRequest(Request request, Hashtable hm) {
        From sender = (From) request.getHeader(From.NAME);
        String client = sender.getAddress().getDisplayName();
        if (hm.containsKey(client)) {
            return false;
        } else {
            hm.put(client, sender.getHostPort().toString());
            return true;
        }
    }

    public boolean isRegister(String client) {
        return childs.containsKey(client);
    }

    public String removeChild(String client) {
        return childs.remove(client);
    }

}
