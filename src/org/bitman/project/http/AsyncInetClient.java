package org.bitman.project.http;

import android.media.AsyncPlayer;
import android.util.Log;
import com.loopj.android.http.*;
import org.apache.http.Header;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-8.
 */
public class AsyncInetClient {

    private static final String TAG = "AsyncInetClient";
    private AsyncHttpClient client;
    private String url;
    private static AsyncInetClient instance = null;

    /**
     * The Message-Type. The user's message must be based on in.
     */
    public static enum Type {
        SearchCity, ListPast, ListNow, ListFile, PlayFile, PlayNow, Record, Close, Online
    }

    public static class SendData {
        private static final String TAG = "SendData";

        private String IMEI;
        private String CityID;
        private String Keyword;
        private String FilePath;
        private String RtspUrl;

        public SendData setIMEI(String IMEI) {
            this.IMEI = IMEI;
            return this;
        }

        public SendData setCityID(String cityID) {
            CityID = cityID;
            return this;
        }

        public SendData setKeyword(String keyword) {
            Keyword = keyword;
            return this;
        }

        public SendData setFilePath(String filePath) {
            FilePath = filePath;
            return this;
        }

        public SendData setRtspUrl(String rtspUrl) {
            RtspUrl = rtspUrl;
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
                    int accessible = fields[i].getModifiers();

                    if (Modifier.isPublic(accessible) && var!=null && !var.equals(""))
                        params.add(varName, var);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                }
            }

//            if (IMEI != null && !IMEI.equals(""))
//                params.add("IMEI", IMEI);
//            if (CityID!=null && !CityID.equals(""))
//                params.add("CityID", CityID);
//            if (Keyword!=null && !Keyword.equals(""))
//                params.add("Keyword", Keyword);
//            if (FilePath!=null && !FilePath.equals(""))
//                params.add("FilePath", FilePath);
//            if (RtspUrl!=null && !RtspUrl.equals(""))
//                params.add("RtspUrl", RtspUrl);

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

    public void searchCity(final String IMEI, final String keyword, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.SearchCity, new SendData().setIMEI(IMEI).setKeyword(keyword), httpResponseHandler);
    }

    public void listPast(final String IMEI, final String cityId, final ResponseHandlerInterface responseHandler) {
        sendMessage(Type.ListPast, new SendData().setIMEI(IMEI).setCityID(cityId), responseHandler);
    }

    public void listNow(final String IMEI, final String cityId, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.ListNow, new SendData().setIMEI(IMEI).setCityID(cityId), httpResponseHandler);
    }

    public void record(final String IMEI, final String cityId, final String RtspUrl, final AsyncHttpResponseHandler httpResponseHandler) {
        sendMessage(Type.Record, new SendData().setIMEI(IMEI).setCityID(cityId).setRtspUrl(RtspUrl), httpResponseHandler);
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
