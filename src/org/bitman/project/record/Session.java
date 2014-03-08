package org.bitman.project.record;

import android.util.Log;
import org.bitman.project.http.GetIP;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtp.Packetizer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

public class Session {

    private static final String TAG = "Session";

    private static Session instance = null;

    // will be used in rtp/RTP_Sockrt & RtspServer
    private int SSRC = new Random().nextInt();
    private final long TimeStamp;

    public final InetAddress localAddress;
    // Only has one track.
    public final int trackID = 1;

    // client_port will be set in RtspServer->SETUP, will be used in rtp/Packetizer.
    // server_port will be set in rtp/RtpSocket or rtp/Packetizer, will be used in RtspServer.
    private int[] client_port = new int[]{0, 0}, server_port;

    private CameraWorker worker = CameraWorker.getInstance();
    private Packetizer packetizer;

    // a simple proxy.
    public InetAddress getDestination() { return packetizer.getDestination(); }
    public void setDestination(InetAddress destination) { packetizer.setDestination(destination); }

    public int getSSRC() { return SSRC; }
    public int[] getServer_port() { return server_port; }

    public int[] getClient_port() { return client_port; }
    public void setClient_port(int[] client_port) {
        if (client_port[0] % 2 == 1)
            this.client_port = new int[]{client_port[0]-1, client_port[0]};
        else
            this.client_port = client_port;
        packetizer.setPorts(client_port[0], client_port[1]);
    }

    private Session() throws Exception {
        localAddress = InetAddress.getByName(GetIP.getLocalIpAddress(true));
        long uptime = System.currentTimeMillis();
        TimeStamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000);

        packetizer = new Packetizer();
        server_port = packetizer.getPorts();
    }
    public static Session getInstance()
    {
        if (instance == null)
            try {
                return (instance = new Session());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return null;
            }
        else
            return instance;
    }

    public void start()
    {
        Log.i(TAG, "Session start.");
        // TODO: for test.
        worker.start();
        // will delay a little time to wait the stream.
        try { Thread.sleep(10); } catch (InterruptedException e) { }

        packetizer.setStream(worker.getStream());
        packetizer.setSSRC(SSRC);
        packetizer.start();
    }

    public void stop()
    {
        Log.i(TAG, "session stop.");
        worker.stop();
        packetizer.stop();
    }

    public synchronized String getSessionDescription() throws IllegalStateException, IOException {
        if (getDestination()==null) {
            throw new IllegalStateException("setDestination() has not been called !");
        }
        StringBuilder sessionDescription = new StringBuilder();
        sessionDescription.append("v=0\r\n");
        sessionDescription.append("o=- "+TimeStamp+" "+TimeStamp+" IN IP4 "+(localAddress==null?"127.0.0.1":localAddress.getHostAddress())+"\r\n");
        sessionDescription.append("s=Unnamed\r\n");
        sessionDescription.append("i=N/A\r\n");
        sessionDescription.append("c=IN IP4 "+getDestination().getHostAddress()+"\r\n");
        sessionDescription.append("t=0 0\r\n");
        sessionDescription.append("a=recvonly\r\n");

        sessionDescription.append(VideoQuality.getInstance().getDescription(client_port[0]));
        sessionDescription.append("a=control:trackID="+trackID+"\r\n\r\n");
        return sessionDescription.toString();
    }

}
