package org.bitman.project.record.rtp;

// Copy from spydroid project.
public class RtpSocket implements Runnable {
    private static final String TAG = "RtpSocket";

    public static final int RTP_HEADER_LENGTH = 12;

    // the frequency of clock
    private static final long mClock = 90000;

    private static RtpSocket instance = null;
    private RtpSocket() {

    }

    public static RtpSocket getInstance()
    {
        if (instance == null)
            return (instance = new RtpSocket());
        else return instance;
    }

    public void stop()
    {

    }


    @Override
    public void run() {

    }

    // TODO
    public byte[] requestBuffer() {
        return new byte[0];
    }

    public void updateTimeStamp(long ts) {

    }

    public void markNextPacket() {

    }

    public void commitBuffer(int i) {

    }
}
