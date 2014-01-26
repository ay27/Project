package org.bitman.project.record;

public class Session {

    private static final String TAG = "Session";

    private static Session instance = null;
    private Session() { }
    public static Session getInstance()
    {
        if (instance == null)
            return (instance = new Session());
        else
            return instance;
    }

    private int rtsp_port = 8554;

    public int getRtsp_port() { return rtsp_port; }
    public void setRtsp_port(int rtsp_port) { this.rtsp_port = rtsp_port; }
}
