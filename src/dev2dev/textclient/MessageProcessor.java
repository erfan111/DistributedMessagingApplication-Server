package dev2dev.textclient;

import java.util.Set;

interface MessageProcessor {
    void processMessage(String sender, String message);

    void processError(String errorMessage);

    void processInfo(String infoMessage);

    void processClientReg(String client);

    void processClientDeReg(Set<String> clients);

    void processServerReg(String server);

    void processServerDeReg(Set<String> servers);
}
