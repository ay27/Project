package org.bitman.project.record;

import android.util.Log;
import org.bitman.project.http.GetIP;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtp.RTP_Socket;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Session {

    private static final String TAG = "Session";

    private static Session instance = null;
    private int SSRC;

    private InetAddress origin;
    private InetAddress destination;

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
    public int[] client_port, server_port;
    public int getRtsp_port() { return rtsp_port; }
    public void setRtsp_port(int rtsp_port) { this.rtsp_port = rtsp_port; }


    public void setDestination(InetAddress destination) { this.destination = destination; }
    public InetAddress getDestination() { return destination; }

    private CameraWorker worker = CameraWorker.getInstance();
    private RTP_Socket socket = RTP_Socket.getInstance();
    public void start()
    {
        worker.start();
        // will delay a little time to wait the stream.
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        socket.setDataStream(worker.getStream());
    }

    public void stop()
    {
        worker.stop();
        socket.stop();
    }

    // TODO finish it, and add the pps & sps data, which will be get in the VideoQuality.
    public String getSessionDescription()
    {
        return null;
    }

    public int getSSRC() {
        return SSRC;
    }
}
