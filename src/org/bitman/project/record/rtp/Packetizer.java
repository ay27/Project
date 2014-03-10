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

    public InetAddress getDestination() { return rtpSocket.getDestination(); }
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
        statistics.reset();

        Log.i(TAG, "start to skip the MPEG-4 header.");
        // skip the MPEG-4 header
        try {
            byte[] bytes = new byte[4];
            while (true)
            {
                if (cameraStream == null)
                    Log.e(TAG, "cameraStream is null.");
                while (cameraStream.read() != 'm');
                cameraStream.read(bytes, 0, 3);
                if (bytes[0]=='d' && bytes[1]=='a' && bytes[2]=='t') break;
            }
        } catch (Exception e)
        {
            Log.e(TAG, e.toString());
            Log.e(TAG, "could not skip the MPEG-4 header");
            return;
        }

        Log.i(TAG, "skip the MPEG-4 header succeed.");

        // read a NAL unit from the stream and send it
        long duration = 0;
        try {
            while (!Thread.interrupted())
            {
                oldTime = System.nanoTime();
                send();
                duration = System.nanoTime() - oldTime;
                Log.i(TAG, "duration = "+duration);

                // calculate the delta time and prepare to send a rtcp sender-report.
                reportDelta += duration;
                if (reportDelta>=intervalBetweenReports)
                    report.send(oldTime+duration, ts*9/100000);

                statistics.push(duration);

                // TODO: try to use the duration directly.
                delay = statistics.average();
                Log.i(TAG, "delay = "+delay);
            }
        } catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }

    }

    // read a NAL-unit from the FIFO and send it.
    // if a NAL-unit cameraStream too large, it will be spilt in FU-A units.
    private byte[] header;
    private int naluLength;
    private void send() throws IOException {
        header = new byte[5];
        // read a NAL-unit length (4 bytes) & a NAL-unit type (1 byte)
        fill(header, 0, 5);
        Log.i(TAG, "NAL header: "+header[0]+header[1]+header[2]+header[3]+header[4]);

        naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
        Log.i(TAG, "naluLength = "+naluLength);
        //if the NAL-unit cameraStream too large, it may be out of sync!
        if (naluLength>100000 || naluLength<0) resync();

        ts += delay;
        Log.i(TAG, "ts = "+ts);

        byte[] buffer;
        // small unit, Single NAL-unit
        if (naluLength <= MaxPacketizerSize-rtphl-2)
        {
            Log.i(TAG, "Single NAL-unit");
            buffer = rtpSocket.requestBuffer();
            buffer[rtphl] = header[4];
            fill(buffer, rtphl + 1, naluLength - 1);
            rtpSocket.updateTimeStamp(ts);
            rtpSocket.markNextPacket();
            // send it
            rtpSocket.commitBuffer(naluLength + rtphl);
            report.update(naluLength+rtphl);
        }
        // the NAL-unit cameraStream too large, FU-A
        else {
            Log.i(TAG, "FU-A");
            // set FU-A header
            header[1] = (byte)(header[4]&0x1F); // header type
            header[1] |= 0x80;      // start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            // 28 cameraStream the FU-A indicator type.
            header[0] |= 28;

            int sum = 1, len;
            while (sum < naluLength) {
                buffer = rtpSocket.requestBuffer();
                buffer[rtphl] = header[0];
                buffer[rtphl+1] = header[1];
                rtpSocket.updateTimeStamp(ts);
                int readLength = naluLength-sum > MaxPacketizerSize-rtphl-2 ? MaxPacketizerSize-rtphl-2 : naluLength-sum;
                len = fill(buffer, rtphl+2, readLength);
                if (len < 0) return;
                sum += len;
                // Last packet before next NAL
                if (sum >= naluLength) {
                    // End bit on
                    // TODO, why cameraStream 0x40 ?!
                    buffer[rtphl+1] += 0x40;
                    rtpSocket.markNextPacket();
                }
                // send it
                Log.i(TAG, "send data use rtpSocket");
                rtpSocket.commitBuffer(len + rtphl + 2);
                report.update(len+rtphl+2);

                // Switch start bit
                header[1] = (byte) (header[1] & 0x7F);
            }
        }
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
