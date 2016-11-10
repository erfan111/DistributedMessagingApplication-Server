package dev2dev.textclient;

import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.parser.StringMsgParser;

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

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
class SipRegister {

    private HashMap<String, String> childs = new HashMap<>();

    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;

    private String username;
    private String server;
    private String protocol;
    private String ip;
    private int port;
    private long cseq = 0;
    public static final String RegisterHeader = "regidterHeader";

    public static final String ClientRegister = "Reg_Client";
    public static final String ClientDeRegister = "DeReg_Client";

    public static final String ServerRegister = "Reg_Server";
    public static final String ServerDeRegister = "DeReg_Server";

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
        CallIdHeader callIdHeader = sipProvider.getNewCallId();
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cseq, "REGISTER");
        Address fromAddress = addressFactory.createAddress("sip:" + username + '@' + server);
        String tag = "Tag";
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, String.valueOf(tag));
        ToHeader toHeader = headerFactory.createToHeader(fromAddress, null);
        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        ViaHeader viaHeader = headerFactory.createViaHeader(ip, port, protocol, null);
        viaHeaders.add(viaHeader);
        Address contactAddress = addressFactory.createAddress("sip:" + username + '@' + ip + "transport=" + protocol);
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        Request request = messageFactory.createRequest("REGISTER sip:" + server + "SIP/2.0\r\n\r\n");
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

    static boolean processRequestServer(Request request, Hashtable<String, String> hm) {
        From sender = (From) request.getHeader(From.NAME);
        String client = sender.getAddress().getDisplayName();
        if (hm.containsKey(client)) {
            return false;
        } else {
            hm.put(client, sender.getHostPort().toString());
            return true;
        }
    }

    static boolean registerClient(Request request, Hashtable<String, String> hm, MessageProcessor messageProcessor) {
        From sender = (From) request.getHeader(From.NAME);
        String client = sender.getAddress().getDisplayName();
        if (hm.containsKey(client)) {
            return false;
        } else {
            hm.put(client, sender.getHostPort().toString());
            messageProcessor.processClientReg(client);
            return true;
        }
    }

    static boolean deRegisterClient(Request request, Hashtable<String, String> hm) {
        From sender = (From) request.getHeader(From.NAME);
        String client = sender.getAddress().getDisplayName();
        if (hm.containsKey(client)) {
            hm.remove(client);
            return true;
        } else {
            return false;
        }
    }


    public boolean isRegister(String client) {
        return childs.containsKey(client);
    }

    public String removeChild(String client) {
        return childs.remove(client);
    }

}
