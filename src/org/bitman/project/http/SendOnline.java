package org.bitman.project.http;

public class SendOnline implements Runnable {

    private static final int SleepTime = 60;

    private static SendOnline instance;
    private SendOnline() { }
    public static SendOnline getInstance()
    {
        if (instance == null)
            return (instance = new SendOnline());
        else return instance;
    }

    private Thread mThread;
    public void open() {
        if (mThread == null)
        {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    public void close() {
        if (mThread != null)
            mThread.interrupt();
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            try {
                Thread.sleep(SleepTime*1000);
            } catch (InterruptedException e) { }
            HttpServer.getInstance().send(HttpServer.Options.Online, null);
        }
    }
}
