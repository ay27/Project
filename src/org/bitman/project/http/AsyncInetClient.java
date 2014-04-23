package org.bitman.project.http;

import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import org.apache.http.Header;
import org.bitman.project.ProjectApplication;

import java.lang.reflect.Field;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-8.
 */
public class AsyncInetClient {

    private static final String TAG = "AsyncInetClient";
    public static final String StartOfRtsp = "rtsp://";
    private AsyncHttpClient client;
    private String url;
    private static AsyncInetClient instance = null;

    public static final String RecordOK = "Record_OK";
    public static final String CloseOK = "Close_OK";
    public static final String OnlineOK = "Online_OK";
    public static final String LoginOK = "Login_OK";
    public static final String AddUserOK = "Add_OK";
    public static final String UpdateOK = "Password_OK";

    /**
     * The Message-Type. The user's message must be based on in.
     */
    public static enum Type {
        SearchCity, ListPast, ListNow, ListFile, PlayFile, PlayNow, Record, Close, Online, Add, Password, Login
    }

    public static class SendData {
//        private static final String TAG = "SendData";

        private String IMEI;
        private String CityID;
        private String Keyword;
        private String FileName;
        private String RtspUrl;
        private String DeviceID;
        private String UserName;
        private String Passwd;
        private String OldPasswd;
        private String NewPasswd;

        public SendData() {
            IMEI = ProjectApplication.IMEI;
            UserName = ProjectApplication.getUserName();
            Passwd = ProjectApplication.getPassword();
        }

        public SendData setDeviceID(String deviceID) {
            this.DeviceID = deviceID;
            return this;
        }

        public SendData setCityID(int cityID) {
            this.CityID = Integer.toString(cityID);
            return this;
        }

        public SendData setKeyword(String keyword) {
            Keyword = keyword;
            return this;
        }

        public SendData setFileName(String fileName) {
            FileName = fileName;
            return this;
        }

        public SendData setRtspUrl(String rtspUrl) {
            RtspUrl = rtspUrl;
            return this;
        }

        public SendData setUserName(String userName) {
            UserName = userName;
            return this;
        }

        public SendData setPasswd(String passwd) {
            Passwd = passwd;
            return this;
        }

        public SendData setOldPasswd(String oldPasswd) {
            OldPasswd = oldPasswd;
            return this;
        }

        public SendData setNewPasswd(String newPasswd) {
            NewPasswd = newPasswd;
            return this;
        }

        public RequestParams getParams() {

            // Use the java reflection.
            // Well, it works OK! And the cost of it is acceptable.
            RequestParams params = new RequestParams();
            Field[] fields = this.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String varName = fields[i].getName();
                try {
                    String var = (String)fields[i].get(this);
//                    int accessible = fields[i].getModifiers();

                    if (var!=null && !var.equals(""))
                        params.add(varName, var);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                }
            }

            return params;
        }
    }


    private AsyncInetClient() {
        client = new AsyncHttpClient();
    }

    /**
     * We must be sure that the Application only has one HttpClient.
     * @return the instance of AsyncInetClient
     */
    public static AsyncInetClient getInstance() {
        if (instance == null)
            return (instance = new AsyncInetClient());
        else
            return instance;
    }

    public void setHttpPort(int port) {
        client = null;
        client = new AsyncHttpClient(port);
    }

    public void setTimeOut(final int timeOut) {
        client.setTimeout(timeOut);
    }

    public void setServer(final String url) {
        this.url = url;
    }

    /**
     * Get the AsyncHttpClient instance.
     * If the user want to use some functions on it, you can use this to get the instance.
     * But in usually, we do not recommend you to use it.
     * @return the instance of the AsyncHttpClient
     */
    public AsyncHttpClient getClient() {
        return client;
    }

    public void searchCity(final String keyword, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.SearchCity, new SendData().setKeyword(keyword), httpResponseHandler);
    }

    public void listPast(final int cityId, final ResponseHandlerInterface responseHandler) {
        sendMessage(Type.ListPast, new SendData().setCityID(cityId), responseHandler);
    }

    public void listNow(final int cityId, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.ListNow, new SendData().setCityID(cityId), httpResponseHandler);
    }

    public void listFile(final String deviceId, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.ListFile, new SendData().setDeviceID(deviceId), httpResponseHandler);
    }

    public void playFile(final String deviceId, final String filePath, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.PlayFile, new SendData().setDeviceID(deviceId).setFileName(filePath), httpResponseHandler);
    }

    public void playNow(final String deviceId, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.PlayNow, new SendData().setDeviceID(deviceId), httpResponseHandler);
    }

    public void record(final int cityId, final String RtspUrl, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Record, new SendData().setCityID(cityId).setRtspUrl(RtspUrl), httpResponseHandler);
    }

    public void close(final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Close, new SendData(), httpResponseHandler);
    }

    public void online(final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Online, new SendData(), httpResponseHandler);
    }

    /**
     * Remember that set the ProjectApplication's UserName & Password if the receive is OK.
     * @param userName
     * @param passwd
     * @param httpResponseHandler
     */
    public void addUser(final String userName, final String passwd, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Add, new SendData().setUserName(userName).setPasswd(passwd), httpResponseHandler);
    }

    /**
     * Remember that set the ProjectApplication's UserName & Password if the receive is OK.
     * @param userName
     * @param oldPasswd
     * @param newPasswd
     * @param httpResponseHandler
     */
    public void rePasswd(final String userName, final String oldPasswd, final String newPasswd, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Password, new SendData().setUserName(userName).setOldPasswd(oldPasswd).setNewPasswd(newPasswd), httpResponseHandler);
    }

    /**
     * Remember that set the ProjectApplication's UserName & Password if the receive is OK.
     * @param userName
     * @param passwd
     * @param httpResponseHandler
     */
    public void login(final String userName, final String passwd, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Login, new SendData().setUserName(userName).setPasswd(passwd), httpResponseHandler);
    }

    public void sendMessage(final Type type,
                            final SendData data,
                            final ResponseHandlerInterface responseHandler) {

        if (url == null || url.equals(""))
            throw new IllegalStateException("The Server Url must be set before use the GET function.");

        if (type == null || type.equals(""))
            throw new IllegalArgumentException("You must set the Message Type when you send a message to server.");

        if (data == null)
            throw new IllegalArgumentException("You must set the Data argument when you use the sendMessage function.");

        RequestParams params = data.getParams();
        params.add("Action", type.name());

        if (responseHandler == null)
            client.get(url, params, defaultHandler);
        else
            client.get(url, params, responseHandler);

    }

    /**
     * Just for debug.
     */
    private AsyncHttpResponseHandler defaultHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            Log.i(TAG+" OnSuccess", "receive some data from server: "+new String(responseBody));
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG + " OnFailure", "receive some data from server: " + new String(responseBody));
        }
    };

}
