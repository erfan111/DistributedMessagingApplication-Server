package dev2dev.textclient;

/**
 * Created by hooman on 10/16/16.
 */
public class seqManager {
    private static long seq = 0;

    public void setSeq() {
    }

    public synchronized long getGetSequnceNumber() {
        return this.seq++;
    }
}
