package dev2dev.textclient;

import java.util.ArrayList;

/**
 * Created by hooman on 10/19/16.
 */
public class serverManagement {
    private ArrayList<serverAddressItem> servers = new ArrayList<>();

    public serverManagement () {
    }

    public boolean addServer(String ip, int port) {
        if (hasItem(ip, port))
            return false;
        servers.add(new serverAddressItem(ip, port));
        return true;
    }

    public boolean hasItem(String ip, int port) {
        for (int i = 0; i < servers.size(); ++i)
            if (servers.get(i).equals(ip, port))
                return true;
        return false;
    }

}
