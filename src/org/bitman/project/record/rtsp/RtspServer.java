package org.bitman.project.record.rtsp;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import org.bitman.project.networkmiscellaneous.UPnP_PortMapper;
import org.bitman.project.record.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 *
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 *
 */
public class RtspServer extends Service {

    public final static String TAG = "RtspServer";

    /** The server name that will appear in responses. */
    public static String SERVER_NAME = "Bitman_ay27 RTSP Server";

    /** Port used by default. */
    public static final int DEFAULT_RTSP_PORT = 8554;

    /** Port already in use. */
    public final static int ERROR_BIND_FAILED = 0x00;

    /** A stream could not be started. */
    public final static int ERROR_START_FAILED = 0x01;

    /** Streaming started. */
    public final static int MESSAGE_STREAMING_STARTED = 0X00;

    /** Streaming stopped. */
    public final static int MESSAGE_STREAMING_STOPPED = 0X01;

    private static final String SessionID = "Phone"+Integer.toHexString(new Random().nextInt());

    protected static int mPort = DEFAULT_RTSP_PORT;
    protected WeakHashMap<Session,Object> mSessions = new WeakHashMap<Session,Object>(2);

    private RequestListener mListenerThread;
    private final IBinder mBinder = new LocalBinder();
    private boolean mRestart = false;
    private final LinkedList<CallbackListener> mListeners = new LinkedList<CallbackListener>();


    /** Be careful: those callbacks won't necessarily be called from the ui thread ! */
    public interface CallbackListener {

        /** Called when an error occurs. */
        void onError(RtspServer server, Exception e, int error);

        /** Called when streaming starts/stops. */
        void onMessage(RtspServer server, int message);

    }

    /**
     * See {@link RtspServer.CallbackListener} to check out what events will be fired once you set up a listener.
     * @param listener The listener
     */
    public void addCallbackListener(CallbackListener listener) {
        synchronized (mListeners) {
            if (mListeners.size() > 0) {
                for (CallbackListener cl : mListeners) {
                    if (cl == listener) return;
                }
            }
            mListeners.add(listener);
        }
    }

    /**
     * Removes the listener.
     * @param listener The listener
     */
    public void removeCallbackListener(CallbackListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public static void setRtspPort(int port) {
        mPort = port;
    }

    /**
     * Starts (or restart if needed, if for example the configuration
     * of the server has been modified) the RTSP server.
     */
    public void start() {
        if (mRestart) stop();
        if (mListenerThread == null) {
            try {
                mListenerThread = new RequestListener();
            } catch (Exception e) {
                mListenerThread = null;
            }
        }
        mRestart = false;
    }

    /**
     * Stops the RTSP server but not the Android Service.
     * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)};
     */
    public void stop() {
        if (mListenerThread != null) {
            try {
                mListenerThread.kill();
                for ( Session session : mSessions.keySet() ) {
                    if ( session != null ) {
                        if (session.isStreaming()) session.stop();
                    }
                }
            } catch (Exception e) {
            } finally {
                mListenerThread = null;
            }
        }
    }

    /** Returns whether or not the RTSP server is streaming to some client(s). */
    public boolean isStreaming() {
        for ( Session session : mSessions.keySet() ) {
            if ( session != null ) {
                if (session.isStreaming()) return true;
            }
        }
        return false;
    }

    /** Returns the bandwidth consumed by the RTSP server in bits per second. */
//    public long getBitrate() {
//        long bitrate = 0;
//        for ( Session session : mSessions.keySet() ) {
//            if ( session != null ) {
//                if (session.isStreaming()) bitrate += session.getBitrate();
//            }
//        }
//        return bitrate;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        start();
    }

    @Override
    public void onDestroy() {
        stop();
    }

