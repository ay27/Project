package org.bitman.project.http;

public class SendOnline extends Thread {

    private static final int SleepTime = 60;

    @Override
    public void run()
    {
        while (true)
        {
            HttpServer.getInstance().send(HttpServer.Options.Online, null);
            try {
                Thread.sleep(SleepTime*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
