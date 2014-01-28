package org.bitman.project.http;

import junit.framework.TestCase;

/**
 * Created by ay27 on 14-1-26.
 */
public class WorkerThread_Test extends TestCase {

    public void testGetInstance()
    {
        WorkerThread workerThread = WorkerThread.getInstance();
        WorkerThread workerThread1 = WorkerThread.getInstance();
        assertEquals(workerThread, workerThread1);
    }

    public void testSend()
    {
        // TODO I don't know how to test it.
    }

}