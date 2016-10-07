package dev2dev.textclient;

import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.parser.ViaParser;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
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
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

@SuppressWarnings("ALL")
public class SipLayer implements SipListener {

	private HashMap<String, String> register = new HashMap<>();

    private MessageProcessor messageProcessor;

    private String username;

    private SipStack sipStack;

    private SipFactory sipFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;
	public RequestEvent storedEvent;

    /** Here we initialize the SIP stack. */
    public SipLayer(String username, String ip, int port)
	    throws PeerUnavailableException, TransportNotSupportedException,
	    InvalidArgumentException, ObjectInUseException,
	    TooManyListenersException {


	register.put("client1", "127.0.1.1:5061");
	register.put("client2", "127.0.1.1:5062");
	setUsername(username);
	sipFactory = SipFactory.getInstance();
	sipFactory.setPathName("gov.nist");
	Properties properties = new Properties();
	properties.setProperty("javax.sip.STACK_NAME", "TextClient");
	properties.setProperty("javax.sip.IP_ADDRESS", ip);

	//DEBUGGING: Information will go to files 
	//textclient.log and textclientdebug.log
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
		"textclient.txt");
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
		"textclientdebug.log");

	sipStack = sipFactory.createSipStack(properties);
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

    /**
     * This method uses the SIP stack to send a message. 
     */
    public void sendMessage(Request req, FromHeader Sender, String to, String message) throws ParseException,
	    InvalidArgumentException, SipException {

	SipURI from = addressFactory.createSipURI(getUsername(), getHost()
		+ ":" + getPort());
	Address fromNameAddress = addressFactory.createAddress(from);
	fromNameAddress.setDisplayName(getUsername());
//	FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
//		"textclientv1.0");

	String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
	String address = to.substring(to.indexOf("@") + 1);

	SipURI toAddress = addressFactory.createSipURI(username, address);
	Address toNameAddress = addressFactory.createAddress(toAddress);
	toNameAddress.setDisplayName(username);
	ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

	SipURI requestURI = addressFactory.createSipURI(username, address);
	requestURI.setTransportParam("udp");

	ListIterator viaListIter = req.getHeaders("Via");
	ViaHeader Via;
	ArrayList viaHeader = new ArrayList();
	System.out.println("test");

	while(viaListIter.hasNext())
	{
		Via = (ViaHeader) viaListIter.next();
		System.out.println(Via.getHost() +  " " + Via.getMAddr() + " " +  Via.getPort());
		System.out.println("salam");
		ViaHeader myviaHeader = headerFactory.createViaHeader(Via.getHost(),
				Via.getPort(), "udp", "branch1");
		viaHeader.add(myviaHeader);
	}
	ViaHeader	myviaHeader = headerFactory.createViaHeader(getHost(),
			getPort(), "udp", "branch1");
	viaHeader.add(myviaHeader);
	System.out.println("bye");



	CallIdHeader callIdHeader = sipProvider.getNewCallId();

	CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
		Request.MESSAGE);

	MaxForwardsHeader maxForwards = headerFactory
		.createMaxForwardsHeader(70);

	Request request = messageFactory.createRequest(requestURI,
		Request.MESSAGE, callIdHeader, cSeqHeader, Sender,		// **
		toHeader, viaHeader, maxForwards);

	SipURI contactURI = addressFactory.createSipURI(getUsername(),
		getHost());
	contactURI.setPort(getPort());
	Address contactAddress = addressFactory.createAddress(contactURI);
	contactAddress.setDisplayName(getUsername());
	ContactHeader contactHeader = headerFactory
		.createContactHeader(contactAddress);
	request.addHeader(contactHeader);

	ContentTypeHeader contentTypeHeader = headerFactory
		.createContentTypeHeader("text", "plain");
	request.setContent(message, contentTypeHeader);

	sipProvider.sendRequest(request);
    }


    /** This method is called by the SIP stack when a response arrives. */
    public void processResponse(ResponseEvent evt) {
	Response response = evt.getResponse();
	int status = response.getStatusCode();

	if ((status >= 200) && (status < 300)) { //Success!
	    messageProcessor.processInfo("--Sent");
		/////////////////
		Request req = storedEvent.getRequest();

		String method = req.getMethod();
		if (!method.equals("MESSAGE")) { //bad request type.
			messageProcessor.processError("Bad request type: " + method);
			return;
		}

		FromHeader from = (FromHeader) req.getHeader("From");
		messageProcessor.processMessage(from.getAddress().toString(),
				new String(req.getRawContent()));
		Response response2 = null;
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
		storedEvent = evt;
		Request req = evt.getRequest();

		String method = req.getMethod();
		int at = evt.getRequest().getRequestURI().toString().indexOf("@");
		System.out.println(evt.getRequest().getRequestURI().toString().substring(4, at));
		String Receiver = evt.getRequest().getRequestURI().toString().substring(4, at);
		FromHeader Sender = (FromHeader) evt.getRequest().getHeader("From");
		System.out.println(Sender);

		if(method.equals("MESSAGE"))
		{
			try {
				messageProcessor.processMessage("Event",  method);
				System.out.println("salam123123");

				sendMessage(req ,Sender, "sip:" + Receiver + '@' + register.get(Receiver), evt.getRequest().getContent().toString());
//				createResponse(req, 100);


			} catch (ParseException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
			System.out.println("msg");

		}

	else { //bad request type.
	    messageProcessor.processError("Bad request type: " + method);
	    return;
	}
		//createResponse(req, 500);

    }

	public void createResponse(Request req, int response_status_code)
	{
		FromHeader from = (FromHeader) req.getHeader("From");
//		messageProcessor.processMessage(from.getAddress().toString(),
//				new String(req.getRawContent()));
		Response response = null;

		try {
			response = messageFactory.createResponse(response_status_code, req);
			ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
			toHeader.setTag("888"); //This is mandatory as per the spec.
			ServerTransaction st = sipProvider.getNewServerTransaction(req);
			st.sendResponse(response);
			System.out.println("sent trying");
		} catch (Throwable e) {
			e.printStackTrace();
			messageProcessor.processError("Can't send OK reply.");
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

    public String getHost() {
	int port = sipProvider.getListeningPoint().getPort();
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

    public MessageProcessor getMessageProcessor() {
	return messageProcessor;
    }

    public void setMessageProcessor(MessageProcessor newMessageProcessor) {
	messageProcessor = newMessageProcessor;
    }

}
