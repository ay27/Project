package org.bitman.project.record.rtsp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.bitman.project.record.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspServer extends Service {

    private static final String TAG = "RtspServer";
    private static final String SERVER_NAME = "org.bitman.ay27 rtsp server";
    private static final String SessionId = "phone"+Integer.toHexString(new Random().nextInt());

    private static int rtsp_port = 8554;
    // Will be set in the next time, or you must set it before the service start.
    public static void setRtsp_port(int rtsp_port) { RtspServer.rtsp_port = rtsp_port; }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public RtspServer getService() {
            return RtspServer.this;
        }
    }

    private RequestListener listenerThread = null;

    @Override
    public void onCreate() {
        start();
    }

    public void start() {
        Log.i(TAG, "start");
        if (listenerThread == null)
            listenerThread = new RequestListener();
    }
    public void stop() {
        Log.i(TAG, "rtsp stop");
        if (listenerThread != null)
        {
            listenerThread.kill();
            listenerThread = null;
        }
    }

    private class RequestListener extends Thread {
        private static final String TAG = "RequestListener";
        private ServerSocket serverSocket;

        public RequestListener()
        {
            try {
                serverSocket = new ServerSocket(rtsp_port);
                Log.i(TAG, "serverSocket start on "+rtsp_port);
                start();
            } catch (IOException e) {
                Log.i(TAG, e.toString());
            }
        }

        public void run()
        {
            int cc = 0;
            Thread work = null;
            while (true)
            {
                // We make sure that only has one client connect to us.
                if (work != null)
                    work.interrupt();
                Log.i(TAG, "cc = "+(++cc));
                try {
                    Socket temp = serverSocket.accept();
                    work = new WorkerThread(temp);
                    work.start();
                } catch (IOException e) {
                    Log.e(TAG, "in RequestListener->run(): "+e.toString());
                    break;
                }
            }
        }

        public void kill()
        {
            Log.i(TAG, "kill the RequestListener");
            try {
                this.interrupt();
                serverSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "in RequestListener->kill(): "+e.toString());
            }
        }
    }

    private class WorkerThread extends Thread {
        private final Socket mClient;
        private final OutputStream mOutput;
        private final BufferedReader mInput;

        // Each client has an associated session
        private Session mSession = Session.getInstance();

        public WorkerThread(final Socket client) throws IOException {
            mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
            mOutput = client.getOutputStream();
            mClient = client;
        }

        public void run()
        {
            Log.i(TAG, "worker thread run.");

            Request request;
            Response response;

            while (!Thread.interrupted()) {
                request = null;
                response = null;

                // Parse the request
                try {
                    request = Request.parseRequest(mInput);
                } catch (SocketException e) {
                    Log.e(TAG, e.toString());
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    // We don't understand the request :/
                    request = null;
                    response = new Response();
                    response.status = Response.Status.BadRequest;
                }

                // Do something accordingly like starting the streams, sending a session description
                if (request != null) {
                    Log.i(TAG, "begin to process request.");
                    try {
                        response = processRequest(request);
                    }
                    catch (Exception e) {
                        Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
                        response = new Response(request);
                    }
                }

                Log.i(TAG, "begin to send the response.");
                // We always send a response
                // The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
                response.send(mOutput);

            }

            mSession.stop();

            try {
                mClient.close();
            } catch (IOException ignore) {}
            Log.i(TAG, "Client disconnected");
        }

        private Response processRequest(Request request) throws Exception {
            Response response = new Response(request);

			/* ********************************************************************************** */
			/* ********************************* Method DESCRIBE ******************************** */
			/* ********************************************************************************** */
            if (request.method.equalsIgnoreCase("DESCRIBE")) {

                if (mSession.getDestination()==null) {
                    mSession.setDestination(mClient.getInetAddress());
                }

                String requestContent = mSession.getSessionDescription();

                response.attributes = "Content-Base: "+mSession.localAddress.getHostAddress()+":"+mClient.getLocalPort()+"/\r\n" +
                        "Content-Type: application/sdp\r\n";
                response.content = requestContent;

                // If no exception has been thrown, we reply with OK
                response.status = Response.Status.OK;

            }

			/* ********************************************************************************** */
			/* ********************************* Method OPTIONS ********************************* */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("OPTIONS")) {
                response.status = Response.Status.OK;
                response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                response.status = Response.Status.OK;
            }

			/* ********************************************************************************** */
			/* ********************************** Method SETUP ********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("SETUP")) {
                Pattern p; Matcher m;
                int p2, p1, ssrc, src[];
                InetAddress destination;

                p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
                m = p.matcher(request.uri);

                if (!m.find()) {
                    response.status = Response.Status.BadRequest;
                    return response;
                }

//                if (!mSession.trackExists()) {
//                    response.status = Response.STATUS_NOT_FOUND;
//                    return response;
//                }

                p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
                m = p.matcher(request.headers.get("transport"));

                if (!m.find()) {
                    int[] ports = mSession.getClient_port();
                    p1 = ports[0];
                    p2 = ports[1];
                }
                else {
                    p1 = Integer.parseInt(m.group(1));
                    p2 = Integer.parseInt(m.group(2));
                }

                ssrc = mSession.getSSRC();
                src = mSession.getServer_port();
                destination = mSession.getDestination();

                mSession.setClient_port(new int[]{p1, p2});

                response.attributes = "Transport: RTP/AVP/UDP;"+(destination.isMulticastAddress()?"multicast":"unicast")+
                    ";destination="+mSession.getDestination().getHostAddress()+
                    ";client_port="+p1+"-"+p2+
                    ";server_port="+src[0]+"-"+src[1]+
                    ";ssrc="+Integer.toHexString(ssrc)+
                    ";mode=play\r\n" +
                    "Session: "+ SessionId + "\r\n" +
                    "Cache-Control: no-cache\r\n";
                response.status = Response.Status.OK;
            }

			/* ********************************************************************************** */
			/* ********************************** Method PLAY *********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PLAY")) {
                String requestAttributes = "RTP-Info: ";
                // TODO: here, maybe has a problem.
                // Why the second line use a subString() function.
                requestAttributes += "url=rtsp://"+mClient.getLocalAddress().getHostAddress()+":"+mClient.getLocalPort()+"/trackID="+mSession.trackID+";seq=0,";
                requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: "+ SessionId +"\r\n";

                response.attributes = requestAttributes;

                // Here start the stream.
                mSession.start();

                // If no exception has been thrown, we reply with OK
                response.status = Response.Status.OK;

            }

			/* ********************************************************************************** */
			/* ********************************** Method PAUSE ********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PAUSE")) {
                response.status = Response.Status.OK;
            }

			/* ********************************************************************************** */
			/* ********************************* Method TEARDOWN ******************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                response.status = Response.Status.OK;
            }

			/* ********************************************************************************** */
			/* ********************************* Unknown method ? ******************************* */
			/* ********************************************************************************** */
            else {
                Log.e(TAG,"Command unknown: "+request);
                response.status = Response.Status.BadRequest;
            }

            return response;

        }
    }

    private static class Request
    {
        private static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE);
        private static final Pattern regexHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE);

        private String method;
        private String uri;
        private HashMap<String, String> headers = new HashMap<String, String>();

        public static Request parseRequest(BufferedReader input) throws IOException {
            Request request = new Request();
            String line;
            Matcher matcher;

            if ((line = input.readLine()) == null) throw new SocketException("Client disconnected");

            matcher = regexMethod.matcher(line);
            matcher.find();
            request.method = matcher.group(1);
            request.uri = matcher.group(2);

            while ((line = input.readLine()) != null && line.length()>3)
            {
                matcher = regexHeader.matcher(line);
                matcher.find();
                request.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2));
            }
            if (line == null) throw new SocketException("Client disconnected");

            Log.i(TAG, "request: "+request.method+" "+request.uri);

            return request;
        }
    }

    private static class Response {
        public Response() {
            request = null;
        }

        public static class Status {
            public static final String OK = "200 OK";
            public static final String BadRequest = "400 Bad Request";
            public static final String NotFound = "404 Not Found";
            public static final String InternelServerError = "500 Internel Server Error";
        }

        public String status = Status.InternelServerError;
        public String content = null;
        public String attributes = null;
        private final Request request;
        public Response(Request request) { this.request = request; }

        public void send(OutputStream output)
        {
            int seqid = -1;
            try {
                seqid = Integer.parseInt(request.headers.get("cseq").replace(" ", ""));
            } catch (Exception e) { Log.e(TAG, "error parse cseq id"); }

            content = (content == null)? new String():content;

            String response = 	"RTSP/1.0 "+status+"\r\n" +
                    "Server: "+SERVER_NAME+"\r\n" +
                    (seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
                    "Content-Length: " + content.length() + "\r\n" +
                    attributes + "\r\n" + content;

            Log.i(TAG,"response: "+response.replace("\r", ""));

            try {
                output.write(response.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "error to send the response");
            }
        }
    }
}
