package org.bitman.project.record.rtp;

import java.io.InputStream;

public class RTP_Socket {
    private static final String TAG = "RTP_Socket";

    private InputStream dataStream;

    public void setDataStream(InputStream dataStream) { this.dataStream = dataStream; }


    private static RTP_Socket instance = null;
    private RTP_Socket() { }
    public static RTP_Socket getInstance()
    {
        if (instance == null)
            return (instance = new RTP_Socket());
        else return instance;
    }
    public void stop()
    {

    }
}
