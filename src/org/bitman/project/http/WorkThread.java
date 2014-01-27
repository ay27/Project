package org.bitman.project.http;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;


public class WorkThread extends Thread {
	
	private static final String TAG = "WorkThread";

    public static enum Status{
        error, ok, waiting, init
    }
    private Status status = Status.init;

    public Status getStatus() { return status; }

    private static WorkThread instance = null;
	private WorkThread() {
        this.start();
    }
	
	/**
	 * use the Singleton Pattern
	 * @return the only instance of WorkThread.
	 */
	public static WorkThread getInstance()
	{
		if (instance == null)
			try {
				return (instance = new WorkThread());
			} catch (Exception e)
			{
				Log.e(TAG, "where: getInstance() "+e.toString());
			}
		return instance;
	}

    private static final int MAX_LENGTH = 50;
	private String[] send = new String[MAX_LENGTH];
	private String[] receive = new String[MAX_LENGTH];
    private int send_in = 0, send_out = 0;
    private int receive_in = 0, receive_out = 0;
    //private Semaphore semaphore = new Semaphore(MAX_LENGTH);
	/**
	 * Here is the working entry. The semaphore guarantees that there is only one
     *  run() thread is running.
	 * @param data : the data waiting to send.
	 * @return the data receive from the remote server.
	 */
	public synchronized String send(String data) {
        send[send_in++] = data;

	}

	private HttpURLConnection connection;
	@Override
	public void run()
	{
        while (semaphore.tryAcquire()) {
		try {
			connection = (HttpURLConnection) (new URL(HttpServer.getInstance().getDestination())).openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			
			connection.setRequestMethod("POST");
			connection.connect();
			
			// send
			OutputStream dop = connection.getOutputStream();
			dop.write(send_data.getBytes("UTF-8"));
			dop.flush();
			dop.close();

			// receive
			InputStream is = connection.getInputStream();
			byte[] bytes = new byte[10240];
			is.read(bytes);
			receive_data = new String(bytes, "UTF-8");
			is.close();
			
			connection.disconnect();
			connection = null;

			status = Status.ok;
		} catch (Exception e) {
			Log.e(TAG, "where: run() " + e.toString());
			receive_data = "ERROR: "+e.toString();
            status = Status.error;
		}
		finally {
			if (connection != null)
			{
				connection.disconnect();
				connection = null;
			}
            try { semaphore.acquire(); } catch (InterruptedException e) { }
        }
	
	}
    }
	
	
	
	

}
