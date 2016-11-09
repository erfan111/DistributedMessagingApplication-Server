package dev2dev.textclient;

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

class SipLayer implements SipListener {

    // *********************************************** Private variable ************************************************

    private Hashtable<String, String> serverRegistery = new Hashtable<>();
    private Hashtable<String, String> clientRegistery = new Hashtable<>();
    private MessageProcessor messageProcessor;
    private String username;
    private SipFactory sipFactory;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;
    private SipStack sipStack;
    private serverManagement srvm;

    // ************************************************* Constructors **************************************************

    SipLayer(String username, String ip, int port) throws Exception {
        setUsername(username);
        initSip(ip, port);
        srvm = new serverManagement(ip, port);
        srvm.addServer(InetAddress.getLocalHost().getHostAddress(), 5063);
        srvm.addServer(InetAddress.getLocalHost().getHostAddress(), 5064);
    }

    // ************************************************ HeaderHelper methods *************************************************

    private SipStack createSipStack(String ip) throws PeerUnavailableException {

        Properties properties = new Properties();

        properties.setProperty("javax.sip.STACK_NAME", "TextClient");
        properties.setProperty("javax.sip.IP_ADDRESS", ip);
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "textclient.txt");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "textclientdebug.log");

        return sipFactory.createSipStack(properties);
    }

    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    void sendMessage(Request req, FromHeader Sender, String to, String message) throws ParseException,
            InvalidArgumentException, SipException {

        System.out.println("SendMessage called with: " + to + message);

        //prepare data and header that needs for sending Message/Request

        ToHeader toHeader = Helper.createToHeader(addressFactory, headerFactory, to);

        SipURI requestURI = addressFactory.createSipURI(getUsername(), Helper.getAddressFromSipUri(to));
        requestURI.setTransportParam("udp");

        ListIterator viaListIter = req.getHeaders(ViaHeader.NAME);
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();

        viaHeaders.add(getSelfViaHeader());

        while (viaListIter.hasNext()) {
            ViaHeader Via = (ViaHeader) viaListIter.next();
            System.out.println(Via.getHost() + " " + Via.getMAddr() + " " + Via.getPort());
            ViaHeader viaHeader = headerFactory.createViaHeader(Via.getHost(),
                    Via.getPort(), "udp", "branch1");
            viaHeaders.add(viaHeader);
        }

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


    private void createResponse(RequestEvent evt, int response_status_code) {
        Response response;

        try {
            response = messageFactory.createResponse(response_status_code, evt.getRequest());
            if(evt.getRequest().getHeader(SipRegister.RegisterHeader) != null)
                response.addHeader(headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ServerRegister));

            SipProvider p = (SipProvider) evt.getSource();
            p.sendResponse(response);
        } catch (Throwable e) {
            e.printStackTrace();
            messageProcessor.processError("Can't send OK reply.");
        }
    }

    private void createForwardResponse(ResponseEvent evt) {
        Response response;

        try {
            response = evt.getResponse();
            SipProvider p = (SipProvider) evt.getSource();
            response.removeFirst(ViaHeader.NAME);
            p.sendResponse(response);
        } catch (Throwable e) {
            e.printStackTrace();
            messageProcessor.processError("Can't send OK reply.");
        }
    }

    void CallregisterRequest(String serverAddress) throws ParseException,
            InvalidArgumentException, SipException {

        SipURI requestURI = addressFactory.createSipURI(getUsername(), serverAddress);
        requestURI.setTransportParam("udp");

        FromHeader fromHeader = Helper.createFromHeader(addressFactory, headerFactory, getUsername(), getAddress());
        ToHeader toHeader = Helper.createToHeader(addressFactory, headerFactory, getUsername(), getAddress());

        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(getSelfViaHeader());

        CallIdHeader callIdHeader = sipProvider.getNewCallId();

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
                Request.REGISTER);

        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);


        Request request = messageFactory.createRequest(requestURI,
                Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        request.addHeader(getSelfContactHeader());

        request.addHeader(headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ServerRegister));

        sipProvider.sendRequest(request);

    }

    // ********************************************* SipListener Interface *********************************************

    /**
     * This method is called by the SIP stack when a response arrives.
     */
    public void processResponse(ResponseEvent evt) {
        //when send message we get response of send Request
        Response response = evt.getResponse();
        int status = response.getStatusCode();

        if ((status >= 200) && (status < 300)) {
            messageProcessor.processInfo("--Sent");
            if (response.getHeader(SipRegister.RegisterHeader) == null){
                //response doesn't contain a header register
                createForwardResponse(evt);
            }else{
                messageProcessor.processInfo("You are registered in destination server" + status);
            }
        } else {
            messageProcessor.processError("Previous message not sent: " + status);
        }
    }

    /**
     * This method is called by the SIP stack when a new request arrives.
     */
    public void processRequest(RequestEvent evt) {
        //when anyone sends message to me
        Request req = evt.getRequest();

        // Getting the Sender and Receiver name
        String method = req.getMethod();
        String Receiver = ((ToHeader) req.getHeader(ToHeader.NAME)).getAddress().getDisplayName();
        FromHeader Sender = (FromHeader) req.getHeader(FromHeader.NAME);

        try {


            if (method.equals(Request.REGISTER)) {
                Header sh = req.getHeader(SipRegister.RegisterHeader);
                System.out.println(sh.toString().split(" ")[1]);
                if (sh.toString().split(" ")[1].startsWith(SipRegister.ServerRegister)) {
                    System.out.println("fucking server");
                    if (SipRegister.processRequestServer(req, serverRegistery)) {
                        System.out.println("server : registered the server");
                    } else {
                        System.out.println("server : already registered");
                    }

                    messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                    createResponse(evt, 200);

                } else {
                    System.out.println("fucking client");
                    if (SipRegister.processRequestClient(req, clientRegistery)) {
                        System.out.println("client : registered the sender");
                    } else {
                        System.out.println("client : already registered");
                    }

                    messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                    createResponse(evt, 200);

                }


            } else if (method.equals(Request.MESSAGE)) {
                try {
                    ListIterator viaListIter = req.getHeaders(ViaHeader.NAME);
                    ViaHeader firstVia = (ViaHeader) viaListIter.next();
                    Boolean from_me = false;
                    Boolean from_other_server = false;

                    if (srvm.hasItem(firstVia.getHost(), firstVia.getPort()))
                        from_other_server = true;
                    if (srvm.eqaulMyAddress(firstVia.getHost(), firstVia.getPort()))
                        from_me = true;

                    if (from_other_server) {
                        System.out.println("from other server" + (Receiver) + " " + from_me);
                        messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                        if (!from_me && clientRegistery.containsKey(Receiver)) {
                            System.out.println("I have the receiver " + clientRegistery.get(Receiver));
                            sendMessage(req, Sender, "sip:" + Receiver + '@' + clientRegistery.get(Receiver), new String(evt.getRequest().getRawContent()));
                        } else {
                            //TODO: Save the message and send it if the client registers later
                            System.out.println("no such client, dropping.... may need to waite for registering");
                        }
                    } else {
                        // from client
                        messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                        System.out.println("Message is received from my client");
                        // if the receiver is also in my register
                        if (clientRegistery.containsKey(Receiver)) {
                            sendMessage(req, Sender, "sip:" + Receiver + '@' + clientRegistery.get(Receiver), new String(evt.getRequest().getRawContent()));
                            System.out.println("Sent directly to receiver");
                        } else {
                            // I don't know the receiver, I will send it to other servers
                            srvm.sendToAll(req, Sender, Receiver, new String(evt.getRequest().getRawContent()), this);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            } else {//bad request type.
                createResponse(evt, 400);

            }
        }catch (Exception e){
            e.printStackTrace();

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

    private String getAddress() {
        return getHost() + ":" + getPort();
    }

    private ContactHeader getSelfContactHeader() throws ParseException {
        Address contactAddress = Helper.createSipAddress(addressFactory, getUsername(), getAddress());
        return headerFactory.createContactHeader(contactAddress);
    }

    private ViaHeader getSelfViaHeader() throws ParseException, InvalidArgumentException {
        return headerFactory.createViaHeader(getHost(), getPort(), "udp", "branch1");
    }

    String getHost() {
        return sipStack.getIPAddress();
    }

    @SuppressWarnings("deprecation")
    int getPort() {
        return sipProvider.getListeningPoint().getPort();
    }

    String getUsername() {
        return username;
    }

    private void setUsername(String newUsername) {
        username = newUsername;
    }

    void setMessageProcessor(MessageProcessor newMessageProcessor) {
        messageProcessor = newMessageProcessor;
    }

    // ***************************************************** End *******************************************************
}
