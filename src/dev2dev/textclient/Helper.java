package dev2dev.textclient;

import com.sun.deploy.util.StringUtils;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
class Helper {
    public static String FailHeader = "FailHeader";
    public static String cachedHeader = "cachedHeader";
    public static String ServerSource = "ServerSource";

    // ********************************************  Custom Header Helper **********************************************

    static String getHeaderValue(Header header) {
        if (header == null)
            return null;
        return header.toString().trim().split(" ")[1];
    }

    static String getRegisterHeaderValue(Request req) {
        return getHeaderValue(req.getHeader(SipRegister.RegisterHeader));
    }

    static String getRegisterHeaderValue(Response res) {
        return getHeaderValue(res.getHeader(SipRegister.RegisterHeader));
    }

    static String getRegisterHeaderValue(RequestEvent evt) {
        return getRegisterHeaderValue(evt.getRequest());
    }

    static String getRegisterHeaderValue(ResponseEvent ret) {
        return getRegisterHeaderValue(ret.getResponse());
    }

    static String getFailHeaderValue(Request req) {
        return getHeaderValue(req.getHeader(FailHeader));
    }

    static String getFailHeaderValue(Response res) {
        return getHeaderValue(res.getHeader(FailHeader));
    }

    static String getFailHeaderValue(RequestEvent evt) {
        return getFailHeaderValue(evt.getRequest());
    }

    static String getFailHeaderValue(ResponseEvent ret) {
        return getFailHeaderValue(ret.getResponse());
    }

    static ArrayList<MyAddress> getMyViaHeaderValue(Request req) {
        return getMyViaHeaderValue(req.getHeader(serverManagement.MyViaHeader));
    }

    static ArrayList<MyAddress> getMyViaHeaderValue(Response response) {
        return getMyViaHeaderValue(response.getHeader(serverManagement.MyViaHeader));
    }

    private static ArrayList<MyAddress> getMyViaHeaderValue(Header header) {
        if (header == null)
            return new ArrayList<>();

        String[] array = getHeaderValue(header).split(",");

        System.out.println("start myvia");
        System.out.println(getHeaderValue(header));
        System.out.println("end myvia");

        ArrayList<MyAddress> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(new MyAddress(array[i]));
        }

        return result;
    }

    static ArrayList<MyAddress> getMyViaHeaderValue(RequestEvent evt) {
        return getMyViaHeaderValue(evt.getRequest());
    }

    static ArrayList<MyAddress> getMyViaHeaderValue(ResponseEvent ret) {
        return getMyViaHeaderValue(ret.getResponse());
    }

    static Header createMyViaHeader(HeaderFactory hf, ArrayList<MyAddress> myViaHeader) throws Exception {
        String temp = "";
        for (int i = 0; i < myViaHeader.size(); i++) {
            temp = temp + myViaHeader.get(i).toString();
            if (i != myViaHeader.size() - 1) {
                temp = temp + ",";
            }
        }

        Header header = hf.createHeader(serverManagement.MyViaHeader, temp);
        return header;
    }


    // ******************************************** Sip Uri HeaderHelper ***********************************************

    static String getAddressFromSipUri(String uri) {
        return uri.substring(uri.indexOf("@") + 1);
    }

    public static String getUserNameFromSipUri(String uri) {
        return uri.substring(uri.indexOf(":") + 1, uri.indexOf("@"));
    }

    public static String getPortFromSipUri(String uri) {
        return getPortFromAddress(getAddressFromSipUri(uri));
    }

    // *************************************** Sip HeaderUtilities HeaderHelper ****************************************

    static ToHeader createToHeader(AddressFactory af, HeaderFactory hf, String to) throws ParseException {
        return createToHeader(af, hf, getUserNameFromSipUri(to), getAddressFromSipUri(to));
    }

    public static FromHeader createFromHeader(AddressFactory af, HeaderFactory hf, String from) throws ParseException {
        return createFromHeader(af, hf, getUserNameFromSipUri(from), getAddressFromSipUri(from));
    }

    static ToHeader createToHeader(AddressFactory af, HeaderFactory hf, String username, String address)
            throws ParseException {
        Address toNameAddress = createSipAddress(af, username, address);
        return hf.createToHeader(toNameAddress, null);
    }

    static FromHeader createFromHeader(AddressFactory af, HeaderFactory hf, String username, String address)
            throws ParseException {
        Address fromNameAddress = createSipAddress(af, username, address);
        return hf.createFromHeader(fromNameAddress, null);
    }

    // ******************************************* Sip Address HeaderHelper ********************************************

    static Address createSipAddress(AddressFactory af, String username, String address) throws ParseException {
        SipURI Address = af.createSipURI(username, address);
        javax.sip.address.Address NameAddress = af.createAddress(Address);
        NameAddress.setDisplayName(username);
        return NameAddress;
    }

    // ****************************************** String Address HeaderHelper ******************************************

    static String getPortFromAddress(String address) {
        return address.substring(address.indexOf(":") + 1);
    }

    static String getIpFromAddress(String address) {
        return address.substring(0, address.indexOf(":"));
    }

    // ****************************************************** End ******************************************************
}
