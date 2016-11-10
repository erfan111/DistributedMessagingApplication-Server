package dev2dev.textclient;

import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import java.util.ArrayList;

class Client {

    private String username;
    private MyAddress address;
    private ArrayList<RequestEvent> requestEvents;
    private boolean onlineStatus;

    Client(String username, MyAddress address, boolean onlineStatus){
        this.username = username;
        this.requestEvents = new ArrayList<>();
        this.address = address;
        this.onlineStatus = onlineStatus;
    }

    public Client(String username, MyAddress address){
        this(username, address, false);
    }

    public String getUserName(){
        return username;
    }

    MyAddress getAddress(){
        return address;
    }

    boolean getOnlineStatus(){
        return onlineStatus;
    }

    void setOnlineStatus(boolean onlineStatus){
        this.onlineStatus = onlineStatus;
    }

    void CheckMessageStorage(SipLayer sip){

        for (RequestEvent requestEvent : requestEvents) {
            try {
                sip.forwardMessage(requestEvent, address, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void removeRequestEventWithCallIdHeader(CallIdHeader callIdHeader){
            System.out.println("inside remove req evt");
            for (int i = 0; i < requestEvents.size(); i++) {
                System.out.println("removing req event from storage");
                CallIdHeader callIdHeaderTemp = (CallIdHeader) requestEvents.get(i).getRequest().getHeader(CallIdHeader.NAME);
                if (callIdHeader.getCallId().equals(callIdHeaderTemp.getCallId())) {
                    requestEvents.remove(requestEvents.get(i));

                    break;
                } else {
                    System.out.println("big bug");
                }
            }

    }

    private boolean hasItem(RequestEvent re){
        for (RequestEvent requestEvent : requestEvents) {
            if (((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId().equals(
                    ((CallIdHeader) re.getRequest().getHeader(CallIdHeader.NAME)).getCallId()
            )) {
                return true;
            }
        }
        return false;
    }

    void addRequestEvent(RequestEvent re){
        if (!hasItem(re)){
            this.requestEvents.add(re);
        }
    }
}
