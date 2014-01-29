package org.bitman.project.record.rtsp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import org.bitman.project.http.HttpClient;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspServer extends Service {

    private static final String TAG = "RtspServer";
    private static final String SERVER_NAME = "org.bitman.ay27 rtsp server";

    //private boolean streaming = false;
    //public boolean isStreaming() { return streaming; }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private class LocalBinder extends Binder {
        public RtspServer getService() {
            return RtspServer.this;
        }
    }


    private RequestListener listenerThread = null;

    @Override
    public void onCreate() {
        start();
    }

    private void start() {
        if (listenerThread == null)
            listenerThread = new RequestListener();
    }

    private class RequestListener extends Thread {
        private ServerSocket serverSocket;

        public RequestListener()
        {
            try {
                serverSocket = new ServerSocket(Session.getInstance().rtsp_port);
                start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            int cc = 0;
            while (!Thread.interrupted())
            {
                Log.i(TAG, "cc = "+cc);
                try {
                    new WorkerThread(serverSocket.accept()).start();
                } catch (IOException e) {
                    Log.e(TAG, "in RequestListener->run(): "+e.toString());
                }
            }
        }

        public void kill()
        {
            try {
                serverSocket.close();
                this.join();
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
        private Session mSession;

        public WorkerThread(final Socket client) throws IOException {
            mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
            mOutput = client.getOutputStream();
            mClient = client;
            mSession = Session.getInstance();
        }

        public void run()
        {
            Request request;
            Response response;

            while (!Thread.interrupted()) {
                request = null;
                response = null;

                // Parse the request
                try {
                    request = Request.parseRequest(mInput);
                } catch (SocketException e) {
                    break;
                } catch (Exception e) {
                    // We don't understand the request :/
                    response = new Response();
                    response.status = Response.Status.BadRequest;
                }

                // Do something accordingly like starting the streams, sending a session description
                if (request != null) {
                    try {
                        response = processRequest(request);
                    }
                    catch (Exception e) {
                        // This alerts the main thread that something has gone wrong in this thread
//                        postError(e, ERROR_START_FAILED);
                        Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
                        e.printStackTrace();
                        response = new Response(request);
                    }
                }

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

                mSession = Session.getInstance();
                //mSession.setOrigin(mClient.getLocalAddress());
                if (mSession.destination==null) {
                    mSession.destination = mClient.getInetAddress();
                }
                //mSession = handleRequest(request.uri, mClient);
                //mSessions.put(mSession, null);

                String requestContent = mSession.getSessionDescription();

                response.attributes = "Content-Base: "+mClient.getLocalAddress().getHostAddress()+":"+mClient.getLocalPort()+"/\r\n" +
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
                    int[] ports = mSession.client_port;
                    p1 = ports[0];
                    p2 = ports[1];
                }
                else {
                    p1 = Integer.parseInt(m.group(1));
                    p2 = Integer.parseInt(m.group(2));
                }

                ssrc = mSession.SSRC;
                src = mSession.server_port;
                destination = mSession.destination;

                mSession.client_port = new int[]{p1, p2};

                response.attributes = "Transport: RTP/AVP/UDP;"+(destination.isMulticastAddress()?"multicast":"unicast")+
                    ";destination="+mSession.destination.getHostAddress()+
                    ";client_port="+p1+"-"+p2+
                    ";server_port="+src[0]+"-"+src[1]+
                    ";ssrc="+Integer.toHexString(ssrc)+
                    ";mode=play\r\n" +
                    "Session: "+ HttpClient.ShareData.getMEID() + "\r\n" +
                    "Cache-Control: no-cache\r\n";
                response.status = Response.Status.OK;
            }

			/* ********************************************************************************** */
			/* ********************************** Method PLAY *********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PLAY")) {
                String requestAttributes = "RTP-Info: ";
                requestAttributes += "url=rtsp://"+mClient.getLocalAddress().getHostAddress()+":"+mClient.getLocalPort()+"/trackID=0;seq=0,";
                requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: "+HttpClient.ShareData.getMEID() +"\r\n";

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

            String response = 	"RTSP/1.0 "+status+"\r\n" +
                    "Server: "+SERVER_NAME+"\r\n" +
                    (seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
                    "Content-Length: " + content.length() + "\r\n" +
                    attributes + "\r\n" + content;

            Log.d(TAG,response.replace("\r", ""));

            try {
                output.write(response.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "error to send the response");
            }
        }
    }
}
