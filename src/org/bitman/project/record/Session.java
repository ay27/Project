package org.bitman.project.record;

import android.util.Log;
import org.bitman.project.http.GetIP;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Session {

    private static final String TAG = "Session";

    private static Session instance = null;
    private Session() {
        try {
            origin = InetAddress.getByName(GetIP.getLocalIpAddress(true));
        } catch (UnknownHostException e) {
            Log.e(TAG, "in Session(): "+e.toString());
        }
    }
    public static Session getInstance()
    {
        if (instance == null)
            return (instance = new Session());
        else
            return instance;
    }

    // the port of the rtsp
    private int rtsp_port = 8554;
    // the data port
    private int client_port, server_port;
    public int getRtsp_port() { return rtsp_port; }
    public void setRtsp_port(int rtsp_port) { this.rtsp_port = rtsp_port; }

    private InetAddress origin;
    private InetAddress destination;
    public void start()
    {

    }

    public void stop()
    {

    }

    public String getSessionDescription()
    {
        return null;
    }
}
