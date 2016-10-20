package dev2dev.textclient;

/**
 * Created by hooman on 10/16/16.
 */

@SuppressWarnings("unused")
public class seqManager {
    private static long seq = 0;

    public void setSeq() {
    }

    public synchronized long getGetSequenceNumber() {
        return seq++;
    }
}
