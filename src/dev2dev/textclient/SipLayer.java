package dev2dev.textclient;

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

    public Hashtable<String, Client> clientRegistery = new Hashtable<>();
    private MessageProcessor messageProcessor;
    private String username;
    private SipFactory sipFactory;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;
    private SipStack sipStack;
    public serverManagement srvm;

    // ************************************************* Constructors **************************************************

    SipLayer(String username, String ip, int port) throws Exception {
        setUsername(username);
        initSip(ip, port);
        srvm = new serverManagement(ip, port);
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

    void forwardMessage(RequestEvent evt, MyAddress to, boolean toIsClient) throws ParseException,
            InvalidArgumentException, SipException {
        Request req = evt.getRequest();

        SipURI requestURI = addressFactory.createSipURI(getUsername(), to.toString());
        req.setRequestURI(requestURI);

        req.addFirst(getSelfViaHeader());

        if (toIsClient) {
            String clientStr = ((ToHeader) req.getHeader(ToHeader.NAME)).getAddress().getDisplayName();
            Client client = clientRegistery.get(clientStr);
            client.addRequestEvent(evt);
            if (client.getOnlineStatus()) {
                System.out.println("sent");
                sipProvider.sendRequest(req);
            }
        } else {
            ArrayList<MyAddress> myViaHeaders = Helper.getMyViaHeaderValue(req);
            myViaHeaders.add(new MyAddress(getAddress()));

            try {
                req.setHeader(Helper.createMyViaHeader(headerFactory, myViaHeaders));
            } catch (Exception e) {
                e.printStackTrace();
            }
            req.removeHeader(Helper.FailHeader);

            System.out.println("sent");
            sipProvider.sendRequest(req);
        }
    }

    public void createRequestFailToFindClient(Request req) throws Exception {
        System.out.println("createRequestFailToFindClient");
        ListIterator viaListIter = req.getHeaders(ViaHeader.NAME);
        ViaHeader via = null;
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();

        while (viaListIter.hasNext()) {
            ViaHeader viaHeader = (ViaHeader) viaListIter.next();
            if (via == null) {
                via = viaHeader;
            } else {
                viaHeaders.add(viaHeader);
            }
        }
        System.out.println("send fail to" + via.getHost() + ":" + via.getPort());
        SipURI requestURI = addressFactory.createSipURI(getUsername(), via.getHost() + ":" + via.getPort());
        requestURI.setTransportParam("udp");

        ToHeader toHeader = (ToHeader) req.getHeader(ToHeader.NAME);
        FromHeader fromHeader = (FromHeader) req.getHeader(FromHeader.NAME);

        CallIdHeader callIdHeader = (CallIdHeader) req.getHeader(CallIdHeader.NAME);

        CSeqHeader cSeqHeader = (CSeqHeader) req.getHeader(CSeqHeader.NAME);

        MaxForwardsHeader maxForwards = (MaxForwardsHeader) req.getHeader(MaxForwardsHeader.NAME);
        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) req.getHeader(ContentTypeHeader.NAME);

        Request request = messageFactory.createRequest(requestURI,
                Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards, contentTypeHeader,
                req.getContent());

        ArrayList<MyAddress> list = Helper.getMyViaHeaderValue(req);
        list.add(new MyAddress(getAddress()));

        request.setHeader(Helper.createMyViaHeader(headerFactory, list));

        request.setHeader(headerFactory.createHeader(Helper.FailHeader, "yes"));

        sipProvider.sendRequest(request);
    }

    private void createResponse(Request req, int response_status_code, Header header) {
        Response response;

        try {
            response = messageFactory.createResponse(response_status_code, req);
            if (header != null)
                response.setHeader(header);

            ToHeader toHeader = (ToHeader) req.getHeader(ToHeader.NAME);
            response.setHeader(toHeader);

            FromHeader fromHeader = (FromHeader) req.getHeader(FromHeader.NAME);
            response.setHeader(fromHeader);

            CallIdHeader callIdHeader = (CallIdHeader) req.getHeader(CallIdHeader.NAME);
            response.setHeader(callIdHeader);

            CSeqHeader cSeqHeader = (CSeqHeader) req.getHeader(CSeqHeader.NAME);
            response.setHeader(cSeqHeader);

            MaxForwardsHeader maxForwardsHeader = (MaxForwardsHeader) req.getHeader(MaxForwardsHeader.NAME);
            response.setHeader(maxForwardsHeader);

            response.setHeader(headerFactory.createHeader(Helper.ServerSource,
                    "sip:" + getUsername() + "@" + Helper.getHeaderValue(req.getHeader(Helper.ServerSource))));

            req.removeFirst(ViaHeader.NAME);

            sipProvider.sendResponse(response);
        } catch (Throwable e) {
            e.printStackTrace();
            messageProcessor.processError("Can't send OK reply.");
        }
    }

    private void createResponse(RequestEvent evt, int response_status_code) {
        createResponse(evt.getRequest(), response_status_code, null);
    }

    private void createResponseForServerRegistered(RequestEvent evt, int response_status_code) throws ParseException {
        Header header = headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ServerRegister);
        createResponse(evt.getRequest(), response_status_code, header);
    }

    private void createResponseForClientRegistered(RequestEvent evt, int response_status_code) throws ParseException {
        Header header = headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ClientRegister);
        createResponse(evt.getRequest(), response_status_code, header);

    }

    private void createResponseForServerDeRegistered(RequestEvent evt, int response_status_code) throws ParseException {
        Header header = headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ServerDeRegister);
        createResponse(evt.getRequest(), response_status_code, header);
    }

    private void createResponseForClientDeRegistered(RequestEvent evt, int response_status_code) throws ParseException {
        Header header = headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ClientDeRegister);
        createResponse(evt.getRequest(), response_status_code, header);
    }

    private void createForwardResponse(ResponseEvent evt) {
        Response response;
        try {
            response = evt.getResponse();
            SipProvider p = (SipProvider) evt.getSource();
            response.removeFirst(ViaHeader.NAME);
            response.setHeader(headerFactory.createHeader(Helper.cachedHeader, getHost() + ":" + getPort()));
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
        request.setHeader(headerFactory.createHeader(Helper.ServerSource, serverAddress));

        sipProvider.sendRequest(request);

    }

    void CallDeRegisterRequest(String serverAddress) throws ParseException,
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

        request.addHeader(headerFactory.createHeader(SipRegister.RegisterHeader, SipRegister.ServerDeRegister));
        request.addHeader(headerFactory.createHeader(Helper.ServerSource, serverAddress));

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

            if (response.getHeader(SipRegister.RegisterHeader) == null) {
                messageProcessor.processInfo("--Sent");
                //response doesn't contain a header register
                CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
                String to = ((ToHeader) response.getHeader(ToHeader.NAME)).getAddress().getDisplayName();

                if (!clientRegistery.containsKey(to))
                    srvm.clientCached.put(to, new MyAddress(Helper.getHeaderValue(response.getHeader(Helper.cachedHeader))));

                System.out.println(clientRegistery.toString());

                if (clientRegistery.containsKey(to)) {
                    System.out.println("containes it");
                    clientRegistery.get(to).removeRequestEventWithCallIdHeader(callIdHeader);
                }

                createForwardResponse(evt);
            } else {

                if (Helper.getHeaderValue(response.getHeader(SipRegister.RegisterHeader)).equals(SipRegister.ServerDeRegister)) {

                    String severDesUri = Helper.getHeaderValue(response.getHeader(Helper.ServerSource));
                    boolean isOk = srvm.removeServer(new MyAddress(Helper.getAddressFromSipUri(severDesUri)), messageProcessor);
                    messageProcessor.processInfo("deRegistered in " + Helper.getUserNameFromSipUri(severDesUri));
                } else {
                    String severDesUri = Helper.getHeaderValue(response.getHeader(Helper.ServerSource));
                    boolean isOk = srvm.addServer(new MyAddress(Helper.getAddressFromSipUri(severDesUri)), messageProcessor);
                    messageProcessor.processInfo("registered in " + Helper.getUserNameFromSipUri(severDesUri));
                }
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
            switch (method) {
                case Request.REGISTER:
                    String registerHeaderValue = Helper.getRegisterHeaderValue(evt);
                    String senderAddress;
                    boolean isOk;
                    switch (registerHeaderValue) {
                        case SipRegister.ServerRegister:
                            System.out.println("fucking server");
                            senderAddress = Helper.getAddressFromSipUri(Sender.getAddress().getURI().toString());
                            isOk = srvm.addServer(Helper.getIpFromAddress(senderAddress), Helper.getPortFromAddress(
                                    senderAddress), messageProcessor);

                            if (isOk) {
                                System.out.println("server : registered the server");
                            } else {
                                System.out.println("server : already registered");
                            }

                            messageProcessor.processInfo("registered in " + Sender.getAddress().getDisplayName());
                            createResponseForServerRegistered(evt, 200);

                            break;

                        case SipRegister.ServerDeRegister:
                            System.out.println("fucking server");
                            senderAddress = Helper.getAddressFromSipUri(Sender.getAddress().getURI().toString());
                            isOk = srvm.removeServer(Helper.getIpFromAddress(senderAddress), Helper.getPortFromAddress(
                                    senderAddress), messageProcessor);

                            if (isOk) {
                                System.out.println("server : Deregistered the server");
                            } else {
                                System.out.println("server : already Deregistered");
                            }

                            messageProcessor.processInfo("deRegistered in " + Sender.getAddress().getDisplayName());
                            createResponseForServerDeRegistered(evt, 200);

                            break;
                        case SipRegister.ClientDeRegister:
                            System.out.println("fucking client");
                            if (SipRegister.deRegisterClient(req, clientRegistery)) {
                                messageProcessor.processClientDeReg(clientRegistery.keySet());
                                System.out.println("Deregistered the Client");
                            } else {
                                System.out.println("client : already Deregistered");
                            }

                            messageProcessor.processMessage(Sender.getAddress().getDisplayName(), "DE" + method);
                            createResponseForClientDeRegistered(evt, 200);
                            break;
                        default:
                            System.out.println("fucking client");
                            if (SipRegister.registerClient(req, clientRegistery, messageProcessor)) {
                                System.out.println("registered the client");
                            } else {

                                System.out.println("make client online");
                            }

                            messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                            createResponseForClientRegistered(evt, 200);
                            clientRegistery.get(Receiver).CheckMessageStorage(this);

                            break;
                    }


                    break;
                case Request.MESSAGE:
                    if (Helper.getFailHeaderValue(req) == null) {
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
                                    System.out.println("I have the receiver " + clientRegistery.get(Receiver).getAddress());
                                    forwardMessage(evt, clientRegistery.get(Receiver).getAddress(), true);
                                } else {

                                    //TODO: return error respone for routing

                                    srvm.sendWithRouting(evt, this);
                                    System.out.println("no such client, dropping.... may need to waite for registering");
                                }
                            } else {
                                // from client
                                messageProcessor.processMessage(Sender.getAddress().getDisplayName(), method);
                                System.out.println("Message is received from my client");
                                // if the receiver is also in my register
                                if (clientRegistery.containsKey(Receiver)) {

                                    forwardMessage(evt, clientRegistery.get(Receiver).getAddress(), true);
                                    System.out.println("Sent directly to receiver");
                                } else {
                                    // I don't know the receiver, I will send it to other servers
                                    srvm.sendWithRouting(evt, this);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("FAIL recieved");
                        srvm.sendWithRouting(evt, this);
                    }


                    break;
                default: //bad request type.
                    createResponse(evt, 400);

                    break;
            }
        } catch (Exception e) {
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
