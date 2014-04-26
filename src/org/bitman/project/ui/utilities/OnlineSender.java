package org.bitman.project.ui.utilities;

import org.bitman.project.http.AsyncInetClient;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-26.
 */
public class OnlineSender extends Thread {

    private AsyncInetClient.Type type;
    private volatile boolean onlineSend = false;
    private AsyncInetClient httpClient = AsyncInetClient.getInstance();

    public OnlineSender(AsyncInetClient.Type type) {
        this.type = type;
    }

    @Override
    public synchronized void start() {
        onlineSend = true;
        super.start();
    }

   public void stopSending() {
       onlineSend = false;
   }

    @Override
    public void run() {
        while (onlineSend) {
            httpClient.online(type, null);
            try {
                Thread.sleep(30*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