    /** The Binder you obtain when a connection with the Service is established. */
    public class LocalBinder extends Binder {
        public RtspServer getService() {
            return RtspServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void postMessage(int id) {
        synchronized (mListeners) {
            if (mListeners.size() > 0) {
                for (CallbackListener cl : mListeners) {
                    cl.onMessage(this, id);
                }
            }
        }
    }

    protected void postError(Exception exception, int id) {
        synchronized (mListeners) {
            if (mListeners.size() > 0) {
                for (CallbackListener cl : mListeners) {
                    cl.onError(this, exception, id);
                }
            }
        }
    }

//    /**
//     * By default the RTSP uses {@link UriParser} to parse the URI requested by the client
//     * but you can change that behavior by override this method.
//     * @param uri The uri that the client has requested
//     * @param client The socket associated to the client
//     * @return A proper session
//     */
	/*protected Session handleRequest(String uri, Socket client) throws IllegalStateException, IOException {
		Session session = UriParser.parse(uri);
		session.setOrigin(client.getLocalAddress());
		if (session.getDestination()==null) {
			session.setDestination(client.getInetAddress());
		}
		return session;
	}
	*/
    class RequestListener extends Thread implements Runnable {

        private final ServerSocket mServer;

        public RequestListener() throws IOException {
            try {
                mServer = new ServerSocket(mPort);
                start();
            } catch (BindException e) {
                Log.e(TAG,"Port already in use !");
                postError(e, ERROR_BIND_FAILED);
                throw e;
            }
        }

        public void run() {
            // TODO:....
            UPnP_PortMapper portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
            portMapper.AddPortMapping(8554, 8554, 3600, UPnP_PortMapper.Protocol.TCP);

            Log.i(TAG,"RTSP server listening on port "+mServer.getLocalPort());
            int cc = 0;
            while (!Thread.interrupted()) {
                try {
                    Log.i("cc", Integer.toString(++cc));
                    new WorkerThread(mServer.accept()).start();
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                    continue;
                }
            }
            Log.i(TAG,"RTSP server stopped !");

        }

        public void kill() {
            try {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UPnP_PortMapper portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
                        portMapper.DeleteExistedPortMapping();
                    }
                }).start();

                mServer.close();
            } catch (IOException e) {}
            try {
                this.join();
            } catch (InterruptedException ignore) {}
        }

    }

    // One thread per client
    class WorkerThread extends Thread implements Runnable {

        private final Socket mClient;
        private final OutputStream mOutput;
        private final BufferedReader mInput;

        // Each client has an associated session
        private Session mSession;

        public WorkerThread(final Socket client) throws IOException {
            mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
            mOutput = client.getOutputStream();
            mClient = client;
            mSession = new Session();
        }

        public void run() {
            Request request;
            Response response;

            Log.i(TAG, "Connection from "+mClient.getInetAddress().getHostAddress());

            while (!Thread.interrupted()) {

                request = null;
                response = null;

                // Parse the request
                try {
                    request = Request.parseRequest(mInput);
                } catch (SocketException e) {
                    // Client has left
                    break;
                } catch (Exception e) {
                    // We don't understand the request :/
                    response = new Response();
                    response.status = Response.STATUS_BAD_REQUEST;
                }

                // Do something accordingly like starting the streams, sending a session description
                if (request != null) {
                    try {
                        response = processRequest(request);
                    }
                    catch (Exception e) {
                        // This alerts the main thread that something has gone wrong in this thread
                        postError(e, ERROR_START_FAILED);
                        Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
                        e.printStackTrace();
                        response = new Response(request);
                    }
                }

                // We always send a response
                // The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
                try {
                    response.send(mOutput);
                } catch (IOException e) {
                    Log.e(TAG,"Response was not sent properly");
                    break;
                }

            }

            // Streaming stops when client disconnects
            boolean streaming = isStreaming();
            if (streaming && !isStreaming()) {
                postMessage(MESSAGE_STREAMING_STOPPED);
            }
//            mSession.flush();

            try {
                mClient.close();
            } catch (IOException ignore) {}

            Log.i(TAG, "Client disconnected");

        }

        public Response processRequest(Request request) throws IllegalStateException, IOException {
            Response response = new Response(request);

			/* ********************************************************************************** */
			/* ********************************* Method DESCRIBE ******************************** */
			/* ********************************************************************************** */
            if (request.method.equalsIgnoreCase("DESCRIBE")) {

//                mSession = SessionBuilder.getInstance().build();
//                mSession.setOrigin(mClient.getLocalAddress());
                if (mSession.getDestination()==null) {
                    Log.i(TAG, "client address: "+mClient.getInetAddress());
                    mSession.setDestination(mClient.getInetAddress());
                }
                //mSession = handleRequest(request.uri, mClient);
                mSessions.put(mSession, null);

                String requestContent = mSession.getSessionDescription();
                String requestAttributes =
                        "Content-Base: "+mClient.getLocalAddress().getHostAddress()+":"+mClient.getLocalPort()+"/\r\n" +
                                "Content-Type: application/sdp\r\n";

                response.attributes = requestAttributes;
                response.content = requestContent;

                // If no exception has been thrown, we reply with OK
                response.status = Response.STATUS_OK;

            }

			/* ********************************************************************************** */
			/* ********************************* Method OPTIONS ********************************* */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("OPTIONS")) {
                response.status = Response.STATUS_OK;
                response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                response.status = Response.STATUS_OK;
            }

			/* ********************************************************************************** */
			/* ********************************** Method SETUP ********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("SETUP")) {
                Pattern p; Matcher m;
                int p2, p1, ssrc, trackId, src[];
                InetAddress destination;

                p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
                m = p.matcher(request.uri);

                if (!m.find()) {
                    response.status = Response.STATUS_BAD_REQUEST;
                    return response;
                }

                trackId = Integer.parseInt(m.group(1));

//                if (!mSession.trackExists()) {
//                    response.status = Response.STATUS_NOT_FOUND;
//                    return response;
//                }

                p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
                m = p.matcher(request.headers.get("transport"));

                if (!m.find()) {
//                    int[] ports = mSession.getTrack().getDestinationPorts();
                    int[] ports = mSession.getClient_port();
                    p1 = ports[0];
                    p2 = ports[1];
                }
                else {
                    p1 = Integer.parseInt(m.group(1));
                    p2 = Integer.parseInt(m.group(2));
                }

//                ssrc = mSession.getTrack().getSSRC();
//                src = mSession.getTrack().getLocalPorts();
                ssrc = mSession.SSRC;
                src = mSession.getServer_port();
                destination = mSession.getDestination();

                // TODO : test it
                map_ports(src[0], p1);  // rtp port
                map_ports(src[1], p2);  // rtcp port

                Log.i(TAG, "ports: "+p1+" "+p2);
//                mSession.getTrack().setDestinationPorts(p1, p2);
                mSession.setClient_port(new int[]{p1, p2});
                boolean streaming = isStreaming();
                // ����������rtp������������������
                mSession.start();
                if (!streaming && isStreaming()) {
                    postMessage(MESSAGE_STREAMING_STARTED);
                }

                response.attributes = "Transport: RTP/AVP/UDP;"+(destination.isMulticastAddress()?"multicast":"unicast")+
                        ";destination="+mSession.getDestination().getHostAddress()+
                        ";client_port="+p1+"-"+p2+
                        ";server_port="+src[0]+"-"+src[1]+
                        ";ssrc="+Integer.toHexString(ssrc)+
                        ";mode=play\r\n" +
                        // ����������������MEID����������������
                        "Session: "+ SessionID + "\r\n" +
                        "Cache-Control: no-cache\r\n";
                response.status = Response.STATUS_OK;

                // If no exception has been thrown, we reply with OK
                response.status = Response.STATUS_OK;

            }

			/* ********************************************************************************** */
			/* ********************************** Method PLAY *********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PLAY")) {
                String requestAttributes = "RTP-Info: ";
                requestAttributes += "url=rtsp://"+mClient.getLocalAddress().getHostAddress()+":"+mClient.getLocalPort()+"/trackID="+mSession.trackID+";seq=0,";
                requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: "+SessionID+"\r\n";

                response.attributes = requestAttributes;

                // If no exception has been thrown, we reply with OK
                response.status = Response.STATUS_OK;

            }

			/* ********************************************************************************** */
			/* ********************************** Method PAUSE ********************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PAUSE")) {
                response.status = Response.STATUS_OK;
            }

			/* ********************************************************************************** */
			/* ********************************* Method TEARDOWN ******************************** */
			/* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                response.status = Response.STATUS_OK;
                mSession.stop();
            }

			/* ********************************************************************************** */
			/* ********************************* Unknown method ? ******************************* */
			/* ********************************************************************************** */
            else {
                Log.e(TAG,"Command unknown: "+request);
                response.status = Response.STATUS_BAD_REQUEST;
            }

            return response;

        }

    }

    // TODO : test it
    private void map_ports(int internalPort, int externalPort) {
        Log.i(TAG, "map port: "+internalPort+" --"+externalPort);
        UPnP_PortMapper portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
        boolean ok = false;
        for (int i = 0; !ok; i++) {
            ok = portMapper.AddPortMapping(internalPort, externalPort+i, 3600, UPnP_PortMapper.Protocol.UDP);
            if (ok)
                Log.i(TAG, "port map succeed in : "+internalPort+" -- "+externalPort+i);
        }
    }

    static class Request {

        // Parse method & uri
        public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
        // Parse a request header
        public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);

        public String method;
        public String uri;
        public HashMap<String,String> headers = new HashMap<String,String>();

        /** Parse the method, uri & headers of a RTSP request */
        public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
            Request request = new Request();
            String line;
            Matcher matcher;

            // Parsing request method & uri
            if ((line = input.readLine())==null) throw new SocketException("Client disconnected");
            matcher = regexMethod.matcher(line);
            matcher.find();
            request.method = matcher.group(1);
            request.uri = matcher.group(2);

            // Parsing headers of the request
            while ( (line = input.readLine()) != null && line.length()>3 ) {
                matcher = rexegHeader.matcher(line);
                matcher.find();
                request.headers.put(matcher.group(1).toLowerCase(Locale.US),matcher.group(2));
            }
            if (line==null) throw new SocketException("Client disconnected");

            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            Log.e(TAG,request.method+" "+request.uri);

            return request;
        }
    }

    static class Response {

        // Status code definitions
        public static final String STATUS_OK = "200 OK";
        public static final String STATUS_BAD_REQUEST = "400 Bad Request";
        public static final String STATUS_NOT_FOUND = "404 Not Found";
        public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

        public String status = STATUS_INTERNAL_SERVER_ERROR;
        public String content = "";
        public String attributes = "";

        private final Request mRequest;

        public Response(Request request) {
            this.mRequest = request;
        }

        public Response() {
            // Be carefull if you modify the send() method because request might be null !
            mRequest = null;
        }

        public void send(OutputStream output) throws IOException {
            int seqid = -1;

            try {
                seqid = Integer.parseInt(mRequest.headers.get("cseq").replace(" ",""));
            } catch (Exception e) {
                Log.e(TAG,"Error parsing CSeq: "+(e.getMessage()!=null?e.getMessage():""));
            }

            String response = 	"RTSP/1.0 "+status+"\r\n" +
                    "Server: "+SERVER_NAME+"\r\n" +
                    (seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
                    "Content-Length: " + content.length() + "\r\n" +
                    attributes +
                    "\r\n" +
                    content;

            Log.d(TAG,response.replace("\r", ""));

            output.write(response.getBytes());

        }
    }

}
