package dev2dev.textclient;

import gov.nist.javax.sip.header.To;
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

	private Hashtable<String, String> servers = new Hashtable<>();

	private Hashtable<String, String> register = new Hashtable<>();

    private MessageProcessor messageProcessor;

    private String username;

    private SipStack sipStack;

    private SipFactory sipFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;
	public RequestEvent storedEvent;

	public SipRegister sipRegister;

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

//		sipRegister = new SipRegister()
		if(servers.containsKey(getUsername()))
		{

		}
		if(getPort() == 5063)
		{
			servers.put("server2", getHost()+ ":" + (getPort() + 1));
		}
		else
		{
			servers.put("server", getHost()+ ":" + (getPort() - 1));
		}
    }

    /**
     * This method uses the SIP stack to send a message. 
     */
    public void sendMessage(Request req, FromHeader Sender, String to, String message) throws ParseException,
	    InvalidArgumentException, SipException {
//	SipURI from = addressFactory.createSipURI(getUsername(), getHost()
//		+ ":" + getPort());
//	Address fromNameAddress = addressFactory.createAddress(from);
//	fromNameAddress.setDisplayName(getUsername());
		System.out.println("SendMessage called with: " + to + message);

	ToHeader toHeader = Helper.createToHeader(addressFactory, headerFactory, to);

	SipURI requestURI = addressFactory.createSipURI(username, Helper.getAddressFromSipUri(to));
	requestURI.setTransportParam("udp");

	ListIterator viaListIter = req.getHeaders("Via");
	ViaHeader Via;
	ArrayList viaHeader = new ArrayList();

	while(viaListIter.hasNext())
	{
		Via = (ViaHeader) viaListIter.next();
		System.out.println(Via.getHost() +  " " + Via.getMAddr() + " " +  Via.getPort());
		ViaHeader myviaHeader = headerFactory.createViaHeader(Via.getHost(),
				Via.getPort(), "udp", "branch1");
		viaHeader.add(myviaHeader);
	}
	ViaHeader	myviaHeader = headerFactory.createViaHeader(getHost(),
			getPort(), "udp", "branch1");
	viaHeader.add(myviaHeader);



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

		System.out.println("sent");
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

		// Getting the Sender and Receiver name
		String method = req.getMethod();
		String Receiver = ((To) req.getHeader(To.NAME)).getAddress().getDisplayName();
		FromHeader Sender = (FromHeader) evt.getRequest().getHeader("From");

		if(method.equals("MESSAGE"))
		{
			try {
				// CHECK IF THE MESSAGE HAS COME FROM THE SERVER
				ListIterator viaListIter = req.getHeaders("Via");
				ViaHeader Via;
				ArrayList viaHeader = new ArrayList();
				String Host;
				Boolean from_me = false;
				Boolean from_other_server = false;
				while(viaListIter.hasNext())
				{
					Via = (ViaHeader) viaListIter.next();
					if(getUsername().equals("server"))
					{
						Host = "server2";
					}
					else
					{
						Host = "server";
					}
					System.out.println("other server: " + servers.get(Host).split(":")[1] + " and Via port: " + Via.getPort());
					// if message is comming from other server;
					if( Integer.parseInt(servers.get(Host).split(":")[1]) == Via.getPort()){
						System.out.println("in az server umade, baraye cliente man ");
						from_other_server = true;
						break;
					}
					if( getPort() == Via.getPort()){
						from_me = true;
					}
				}
				if(from_other_server){
					System.out.println("from other server" + (Receiver) + " " +  from_me);
					// If receiver is in my registered clients list
					//register.get(Receiver)
					if(!from_me && register.containsKey(Receiver)){
//						String fucker = register.get(Receiver);
						System.out.println("I have the receiver " + register.get(Receiver));
						sendMessage(req ,Sender, "sip:" + Receiver + '@' + register.get(Receiver), evt.getRequest().getContent().toString());
					}
					// Drop the message TODO: Save the message and send it if the client registers later
					else{
						System.out.println("no such client, dropping....");
					}
				}
				else // from client
				{
					// check the register for the sender
					if(SipRegister.processRequest(req, register)){
						System.out.println("registered the sender");
					}
					else {
						System.out.println("already registered");
					}
					messageProcessor.processMessage("Event",  method);
					System.out.println("Message is received from my client");
					// if the receiver is also in my register
					if(register.containsKey(Receiver)){
						sendMessage(req ,Sender, "sip:" + Receiver + '@' + register.get(Receiver), evt.getRequest().getContent().toString());
						System.out.println("Sent directly to receiver");
					}
					// I don't know the receiver, I will send it to other servers
					else {
						ViaHeader vh = (ViaHeader)req.getHeader(ViaHeader.NAME);
						if(getUsername().equals("server"))
						{
							//sendMessage(req ,Sender, "sip:" + Receiver + '@' + "127.0.1.1:5064", evt.getRequest().getContent().toString()); //TODO:FIX SERVER ADDRESS
							System.out.println("fuck " + servers.get("server2"));
							sendMessage(req ,Sender, "sip:" + Receiver + '@' + servers.get("server2"), evt.getRequest().getContent().toString()); //TODO:FIX SERVER ADDRESS
						}
						else
						{
							sendMessage(req ,Sender, "sip:" + Receiver + '@' + servers.get("server"), evt.getRequest().getContent().toString()); //TODO:FIX SERVER ADDRESS
						}
					}
				}


//				createResponse(req, 100);


			} catch (Exception e) {
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
		FromHeader from = (FromHeader) req.getHeader(FromHeader.NAME);
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

	private SipStack createSipStack(String ip) throws PeerUnavailableException {

		Properties properties = new Properties();

		properties.setProperty("javax.sip.STACK_NAME", "TextClient");
		properties.setProperty("javax.sip.IP_ADDRESS", ip);
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "textclient.txt");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "textclientdebug.log");

		return sipFactory.createSipStack(properties);
	}
}
