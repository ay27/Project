package org.bitman.project.record.rtp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Random;

public class Packetizer implements Runnable {
    private static final String TAG = "Packetizer";

    private final RtpSocket mSocket;

    private long ts;
    private InputStream is;
    /** Used in packetizers to estimate timestamps in RTP packets. */
    private Statistics statistics = new Statistics();

    // work for RtcpSenderReport
    private static final int intervalBetweenReports = 5000;
    private int reportDelta = 0;
    private final RtcpSenderReport report;

    private static final int MaxPacketizerSize = 1400;
    private static final int rtphl = RtpSocket.RTP_HEADER_LENGTH;

    public Packetizer() {
        mSocket = RtpSocket.getInstance();
        report = RtcpSenderReport.getInstance();
        ts = new Random().nextInt();
    }

    public void setStream(InputStream is) { this.is = is; }
    public void setDestination(InetAddress destination) { mSocket.setDestination(destination); }
    public InetAddress getDestination() { return mSocket.getDestination(); }
    public void setSSRC(int SSRC) { mSocket.setSSRC(SSRC); }
    public int[] getPorts() { return new int[] { mSocket.getLocalPort(), report.getLocalPort() }; }

    private Thread mThread = null;
    public void start()
    {
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
            } catch (InterruptedException e) {}
            mThread = null;
        }
        mSocket.stop();
    }

    private long oldTime = 0;
    private long delay = 0;

    @Override
    public void run() {
        statistics.reset();

        // skip the MPEG-4 header
        try {
            byte[] bytes = new byte[4];
            while (!Thread.interrupted())
            {
                while (is.read() != 'm');
                is.read(bytes);
                if (bytes[0]=='m' && bytes[1]=='d' && bytes[2]=='a') break;
            }
        } catch (Exception e)
        {
            Log.e(TAG, "could not skip the MPEG-4 header");
            return;
        }

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
    // if a NAL-unit is too large, it will be spilt in FU-A units.
    byte[] header;
    private void send() throws IOException {
        header = new byte[5];
        // read a NAL-unit length (4 bytes) & a NAL-unit type (1 byte)
        fill(header, 0, 5);
        Log.i(TAG, "NAL header: "+header[0]+header[1]+header[2]+header[3]+header[4]);

        int naluLength = (header[3]&0xFF) | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
        Log.i(TAG, "naluLength = "+naluLength);
        //if the NAL-unit is too large, it may be out of sync!
        if (naluLength>(2<<16) || naluLength<0) naluLength = resync();

        ts += delay;
        Log.i(TAG, "ts = "+ts);

        byte[] buffer;
        // small unit, Single NAL-unit
        if (naluLength <= MaxPacketizerSize-rtphl-2)
        {
            Log.i(TAG, "Single NAL-unit");
            buffer = mSocket.requestBuffer();
            buffer[rtphl] = header[4];
            fill(buffer, rtphl + 1, naluLength - 1);
            mSocket.updateTimeStamp(ts);
            mSocket.markNextPacket();
            // send it
            mSocket.commitBuffer(naluLength+rtphl);
            report.update(naluLength+rtphl);
        }
        // the NAL-unit is too large, FU-A
        else {
            Log.i(TAG, "FU-A");
            // set FU-A header
            header[1] = (byte)(header[4]&0x1F); // header type
            header[1] |= 0x80;      // start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            // 28 is the FU-A indicator type.
            header[0] |= 28;

            int sum = 1, len;
            while (sum < naluLength) {
                buffer = mSocket.requestBuffer();
                buffer[rtphl] = header[0];
                buffer[rtphl+1] = header[1];
                mSocket.updateTimeStamp(ts);
                int readLength = naluLength-sum > MaxPacketizerSize-rtphl-2 ? MaxPacketizerSize-rtphl-2 : naluLength-sum;
                len = fill(buffer, rtphl+2, readLength);
                if (len < 0) return;
                sum += len;
                // Last packet before next NAL
                if (sum >= naluLength) {
                    // End bit on
                    // TODO, why is 0x40 ?!
                    buffer[rtphl+1] += 0x40;
                    mSocket.markNextPacket();
                }
                // send it
                mSocket.commitBuffer(len+rtphl+2);
                report.update(len+rtphl+2);

                // Switch start bit
                header[1] = (byte) (header[1] & 0x7F);
            }
        }
    }

    private int fill(byte[] buffer, int offset, int length) throws IOException {
        int sum = 0, len;
        while (sum<length)
        {
            len = is.read(buffer, offset+sum, length-sum);
            if (len < 0)
                throw new IOException("end of stream");
            sum += len;
        }

        return sum;
    }

    private int resync() throws IOException {
        byte[] header = new byte[5];
        int type;

        Log.i(TAG, "fuck, trying to resync");

        while (true)
        {
            header[0] = header[1];
            header[1] = header[2];
            header[2] = header[3];
            header[3] = header[4];
            header[4] = (byte)is.read();

            type = header[4] & 0x1F;
            if (type == 1 || type == 5)
            {
                int naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
                if (naluLength>0 && naluLength<(2<<16))
                {
                    oldTime = System.nanoTime();
                    Log.i(TAG, "OH! It may be a NAL-unit in the stream!");
                    return naluLength;
                }
            }
        }
    }

}
