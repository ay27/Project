package org.bitman.project.record;

import android.util.Log;
import org.bitman.project.http.GetIP;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtp.Packetizer;
import org.bitman.project.record.rtp.RtpSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Session {

    private static final String TAG = "Session";

    private static Session instance = null;

    // SSRC will be set in rtp/Packetizer, will be used in rtp/RTP_Sockrt & RtspServer
    public int SSRC;
    private final long TimeStamp;

    public final InetAddress localAddress;
    // destination will be used in the RtspServer.
    public InetAddress destination;

    private Session() throws UnknownHostException {
        localAddress = InetAddress.getByName(GetIP.getLocalIpAddress(true));
        long uptime = System.currentTimeMillis();
        TimeStamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000);
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

    // the port of the rtsp, will be set in the ui/Settings, will be used in RtspServer.
    public int rtsp_port = 8554;
    // client_port will be set in RtspServer->SETUP, will be used in rtp/Packetizer.
    // server_port will be set in rtp/RtpSocket or rtp/Packetizer, will be used in RtspServer.
    public int[] client_port = new int[]{0, 0}, server_port;
    public final int trackID = 1;

    private CameraWorker worker = CameraWorker.getInstance();
    private RtpSocket socket = RtpSocket.getInstance();
    public void start()
    {
        worker.start();
        // will delay a little time to wait the stream.
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        Packetizer.getInstance().setDataStream(worker.getStream());
    }

    public void stop()
    {
        worker.stop();
        socket.stop();
    }

    public synchronized String getSessionDescription() throws IllegalStateException, IOException {
        if (destination==null) {
            throw new IllegalStateException("setDestination() has not been called !");
        }
        StringBuilder sessionDescription = new StringBuilder();
        sessionDescription.append("v=0\r\n");
        sessionDescription.append("o=- "+TimeStamp+" "+TimeStamp+" IN IP4 "+(localAddress==null?"127.0.0.1":localAddress.getHostAddress())+"\r\n");
        sessionDescription.append("s=Unnamed\r\n");
        sessionDescription.append("i=N/A\r\n");
        sessionDescription.append("c=IN IP4 "+destination.getHostAddress()+"\r\n");
        sessionDescription.append("t=0 0\r\n");
        sessionDescription.append("a=recvonly\r\n");

        sessionDescription.append(VideoQuality.getInstance().getDescription());
        sessionDescription.append("a=control:trackID="+trackID+"\r\n");
        return sessionDescription.toString();
    }

}
