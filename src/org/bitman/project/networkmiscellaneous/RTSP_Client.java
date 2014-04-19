/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Log;

/**
 *
 * @author ���
 */
public class RTSP_Client {

    /**
     * Tag symbol_RTSP_URL,symbol_CSeq need be replaced.
     */
    private static final String headOPTIONS = "OPTIONS __RU__ RTSP/1.0\r\n"
            + "CSeq: __CSEQ__\r\n"
            + "User-Agent: Bitman RTSP Client\r\n\r\n";
    /**
     * Tag symbol_RTSP_URL,symbol_CSeq need be replaced.
     */
    private static final String headDESCRIBE = "DESCRIBE __RU__ RTSP/1.0\r\n"
            + "CSeq: __CSEQ__\r\n"
            + "Accept: application/sdp\r\n"
            + "User-Agent: Bitman RTSP Client\r\n\r\n";
    /**
     * Tag symbol_TrackCtrURL,symbol_CSeq,symbol_RTP_RTCP_Port,symbol_SESSION_ID
     * need be replaced.
     */
    private static final String headSETUP = "SETUP __RTCU__ RTSP/1.0\r\n"
            + "CSeq: __CSEQ__\r\n"
            + "Transport: RTP/AVP;unicast;client_port=__RCP__\r\n"
            + "__SSID__"
            + "User-Agent: Bitman RTSP Client\r\n\r\n";
    /**
     * Tag symbol_RTSP_URL,symbol_CSeq,symbol_SESSION_ID need be replaced.
     */
    private static final String headPLAY = "PLAY __RU__ RTSP/1.0\r\n"
            + "CSeq: __CSEQ__\r\n"
            + "__SSID__"
            + "Range: npt=0.000-\r\n"
            + "User-Agent: Bitman RTSP Client\r\n\r\n";
    /**
     * Tag symbol_RTSP_URL,symbol_CSeq,symbol_SESSION_ID need be replaced.
     */
    private static final String headTEARDOWN = "TEARDOWN __RU__ RTSP/1.0\r\n"
            + "CSeq: __CSEQ__\r\n"
            + "__SSID__"
            + "User-Agent: Bitman RTSP Client\r\n\r\n";
    /**
     * Example: "rtsp://10.50.2.1:8554/test.sdp".
     */
    private static final String symbol_RTSP_URL = "__RU__";
    /**
     * Replace this tag with var CSeq.
     */
    private static final String symbol_CSeq = "__CSEQ__";
    /**
     * Replace this with track's control URL(get from sdp).<br/>
     * Example:"rtsp://10.50.2.1:8554/test.sdp/trackID=42".
     */
    private static final String symbol_TrackCtrURL = "__RTCU__";
    /**
     * Replace this with RTP and RTCP port.<br/>
     * Example:"39576-39577".
     */
    private static final String symbol_RTP_RTCP_Port = "__RCP__";
    /**
     * If the Session ID has be assigned,Replace this tag with "Session:
     * SID",otherwise replace this with empty string "".
     */
    private static final String symbol_SESSION_ID = "__SSID__";
    private String ipSrv;
    private int portSrv;
    private String url;
    private String sdpSegment;
    private String sessionID;
    private int timeout;
    private int CSeq;
    private Status statusOK;
    private RandomAccessFile sdpFile;
    private Module_TCP_Link TCPChannel;
    private UPnP_PortMapper portMapper;
    private boolean isNeedPortMap;
    private Vector<TrackInfo> trackList;
    private Timer timer;
    private TimerTask task;

