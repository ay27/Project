package org.bitman.project.record;

import android.net.wifi.WifiManager;
import android.util.Log;
import org.bitman.project.ProjectApplication;
import org.bitman.project.http.GetIP;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtp.AbstractPacketizer;
import org.bitman.project.record.rtp.H264Packetizer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

public class Session {
    private static final String TAG = "Session";

    private final long TimeStamp;
    // client_port will be set in RtspServer->SETUP, will be used in rtp/Packetizer.
    // server_port will be set in rtp/RtpSocket or rtp/Packetizer, will be used in RtspServer.
    private int[] client_port = new int[]{0, 0}, server_port;
    private InetAddress destination = null;

    private CameraWorker worker = CameraWorker.getInstance();
    private AbstractPacketizer packetizer;

    // will be used in rtp/RTP_Sockrt & RtspServer
    public final int SSRC;
    public final InetAddress localAddress;
    // Only has one track.
    public final int trackID = 1;


    public Session() {
        localAddress = GetIP.getLocalIpAddress(true);
        long uptime = System.currentTimeMillis();
        TimeStamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000);

        SSRC = new Random().nextInt();

        try {
            packetizer = new H264Packetizer();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return;
        }
        server_port = new int[] {
                this.packetizer.getRtpSocket().getLocalPort(),
                this.packetizer.getRtcpSocket().getLocalPort()
        };
    }

    public InetAddress getDestination() { return destination; }
    public void setDestination(InetAddress destination) {
        this.destination = destination;
//        packetizer.setDestination(destination, ports[0], ports[1]);
    }

    public int[] getServer_port() { return server_port; }

    public int[] getClient_port() { return client_port; }
    public void setClient_port(int[] client_port) {
        if (client_port[0] % 2 == 1)
            this.client_port = new int[]{client_port[0]-1, client_port[0]};
        else
            this.client_port = client_port;
//        packetizer.setPorts(client_port[0], client_port[1]);
    }


    private WifiManager.MulticastLock mLock;
    public synchronized void start()
    {
        Log.i(TAG, "Session start.");
        if (destination.isMulticastAddress()) {
                // Aquire a MulticastLock to allow multicasted UDP packet
                WifiManager wifi = (WifiManager) ProjectApplication.instance.getSystemService(ProjectApplication.instance.WIFI_SERVICE);
                if(wifi != null){
                    mLock = wifi.createMulticastLock("org.bitman.streaming");
                    mLock.acquire();
                }
        }

        worker.start();
        // will delay a little time to wait the stream.
        //try { Thread.sleep(10); } catch (InterruptedException e) { }

        packetizer.setInputStream(worker.getStream());
//        packetizer.setStream(worker.getStream());
        packetizer.setSSRC(SSRC);
        packetizer.setDestination(destination, client_port[0], client_port[1]);
//        packetizer.setPorts(client_port[0], client_port[1]);
        try {
            packetizer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stop()
    {
        Log.i(TAG, "session stop.");
        if (mLock != null) {
            if (mLock.isHeld())
                mLock.release();
            mLock = null;
        }
        packetizer.stop();
        worker.stop();
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
