package dev2dev.textclient;

import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import java.text.ParseException;

public class Helper {

    // ************************************************ Sip Uri Helper *************************************************

    public static String getAddressFromSipUri(String uri) {
        return uri.substring(uri.indexOf("@") + 1);
    }

    public static String getUserNameFromSipUri(String uri) {
        return uri.substring(uri.indexOf(":") + 1, uri.indexOf("@"));
    }

    public static String getPortFromSipUri(String uri) {
        String temp = getAddressFromSipUri(uri);
        return temp.substring(temp.indexOf(":") + 1);
    }

    // *********************************************** Sip Header Helper ***********************************************

    public static ToHeader createToHeader(AddressFactory af, HeaderFactory hf, String to) throws ParseException {
        return createToHeader(af, hf, getAddressFromSipUri(to), getAddressFromSipUri(to));
    }

    public static FromHeader createFromHeader(AddressFactory af, HeaderFactory hf, String to) throws ParseException {
        return createFromHeader(af, hf, getAddressFromSipUri(to), getAddressFromSipUri(to));
    }

    public static ToHeader createToHeader(AddressFactory af, HeaderFactory hf, String username, String address)
            throws ParseException {
        Address toNameAddress = createSipAddress(af, username, address);
        ToHeader toHeader = hf.createToHeader(toNameAddress, null);
        return toHeader;
    }

    public static FromHeader createFromHeader(AddressFactory af, HeaderFactory hf, String username, String address)
            throws ParseException {
        Address fromNameAddress = createSipAddress(af, username, address);
        FromHeader fromHeader = hf.createFromHeader(fromNameAddress, null);
        return fromHeader;
    }

    // ********************************************** Sip Address Helper ***********************************************

    public static Address createSipAddress(AddressFactory af, String username, String address) throws ParseException {
        SipURI Address = af.createSipURI(username, address);
        Address NameAddress = af.createAddress(Address);
        NameAddress.setDisplayName(username);
        return NameAddress;
    }

    // ****************************************************** End ******************************************************
}