    public RTSP_Client(final String url, final RandomAccessFile file) throws IOException, IllegalStateException, InterruptedException {
        ipSrv = Utilities.getURL_IpAddress(url);
        portSrv = new Integer(Utilities.getURL_PortAddress(url));
        this.url = url;
        this.sdpFile = file;
        CSeq = 0;
        sessionID = "";
        timeout = 0;
        TCPChannel = new Module_TCP_Link(ipSrv, portSrv);
        trackList = new Vector<TrackInfo>();
        statusOK = Status.INIT;
        try {
            portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
            isNeedPortMap = true;
        } catch (IllegalStateException ex) {
            isNeedPortMap = false;
        }
        
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                String request = headOPTIONS;
                request = request.replaceAll(symbol_RTSP_URL, url);
                request = request.replaceAll(symbol_CSeq, ++CSeq + "");
                System.out.println("A keep alive packet sent:");
                System.out.println(request);  //debug
                String rtn;
                try {
                    rtn = TCPChannel.Link(request.getBytes());
                    System.out.println(rtn);  //debug
                } catch (IOException ex) {
                } catch (IllegalStateException ex) {
                }
            }
        };
    }

    public void Play() throws IOException, IllegalStateException {
        TCPChannel.EstablishConnection();
        while (true) {
            switch (statusOK) {
                case INIT:
                    doOPTIONS();
                    statusOK = Status.OPTIONS;
                    break;
                case OPTIONS:
                    doDESCRIBE();
                    statusOK = Status.SETUP.DESCRIBE;
                    break;
                case DESCRIBE:
                    doSETUP();
                    statusOK = Status.SETUP.SETUP;
                    break;
                case SETUP:
                    doPLAY();
                    statusOK = Status.PLAY;
                    break;
                case PLAY:
                    //Start keepalive timer before return;
                    enableKeepAlive();
                    doGenSDPFile();
                    return;

                case PAUSE:
                    throw new IllegalStateException("Illegal State Detected.");
                case TEARDOWN:
                    throw new IllegalStateException("Illegal State Detected.");
                default:
                    throw new AssertionError(statusOK.name());
            }
        }


    }

    public void Teardown() throws IOException {
        disableKeepAlive();
        doTEARDOWN();
        TCPChannel.DropConnection();
    }

    private void doGenSDPFile() {
        int lastChangeOffset = 0;
        Iterator<TrackInfo> trackIterator = trackList.iterator();
        while (trackIterator.hasNext()) {
            TrackInfo currentTrack = trackIterator.next();
            currentTrack.sdpOffset += lastChangeOffset;
            sdpSegment = sdpSegment.substring(0, currentTrack.sdpOffset + 8) + currentTrack.localPort[0] + "/2" + sdpSegment.substring(currentTrack.sdpOffset + 9);
            lastChangeOffset = (currentTrack.localPort[0] + "").length() + 1;
        } 


        //sdpSegment is sdpfile
        try {
			//sdpFile.write(sdpSegment.getBytes());
			//sdpFile.close();
        	Log.i("sdp", sdpSegment);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        
    }

    private void doOPTIONS() throws IOException, IllegalStateException {
        String request = headOPTIONS;
        request = request.replaceAll(symbol_RTSP_URL, url);
        request = request.replaceAll(symbol_CSeq, ++CSeq + "");
        System.out.println(request);  //debug
        String rtn = TCPChannel.Link(request.getBytes());
        System.out.println(rtn);  //debug
        if (!rtn.startsWith("RTSP/1.0 200 OK")) {
            throw new IllegalStateException("ERROR:RTSP server report that an error counter(OPTIONS stage).\nDetail:" + rtn);
        }
        if ((SearchSvc.KMP(rtn, "DESCRIBE", 0) == 0)
                || (SearchSvc.KMP(rtn, "SETUP", 0) == 0)
                || (SearchSvc.KMP(rtn, "PLAY", 0) == 0)
                || (SearchSvc.KMP(rtn, "TEARDOWN", 0) == 0)
                || (SearchSvc.KMP(rtn, "DESCRIBE", 0) == 0)) {

            throw new IllegalStateException("Critial method not be supported.");
        }
    }

    private void doDESCRIBE() throws IllegalStateException, IOException {
        String request = headDESCRIBE;
        request = request.replaceAll(symbol_RTSP_URL, url);
        request = request.replaceAll(symbol_CSeq, ++CSeq + "");
        System.out.println(request);  //debug
        String rtn = TCPChannel.Link(request.getBytes());
        System.out.println(rtn);  //debug
        if (!rtn.startsWith("RTSP/1.0 200 OK")) {
            throw new IllegalStateException("ERROR:RTSP server report that an error counter(DESCRIBE stage).\nDetail:" + rtn);
        }

        sdpSegment = rtn.split("\r\n\r\n")[1];

        int[] searchResult = NextTag(sdpSegment, 0);
        if (searchResult[1] == -1) {
            throw new IllegalStateException("Error:No audio/video media track found in stream's describe.");
        } else {
            while (!(searchResult[0] == -1)) {
                TrackInfo track = new TrackInfo();
                this.trackList.add(track);

                track.sdpOffset = searchResult[1];
                int ctUrlStartOffset = SearchSvc.KMP_IgnoreCaption(sdpSegment, "a=control:", searchResult[1]) + 10;
                int ctUrlEndOffset = SearchSvc.KMP_IgnoreCaption(sdpSegment, "\r\n", ctUrlStartOffset);
                track.controlURL = sdpSegment.substring(ctUrlStartOffset, ctUrlEndOffset);
                if (!(track.controlURL.startsWith("rtsp") || track.controlURL.startsWith("rtsp"))) {
                    if (url.endsWith("/")) {
                        track.controlURL = url + track.controlURL;
                    } else {
                        track.controlURL = url + "/" + track.controlURL;
                    }
                }
                switch (searchResult[0]) {
                    case 0:
                        track.paylodType = MediaType.audio;
                        break;
                    case 1:
                        track.paylodType = MediaType.video;
                        break;
                }
                searchResult = NextTag(sdpSegment, searchResult[1]);
            }
        }
    }

    /**
     * Return a 2-item int array: <br/>
     * Item 0 -- Type: <br/>
     * -1 -&#62 m=audio or m=video NotFound, <br/>
     * 0 -&#62 m=audio, <br/>
     * 1 -&#62 m=video. <br/>
     * <br/>
     * Item 1 -- Offset
     *
     * @param startOffset - Start Offset
     * @return [0] - type,[1] - offset
     */
    private static int[] NextTag(String model, int startOffset) {
        int offset = startOffset;
        offset = SearchSvc.KMP_IgnoreCaption(model, "m=", ++offset);
        while (offset != -1) {
            String subs = model.substring(offset + 2, offset + 7);
            if (subs.equalsIgnoreCase("audio")) {
                return new int[]{0, offset};
            } else if (subs.equalsIgnoreCase("video")) {
                return new int[]{1, offset};
            }
            offset = SearchSvc.KMP_IgnoreCaption(model, "m=", ++offset);
        }
        return new int[]{-1, 0};
    }

    private void doSETUP() throws IOException {
        String request;
        String rtn;
        Iterator<TrackInfo> trackIterator = trackList.iterator();
        while (trackIterator.hasNext()) {
            TrackInfo currentTrack = trackIterator.next();
            int[] streamRecvPort;
            boolean isMapSuccess;

            do {
                do {
                    streamRecvPort = Utilities.getFreePortUDP(2);//generate free port for RTP/RTCP
                } while (streamRecvPort[0] + 1 != streamRecvPort[1]);
                isMapSuccess = true;
                if (isNeedPortMap) {
                    isMapSuccess = isMapSuccess && portMapper.AddPortMapping(streamRecvPort[0], streamRecvPort[0], 3600, UPnP_PortMapper.Protocol.UDP);
                    isMapSuccess = isMapSuccess && portMapper.AddPortMapping(streamRecvPort[1], streamRecvPort[1], 3600, UPnP_PortMapper.Protocol.UDP);
                }
                if (!isMapSuccess) {
                    portMapper.DeletePortMapping(streamRecvPort[1], UPnP_PortMapper.Protocol.UDP);
                    portMapper.DeletePortMapping(streamRecvPort[1], UPnP_PortMapper.Protocol.UDP);
                }
            } while (!isMapSuccess);
            currentTrack.localPort = streamRecvPort;

            request = headSETUP;
            request = request.replaceAll(symbol_TrackCtrURL, currentTrack.controlURL);
            request = request.replaceAll(symbol_CSeq, ++CSeq + "");
            request = request.replaceAll(symbol_RTP_RTCP_Port, streamRecvPort[0] + "-" + streamRecvPort[1]);
            request = request.replaceAll(symbol_SESSION_ID, sessionID);

            System.out.println(request); //debug
            rtn = TCPChannel.Link(request.getBytes());
            System.out.println(rtn); //debug
            if (!rtn.startsWith("RTSP/1.0 200 OK")) {
                throw new IllegalStateException("ERROR:RTSP server report that an error counter(SETUP stage).\nDetail:" + rtn);
            }

            //get server's port
            try {
                Pattern servport_Pattern = Pattern.compile("server_port=(\\d+)-(\\d+)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher servport_Matcher = servport_Pattern.matcher(rtn);
                servport_Matcher.find();
                currentTrack.remotePort = new int[]{
                    new Integer(servport_Matcher.group(1)),
                    new Integer(servport_Matcher.group(2))
                };
            } catch (IllegalStateException ex) {
                throw new IllegalStateException("ERROR:No server port found at SETUP stage.");
            }
            //if has sessionID,get it
            try {
                Pattern sessionID_Pattern = Pattern.compile("(Session: \\p{XDigit}+)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher sessionID_Matcher = sessionID_Pattern.matcher(rtn);
                sessionID_Matcher.find();
                sessionID = sessionID_Matcher.group(1) + "\r\n";
            } catch (IllegalStateException ex) {
                sessionID = "";
            }
            //if has SSRC,get it
            try {
                Pattern SSRC_Pattern = Pattern.compile("ssrc=(\\p{XDigit}+)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher SSRC_Matcher = SSRC_Pattern.matcher(rtn);
                SSRC_Matcher.find();
                currentTrack.SSRC = SSRC_Matcher.group(1);
            } catch (IllegalStateException ex) {
                currentTrack.SSRC = "";
            }
            //if has timeout,get it
            try {
                Pattern timeoout_Pattern = Pattern.compile("timeout=(\\d+)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher timeout_Matcher = timeoout_Pattern.matcher(rtn);
                timeout_Matcher.find();
                timeout = new Integer(timeout_Matcher.group(1));
                timeout = timeout - 10;         //pervent the deadline has come but packet haven't arrive to server
            } catch (IllegalStateException ex) {
                timeout = 0;
            }
            //send two UDP pac from local-RTP to remote-RTP and close UDP link immediately
            DatagramSocket UDP_tempSocket = new DatagramSocket(
                    currentTrack.localPort[0],
                    InetAddress.getByName(GetIP.getLocalIpAddress(true)));
            DatagramPacket pac_tempSocket_Tx = new DatagramPacket(
                    new byte[]{(byte) 0xce, (byte) 0xfa, (byte) 0xed, (byte) 0xfe},
                    4,
                    InetAddress.getByName(ipSrv),
                    currentTrack.remotePort[0]);
            UDP_tempSocket.send(pac_tempSocket_Tx);
            UDP_tempSocket.send(pac_tempSocket_Tx);
            UDP_tempSocket.close();
            //ok
        }
    }

    private void doPLAY() throws IOException, IllegalStateException {
        String request = headPLAY;
        request = request.replaceAll(symbol_RTSP_URL, url);
        request = request.replaceAll(symbol_CSeq, ++CSeq + "");
        request = request.replaceAll(symbol_SESSION_ID, sessionID);
        System.out.println(request);//debug
        String rtn = TCPChannel.Link(request.getBytes());
        if (!rtn.startsWith("RTSP/1.0 200 OK")) {
            throw new IllegalStateException("ERROR:RTSP server report that an error counter(PLAY stage).\nDetail:" + rtn);
        }
        System.out.println(rtn);//debug
    }

    private void doTEARDOWN() throws IOException {
        String request = headTEARDOWN;
        request = request.replaceAll(symbol_RTSP_URL, url);
        request = request.replaceAll(symbol_CSeq, ++CSeq + "");
        request = request.replaceAll(symbol_SESSION_ID, sessionID);
        System.out.println(request);
        String rtn = TCPChannel.Link(request.getBytes());
        System.out.println(rtn);
        System.out.println("Stream Teardown.");
    }

    private void enableKeepAlive() {
        if (timeout != 0) {
            timer.scheduleAtFixedRate(task, 0, timeout * 1000);
        }
    }

    private void disableKeepAlive() {
        task.cancel();
        timer.purge();
        timer.cancel();
    }

    class TrackInfo {

        /**
         * value at sdp file"a=conrtol:VALUE",is a absolutely URL
         * like:"rtsp://ip:port/trackID=1".
         */
        public String controlURL;
        /**
         * local port use to recieve RTP stream,localPort[0] - RTP;localPort[1]
         * - RTCP.
         */
        public int[] localPort;
        /**
         * server port use to send RTP stream,remotePort[0] - RTP;remotePort[1]
         * - RTCP.
         */
        public int[] remotePort;
        /**
         * "m=MediaType" offset in sdp segment.
         */
        public int sdpOffset;
        /**
         * Paylod type,support audio and video.
         */
        public MediaType paylodType;
        /**
         * SSRC of this stream.
         */
        public String SSRC;
    }

    enum MediaType {

        audio, video
    }

    enum Status {

        INIT, OPTIONS, DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN
    }
}
