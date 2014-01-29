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
    public int SSRC;

    public final InetAddress origin;
    public InetAddress destination;

    private Session() throws UnknownHostException {
        origin = InetAddress.getByName(GetIP.getLocalIpAddress(true));
    }
    public static Session getInstance()
    {
        if (instance == null)
            try {
                return (instance = new Session());
            } catch (UnknownHostException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        else
            return instance;
    }

    // the port of the rtsp
    public int rtsp_port = 8554;
    // the data port
    public int[] client_port, server_port;

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

}
