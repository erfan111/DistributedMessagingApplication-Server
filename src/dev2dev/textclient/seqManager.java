package dev2dev.textclient;

@SuppressWarnings("unused")
public class seqManager {
    private static long seq = 0;

    public void setSeq() {
    }

    public synchronized long getGetSequenceNumber() {
        return seq++;
    }
}
