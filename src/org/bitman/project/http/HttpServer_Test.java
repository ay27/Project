package org.bitman.project.http;

import junit.framework.TestCase;

/**
 * Created by ay27 on 14-1-27.
 */
public class HttpServer_Test extends TestCase {

    public void testGetInstance()
    {
        HttpServer httpServer = HttpServer.getInstance();
        HttpServer httpServer1 = HttpServer.getInstance();
        assertEquals(httpServer, httpServer1);
    }

    public void testSetIP()
    {
        HttpServer httpServer = HttpServer.getInstance();
        try {
            httpServer.setIP("192.168.1.1");
        } catch (Exception e) {
            assertTrue(false);
        }
        try {
            httpServer.setIP("192.2132321.213");
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            httpServer.setIP("5c:f9:38:aa:de:08");
            System.out.println(httpServer.getDestination());
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}