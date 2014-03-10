package org.bitman.project.record.rtp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Random;

public class Packetizer implements Runnable {
    private static final String TAG = "Packetizer";

    private static final int MaxPacketizerSize = 1400;
    private static final int rtphl = RtpSocket.RTP_HEADER_LENGTH;
    private static final int intervalBetweenReports = 5000;

    private RtpSocket rtpSocket;
    private long ts;
    private InputStream cameraStream;
    /** Used in packetizers to estimate timestamps in RTP packets. */
    private Statistics statistics = new Statistics();

    // work for RtcpSenderReport
    private int reportDelta = 0;
    private final RtcpSenderReport report;

    public Packetizer() {
        try {
            rtpSocket = new RtpSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        report = new RtcpSenderReport();
        ts = Math.abs(new Random().nextInt());
    }

    public void setStream(InputStream is) { this.cameraStream = is; }

    //public InetAddress getDestination() { return rtpSocket.getDestination(); }
    public void setDestination(InetAddress destination) {
        Log.i(TAG, "set destination: "+destination);
        rtpSocket.setDestination(destination);
        report.setDestination(destination);
    }

    public int[] getPorts() {
        return new int[] { rtpSocket.getLocalPort(), report.getLocalPort() };
    }

    public void setPorts(int rtpPort, int rtcpPort) {
        rtpSocket.setPort(rtpPort);
        report.setPort(rtcpPort);
    }

    public void setSSRC(int SSRC) {
        report.setSSRC(SSRC);
        rtpSocket.setSSRC(SSRC);
    }


    private Thread mThread = null;
    public void start()
    {
        Log.i(TAG, "packetizer start.");
        if (mThread == null)
        {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    public void stop()
    {
        if (mThread != null) {
            mThread.interrupt();
            try {
                mThread.join(1000);
            } catch (InterruptedException ignored) {}
            mThread = null;
        }
        rtpSocket.stop();
        report.stop();
    }

    private long oldTime = 0;
    private long delay = 0;

    @Override
    public void run() {
        long duration = 0;
        Log.d(TAG,"H264 packetizer started !");
        statistics.reset();
        // This will skip the MPEG4 header if this step fails we can't stream anything :(
        try {
            byte buffer[] = new byte[4];
            // Skip all atoms preceding mdat atom
            while (!Thread.interrupted()) {
                while (cameraStream.read() != 'm');
                cameraStream.read(buffer, 0, 3);
                if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
            }
        } catch (IOException e) {
            Log.e(TAG,"Couldn't skip mp4 header :/");
            return;
        }

        // We read a NAL units from the input stream and we send them
        try {
            while (!Thread.interrupted()) {

                // We measure how long it takes to receive the NAL unit from the phone
                oldTime = System.nanoTime();
                send();
                duration = System.nanoTime() - oldTime;

                reportDelta += duration/1000000;
                if (intervalBetweenReports>0) {
                    if (reportDelta>=intervalBetweenReports) {
                        // We send a Sender Report
                        report.send(oldTime+duration,ts*90/1000000);
                    }
                }

                statistics.push(duration);
                // Computes the average duration of a NAL unit
                delay = statistics.average();
                Log.i(TAG,"duration: "+duration/1000000+" delay: "+delay/1000000);

            }
        } catch (IOException e) {
        } catch (InterruptedException e) {}

        Log.d(TAG,"H264 packetizer stopped !");

    }

    /**
     * Reads a NAL unit in the FIFO and sends it.
     * If it is too big, we split it in FU-A units (RFC 3984).
     */
    private byte[] header;
    private int naluLength = 0;
    private byte[] buffer;
    private void send() throws IOException, InterruptedException {

        int sum = 1, len = 0, type;
        header = new byte[5];

        // Read NAL unit length (4 bytes) and NAL unit header (1 byte)
        fill(header,0,5);
        Log.i(TAG, "NAL header: "+header[0]+header[1]+header[2]+header[3]+header[4]);
        naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
        //naluLength = is.available();

        if (naluLength>100000 || naluLength<0) resync();

        // Parses the NAL unit type
        type = header[4]&0x1F;

        // Updates the timestamp
        ts += delay;

        Log.i(TAG, Long.toString(ts));
        Log.i(TAG,"- Nal unit length: " + naluLength + " delay: "+delay/1000000+" type: "+type);

        // Small NAL unit => Single NAL unit
        if (naluLength<=MaxPacketizerSize-rtphl-2) {
            buffer = rtpSocket.requestBuffer();
            buffer[rtphl] = header[4];
            len = fill(buffer, rtphl+1,  naluLength-1);
            rtpSocket.updateTimestamp(ts);
            rtpSocket.markNextPacket();
            this.send(naluLength + rtphl);
            Log.i(TAG,"----- Single NAL unit - len:"+len+" delay: "+delay);
        }
        // Large NAL unit => Split nal unit
        else {

            // Set FU-A header
            header[1] = (byte) (header[4] & 0x1F);  // FU header type
            header[1] += 0x80; // Start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            // ������+��������|
            header[0] |= 28;

            while (sum < naluLength) {
                buffer = rtpSocket.requestBuffer();
                buffer[rtphl] = header[0];
                buffer[rtphl+1] = header[1];
                rtpSocket.updateTimestamp(ts);
                if ((len = fill(buffer, rtphl+2,  naluLength-sum > MaxPacketizerSize-rtphl-2 ? MaxPacketizerSize-rtphl-2 : naluLength-sum  ))<0) return; sum += len;
                // Last packet before next NAL
                if (sum >= naluLength) {
                    // End bit on
                    buffer[rtphl+1] += 0x40;
                    rtpSocket.markNextPacket();
                }
                this.send(len + rtphl + 2);
                // Switch start bit
                header[1] = (byte) (header[1] & 0x7F);
                Log.i(TAG,"----- FU-A unit, sum:"+sum);
            }
          }
    }

    private void send(int length) throws IOException {
        rtpSocket.commitBuffer(length);
        report.update(length);
    }

    private int fill(byte[] buffer, int offset,int length) throws IOException {
        int sum = 0, len;

        while (sum<length) {
            len = cameraStream.read(buffer, offset+sum, length-sum);
            if (len<0) {
                throw new IOException("End of stream");
            }
            else sum+=len;
        }

        return sum;

    }

    private void resync() throws IOException {
        byte[] header = new byte[5];
        int type;

        Log.e(TAG,"Packetizer out of sync ! Let's try to fix that...");

        while (true) {

            header[0] = header[1];
            header[1] = header[2];
            header[2] = header[3];
            header[3] = header[4];
            header[4] = (byte) cameraStream.read();

            type = header[4]&0x1F;

            if (type == 5 || type == 1) {
                naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
                if (naluLength>0 && naluLength<100000) {
                    oldTime = System.nanoTime();
                    Log.e(TAG,"A NAL unit may have been found in the bit stream !");
                    break;
                }
            }

        }

    }
}
