package dev2dev.textclient;

import gov.nist.javax.sip.header.To;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class SipLayer implements SipListener {

    // *********************************************** Private variable ************************************************

    private Hashtable<String, String> register = new Hashtable<>();
    private MessageProcessor messageProcessor;
    private String username;
    private SipFactory sipFactory;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;
    private SipStack sipStack;
    private serverManagement srvm;

    // *********************************************** Public variable *************************************************

    public RequestEvent storedEvent;

    // ************************************************* Constructors **************************************************

    public SipLayer(String username, String ip, int port) throws Exception {
        setUsername(username);
        initSip(ip, port);
        srvm = new serverManagement(ip, port);
        srvm.addServer(InetAddress.getLocalHost().getHostAddress(), 5063);
        srvm.addServer(InetAddress.getLocalHost().getHostAddress(), 5064);
    }

    // ************************************************ Helper methods *************************************************

    private SipStack createSipStack(String ip) throws PeerUnavailableException {

        Properties properties = new Properties();

        properties.setProperty("javax.sip.STACK_NAME", "TextClient");
        properties.setProperty("javax.sip.IP_ADDRESS", ip);
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "textclient.txt");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "textclientdebug.log");

        return sipFactory.createSipStack(properties);
    }

    private void initSip(String ip, int port) throws Exception {

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        sipStack = createSipStack(ip);
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();

        ListeningPoint tcp = sipStack.createListeningPoint(port, "tcp");
        ListeningPoint udp = sipStack.createListeningPoint(port, "udp");

        sipProvider = sipStack.createSipProvider(tcp);
        sipProvider.addSipListener(this);
        sipProvider = sipStack.createSipProvider(udp);
        sipProvider.addSipListener(this);
    }

    // ************************************************ Message methods ************************************************

    /**
     * This method uses the SIP stack to send a message.
     */
    public void sendMessage(Request req, FromHeader Sender, String to, String message) throws ParseException,
            InvalidArgumentException, SipException {

        System.out.println("SendMessage called with: " + to + message);

        //prepare data and header that needs for sending Message/Request

        System.out.println(to);
        ToHeader toHeader = Helper.createToHeader(addressFactory, headerFactory, to);

        SipURI requestURI = addressFactory.createSipURI(getUsername(), Helper.getAddressFromSipUri(to));
        requestURI.setTransportParam("udp");

        ListIterator viaListIter = req.getHeaders("Via");
        ArrayList viaHeaders = new ArrayList();

        while (viaListIter.hasNext()) {
            ViaHeader Via = (ViaHeader) viaListIter.next();
            System.out.println(Via.getHost() + " " + Via.getMAddr() + " " + Via.getPort());
            ViaHeader viaHeader = headerFactory.createViaHeader(Via.getHost(),
                    Via.getPort(), "udp", "branch1");
            viaHeaders.add(viaHeader);
        }

        viaHeaders.add(getSelfViaHeader());

        CallIdHeader callIdHeader = sipProvider.getNewCallId();

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
                Request.MESSAGE);

        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);

        ContentTypeHeader contentTypeHeader = headerFactory
                .createContentTypeHeader("text", "plain");

        Request request = messageFactory.createRequest(requestURI,
                Request.MESSAGE, callIdHeader, cSeqHeader, Sender,
                toHeader, viaHeaders, maxForwards, contentTypeHeader,
                message);

        request.addHeader(getSelfContactHeader());

        System.out.println("sent");
        sipProvider.sendRequest(request);
    }


    public void createResponse(Request req, int response_status_code) {
        Response response ;

        try {
            response = messageFactory.createResponse(response_status_code, req);
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("888"); //This is mandatory as per the spec.
            ServerTransaction st = sipProvider.getNewServerTransaction(req);
            st.sendResponse(response);
        } catch (Throwable e) {
            e.printStackTrace();
            messageProcessor.processError("Can't send OK reply.");
        }
    }

    // ********************************************* SipListener Interface *********************************************

    /**
     * This method is called by the SIP stack when a response arrives.
     */
    public void processResponse(ResponseEvent evt) {
        //when send message we get response of send Request
        Response response = evt.getResponse();
        int status = response.getStatusCode();

        if ((status >= 200) && (status < 300)) { //Success!
            messageProcessor.processInfo("--Sent");
            /////////////////
            Request req = storedEvent.getRequest();
//            if (req.equals(evt.getClientTransaction().getRequest()))
//            {
//            }

            String method = req.getMethod();
            if (!method.equals("MESSAGE")) { //bad request type.
                messageProcessor.processError("Bad request type: " + method);
                return;
            }

            FromHeader from = (FromHeader) req.getHeader(FromHeader.NAME);
            messageProcessor.processMessage(from.getAddress().toString(),
                    new String(req.getRawContent()));
            Response response2;
            System.out.println("before try");
            try { //Reply with OK
                System.out.println("generating response");
                response2 = messageFactory.createResponse(200, req);
                ToHeader toHeader = (ToHeader) response2.getHeader(ToHeader.NAME);
                toHeader.setTag("888"); //This is mandatory as per the spec.
                ServerTransaction st = sipProvider.getNewServerTransaction(req);
                st.sendResponse(response2);
                System.out.println("response sent 200");
            } catch (Throwable e) {
                e.printStackTrace();
                messageProcessor.processError("Can't send OK reply.");
            }


            ////////
            return;
        }

        messageProcessor.processError("Previous message not sent: " + status);
    }

    /**
     * This method is called by the SIP stack when a new request arrives.
     */
    public void processRequest(RequestEvent evt) {
        //when anyone sends message to me
        storedEvent = evt;
        Request req = evt.getRequest();

        // Getting the Sender and Receiver name
        String method = req.getMethod();
        String Receiver = ((To) req.getHeader(To.NAME)).getAddress().getDisplayName();
        FromHeader Sender = (FromHeader) req.getHeader(FromHeader.NAME);

        if (method.equals("MESSAGE")) {
            try {
                // CHECK IF THE MESSAGE HAS COME FROM THE SERVER
                ListIterator viaListIter = req.getHeaders(ViaHeader.NAME);
                ViaHeader Via;
//                String Host;
                Boolean from_me = false;
                Boolean from_other_server = false;
                while (viaListIter.hasNext()) {
                    Via = (ViaHeader) viaListIter.next();
                    if (srvm.hasItem(Via.getHost(), Via.getPort()))
                        from_other_server = true;
                    if (srvm.eqaulMyAddress(Via.getHost(), Via.getPort()))
                        from_me = true;
//                    if (getUsername().equals("server")) {
//                        Host = "server2";
//                    } else {
//                        Host = "server";
//                    }
//                    System.out.println("other server: " + servers.get(Host).split(":")[1] + " and Via port: " + Via.getPort());
//                    // if message is comming from other server;
//                    if (Integer.parseInt(servers.get(Host).split(":")[1]) == Via.getPort()) {
//                        System.out.println("in az server umade, baraye cliente man ");
//                        from_other_server = true;
//                        break;
//                    }
//                    if (getPort() == Via.getPort()) {
//                        from_me = true;
//                    }
                }
                if (from_other_server) {
                    System.out.println("from other server" + (Receiver) + " " + from_me);
                    // If receiver is in my registered clients list
                    //register.get(Receiver)
                    if (!from_me && register.containsKey(Receiver)) {
//						String fucker = register.get(Receiver);
                        System.out.println("I have the receiver " + register.get(Receiver));
                        sendMessage(req, Sender, "sip:" + Receiver + '@' + register.get(Receiver), new String(evt.getRequest().getRawContent()));
                    }
                    // Drop the message TODO: Save the message and send it if the client registers later
                    else {
                        System.out.println("no such client, dropping....");
                    }
                } else // from client
                {
                    // check the register for the sender
                    if (SipRegister.processRequest(req, register)) {
                        System.out.println("registered the sender");
                    } else {
                        System.out.println("already registered");
                    }
                    messageProcessor.processMessage("Event", method);
                    System.out.println("Message is received from my client");
                    // if the receiver is also in my register
                    if (register.containsKey(Receiver)) {
                        sendMessage(req, Sender, "sip:" + Receiver + '@' + register.get(Receiver), new String (evt.getRequest().getRawContent()));
                        System.out.println("Sent directly to receiver");
                    }
                    // I don't know the receiver, I will send it to other servers
                    else {
//                        ViaHeader vh = (ViaHeader) req.getHeader(ViaHeader.NAME);
//                        for (int i = 0; i < srvm.servers.size(); ++i)
//                        {
//                            System.out.println("KKK: " + new String (evt.getRequest().getRawContent()));
//                            sendMessage(req, Sender, "sip:" + Receiver + '@' + srvm.servers.get(i).toString(),
//                                    new String (evt.getRequest().getRawContent()));
//
//                        }
                        srvm.sendToAll(req, Sender, Receiver, new String (evt.getRequest().getRawContent()), this);
//                        if (getUsername().equals("server")) {
//                            System.out.println("fuck " + servers.get("server2"));
//                            sendMessage(req, Sender, "sip:" + Receiver + '@' + servers.get("server2"), new String (evt.getRequest().getRawContent())); //TODO:FIX SERVER ADDRESS
//                        } else {
//                            sendMessage(req, Sender, "sip:" + Receiver + '@' + servers.get("server"), new String (evt.getRequest().getRawContent())); //TODO:FIX SERVER ADDRESS
//                        }
                    }
                }

//				createResponse(req, 100);

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else { //bad request type.
            createResponse(req, 400);
        }

    }

    /**
     * This method is called by the SIP stack when there's no answer
     * to a message. Note that this is treated differently from an error
     * message.
     */
    public void processTimeout(TimeoutEvent evt) {
        messageProcessor
                .processError("Previous message not sent: " + "timeout");
    }

    /**
     * This method is called by the SIP stack when there's an asynchronous
     * message transmission error.
     */
    public void processIOException(IOExceptionEvent evt) {
        messageProcessor.processError("Previous message not sent: "
                + "I/O Exception");
    }

    /**
     * This method is called by the SIP stack when a dialog (session) ends.
     */
    public void processDialogTerminated(DialogTerminatedEvent evt) {
    }

    /**
     * This method is called by the SIP stack when a transaction ends.
     */
    public void processTransactionTerminated(TransactionTerminatedEvent evt) {
    }

    // ************************************************ Get/Set methods ************************************************

    public String getAddress() {
        return getHost() + ":" + getPort();
    }

    private ContactHeader getSelfContactHeader() throws ParseException {
        Address contactAddress = Helper.createSipAddress(addressFactory, getUsername(), getAddress());
        return headerFactory.createContactHeader(contactAddress);
    }

    private ViaHeader getSelfViaHeader() throws ParseException, InvalidArgumentException {
        ViaHeader viaHeader = headerFactory.createViaHeader(getHost(), getPort(), "udp", "branch1");
        return viaHeader;
    }

    public String getHost() {
        String host = sipStack.getIPAddress();
        return host;
    }

    public int getPort() {
        int port = sipProvider.getListeningPoint().getPort();
        return port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String newUsername) {
        username = newUsername;
    }

    public void setMessageProcessor(MessageProcessor newMessageProcessor) {
        messageProcessor = newMessageProcessor;
    }

    // ***************************************************** End *******************************************************
}
