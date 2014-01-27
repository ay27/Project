package org.bitman.project.http;

import android.preference.PreferenceManager;
import android.util.Log;
import org.bitman.project.ProjectApplication;

public class HttpServer {
	
	private static final String TAG = "HttpServer";

    public static class ShareData {
        private static String MEID = null;
        private static String city = null;
        private static String local_rtsp_address = null;

        private ShareData() { }

        public static String getMEID() throws Exception {
            if (MEID == null)
                throw new Exception("Error in HttpServer/ShareData->getMEID(), you must set it before get it");
            return MEID;
        }

        public static void setMEID(String MEID1) { MEID = MEID1; }

        public static String getCity() throws Exception {
            if (city == null)
                throw new Exception("Error in HttpServer/ShareData->getCity(), you must set it before get it");
            return city;
        }

        public static void setCity(String city1) { city = city1; }

        public static String getLocal_rtsp_address() throws Exception {
            if (local_rtsp_address == null)
                throw new Exception("Error in HttpServer/ShareData->getLocal_rtsp_address(), you must set it before get it");
            return local_rtsp_address;
        }

        public static void setLocal_rtsp_address(String local_rtsp_address1) { local_rtsp_address = local_rtsp_address1; }
    }

    public static enum Options {
        ListCity, ListDir, ListFile, ListNow,
        Record, PlayFile, PlayTime, PlayNow,
        Online, Close
    }

    // They will have a default value. Which is used to write to preferences at the very beginning.
    private String server_ip = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance).getString("server_ip", "127.0.0.1");
    private String server_address;

    private static WorkThread workThread;
    private static HttpServer instance = null;
    private HttpServer()
    {
        workThread = WorkThread.getInstance();
        setIP(server_ip);
    }

    public static HttpServer getInstance()
    {
        if (instance == null)
            return (instance = new HttpServer());
        else
            return instance;
    }

    /**
     * Must be set before the first time to use the instance.
     * @param IP the IP of remote server.
     */
    public void setIP(String IP) {
        if (!GetIP.isIpAddress(IP))
        {
            Log.e(TAG, "in setIP(): invalid IP");
            throw new IllegalArgumentException("invalid IP");
        }

        server_address = "http://"+IP+":8080/Server/Servlet";
        server_ip = IP;
    }

    public String getServer_ip() { return server_ip; }
    public String getDestination() { return server_address; }

    public void open()
    {
        String receive = send(Options.Online, null);
        if (WorkThread.getInstance().getStatus() == WorkThread.Status.error)
            Log.e(TAG, receive);
    }

    public String send(Options type, String content)
    {
        StringBuffer sBuffer = new StringBuffer();
        try {
            switch (type) {
                case ListCity:
                    sBuffer.append("LISTCITY#");
                    //sBuffer.append(MEID);
                    //sBuffer.append("#");
                    sBuffer.append(content);		// keyword
                    break;
                case Record:
                    sBuffer.append("RECORD#");
                    //sBuffer.append(MEID);
                    //sBuffer.append("/");
                    sBuffer.append(ShareData.getCity());
                    sBuffer.append("#");
                    sBuffer.append(ShareData.getMEID());
                    sBuffer.append("#");
                    sBuffer.append(ShareData.getLocal_rtsp_address());
                    break;
                case Close:
                    sBuffer.append("CLOSE#");
                    //sBuffer.append(cityID);
                    //sBuffer.append("/");
                    //sBuffer.append(MEID);
                    sBuffer.append(ShareData.getMEID());
                    //sBuffer.append("#");
                    //sBuffer.append(PID);
                    break;
                case ListDir:
                    sBuffer.append("LISTDIR#");
                    sBuffer.append(ShareData.getCity());
                    break;
                case ListNow:
                    sBuffer.append("LISTNOW#");
                    sBuffer.append(ShareData.getCity());
                    break;
                case ListFile:
                    sBuffer.append("LISTFILE#");
                    sBuffer.append(ShareData.getCity());
                    sBuffer.append("#");
                    sBuffer.append(content);	// phoneID
                    break;
                case PlayFile:
                    sBuffer.append("PLAYFILE#");
                    sBuffer.append(ShareData.getCity());
                    sBuffer.append("#");
                    sBuffer.append(content);	// PhoneID#FileName
                    sBuffer.append("#");
                    sBuffer.append(ShareData.getMEID());
                    break;
                case PlayTime:
                    sBuffer.append("PLAYTIME#");
                    sBuffer.append(ShareData.getCity());
                    sBuffer.append("#");
                    sBuffer.append(content);	// camID#time
                    sBuffer.append("#");
                    sBuffer.append(ShareData.getMEID());
                    break;
                case PlayNow:
                    sBuffer.append("PLAYNOW#");
                    sBuffer.append(ShareData.getCity());
                    sBuffer.append("#");
                    sBuffer.append(content);	// CamID or phoneID
                    sBuffer.append("#");
                    sBuffer.append(ShareData.getMEID());
                    break;
            /*case LOGIN:
                sBuffer.append("LOGIN#");
                sBuffer.append(MEID);
                break;*/
                case Online:
                    sBuffer.append("ONLINE#");
                    sBuffer.append(ShareData.getMEID());
                    break;
                default:
                    break;
            }
        } catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }

        Log.i(TAG, "send data: "+sBuffer.toString());
        String receive = workThread.send(sBuffer.toString());
        Log.i(TAG, "receive data: "+receive);

        return receive;
    }

}

