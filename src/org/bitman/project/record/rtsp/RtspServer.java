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
import java.net.ServerSocket;
import java.net.Socket;

public class RtspServer extends Service {

    private static final String TAG = "RtspServer";

    private boolean streaming = false;
    public boolean isStreaming() { return streaming; }

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
                serverSocket = new ServerSocket(Session.getInstance().getRtsp_port());
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


    }
}
