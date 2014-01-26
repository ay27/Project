package org.bitman.project.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;

import android.util.Log;


public class WorkThread implements Runnable {
	
	private static final String TAG = "WorkThread";
	
	
	private static WorkThread instance = null;
	private WorkThread() {	}
	
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
	
	private String send_data;
	private String receive_data;
	private Semaphore semaphore = new Semaphore(1);
	/**
	 * Here is the working entry.
	 * @param data : the data waiting to send.
	 * @return the data receive from the remote server.
	 */
	public synchronized String send(String data) {
		send_data = data;
		try { semaphore.acquire(); } catch (InterruptedException e) { }
		this.run();
		try { semaphore.acquire(); } catch (InterruptedException e) { }
		semaphore.release();
		return receive_data;
	}

	private HttpURLConnection connection;
	@Override
	public void run()
	{
		try {
			connection = (HttpURLConnection) (new URL(HttpServer.server_address)).openConnection();
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
			
			semaphore.release();
			
			connection.disconnect();
			connection = null;
			
		} catch (Exception e) {
			Log.e(TAG, "where: run() " + e.toString());
			receive_data = "ERROR: "+e.toString();
		}
		finally {
			if (connection != null)
			{
				connection.disconnect();
				connection = null;
			}
		}
	
	}
	
	
	
	

}
