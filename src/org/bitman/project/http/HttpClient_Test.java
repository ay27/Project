package org.bitman.project.http;

import junit.framework.TestCase;

/**
 * Created by ay27 on 14-1-27.
 */
public class HttpClient_Test extends TestCase {

    public void testGetInstance()
    {
        HttpClient httpClient = HttpClient.getInstance();
        HttpClient httpClient1 = HttpClient.getInstance();
        assertEquals(httpClient, httpClient1);
    }

    public void testSetIP()
    {
        HttpClient httpClient = HttpClient.getInstance();
        try {
            httpClient.setIP("192.168.1.1");
        } catch (Exception e) {
            assertTrue(false);
        }
        try {
            httpClient.setIP("192.2132321.213");
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            httpClient.setIP("5c:f9:38:aa:de:08");
            System.out.println(httpClient.getDestination());
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}