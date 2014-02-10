package org.bitman.project.record.rtp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class RtpSocket implements Runnable {
    private static final String TAG = "RtpSocket";

    public static final int RTP_HEADER_LENGTH = 12;
    public static final int MTU = 1500;

    // the frequency of clock, the H264 video stream is 90000Hz.
    private static final long mClock = 90000;
    private static final int cacheSize = 20;
    private static final int bufferCount = 500;

    // the sequence number.
    private int seqNumber = 0;

    private byte[][] buffers;
    private Semaphore bufferRequest;
    private Semaphore bufferCommit;
    private DatagramPacket[] packets;
    private MulticastSocket socket;
    private long[] timeStamps;
    private int bufferIn, bufferOut;
    private int SSRC = 0;
    private int port = 0;
    private InetAddress destination = null;

    private static RtpSocket instance = null;
    private RtpSocket() throws IOException {
        buffers = new byte[bufferCount][];
        bufferRequest = new Semaphore(bufferCount);
        bufferCommit = new Semaphore(0);
        bufferIn = bufferOut = 0;
        packets = new DatagramPacket[bufferCount];
        timeStamps = new long[bufferCount];

        for (int i = 0; i < bufferCount; i++) {
            buffers[i] = new byte[MTU];
            packets[i] = new DatagramPacket(buffers[i], 1);

            // set the header.
            // Version(2), Padding(0), Extension(0), Source Identifier(0)
            buffers[i][0] = (byte) Integer.parseInt("10000000",2);
			/* Payload Type */
            buffers[i][1] = (byte) 96;
        }

        socket = new MulticastSocket();
    }

    public static RtpSocket getInstance()
    {
        if (instance == null)
            try {
                return (instance = new RtpSocket());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        else return instance;
    }

    public int getLocalPort() { return socket.getLocalPort(); }
    public void setPort(int port) {
        this.port = port;
        for (int i = 0; i < bufferCount; i++) {
            packets[i].setPort(port);
        }
    }
    public void setDestination(InetAddress destination) {
        this.destination = destination;
        for (int i = 0; i < bufferCount; i++) {
            packets[i].setAddress(destination);
        }
    }
    public InetAddress getDestination() { return destination; }

    /* Byte 4,5,6,7    ->  Timestamp                         */
    public void updateTimeStamp(long ts) {
        timeStamps[bufferIn] = ts;
        setLong(buffers[bufferIn], ts*mClock/1000000000L, 4, 8);
    }
    /* Byte 8,9,10,11  ->  Sync Source Identifier            */
    public void setSSRC(int SSRC) {
        this.SSRC = SSRC;
        for (int i = 0; i < bufferCount; i++)
            setLong(buffers[i], SSRC, 8, 12);
    }

    public byte[] requestBuffer() {
        try {
            bufferRequest.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        buffers[bufferIn][1] &= 0x7F;
        return buffers[bufferIn];
    }

    public void markNextPacket() {
        buffers[bufferIn][1] |= 0x80;
    }

    private Thread thread;
    public void commitBuffer(int length) {
        /* Byte 2,3        ->  Sequence Number                   */
        setLong(buffers[bufferIn], ++seqNumber, 2, 4);

        packets[bufferIn].setLength(length);
        bufferCommit.release();

        if (++bufferIn>=bufferOut) bufferIn = 0;

        if (thread == null)
            thread = new Thread(this);
        thread.start();
    }

    public void stop() { socket.close(); }

    private long oldTimeStamp = 0;
    @Override
    public void run() {
        Statistics stats = new Statistics(50, 3300);
        try {
            Thread.sleep(cacheSize);
            while (bufferCommit.tryAcquire(4, TimeUnit.SECONDS)) {
                if (oldTimeStamp > 0 && timeStamps[bufferOut]-oldTimeStamp > 0)
                {
                    stats.push(timeStamps[bufferOut]-oldTimeStamp);
                    long d = stats.average()/1000000;
                    Thread.sleep(d);
                }
            }
            oldTimeStamp = timeStamps[bufferOut];
            socket.send(packets[bufferOut]);
            if (++bufferOut>=bufferCount) bufferOut = 0;
            bufferRequest.release();
        } catch (Exception e)
        { Log.e(TAG, e.toString()); }

        thread = null;
    }

    private void setLong(byte[] bytes, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            bytes[end] = (byte) (n % 256);
            n >>= 8;
        }
    }


}
