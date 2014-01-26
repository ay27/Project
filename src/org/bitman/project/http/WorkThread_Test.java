package org.bitman.project.http;

import junit.framework.TestCase;

/**
 * Created by ay27 on 14-1-26.
 */
public class WorkThread_Test extends TestCase {

    public void testGetInstance()
    {
        WorkThread workThread = WorkThread.getInstance();
        WorkThread workThread1 = WorkThread.getInstance();
        assertEquals(workThread, workThread1);
    }

    public void testSend()
    {
        // TODO I don't know how to test it.
    }

}