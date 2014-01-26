package org.bitman.project.http;

import junit.framework.TestCase;

public class GetIP_Test extends TestCase {
	
	public void testGetLocalIPAddress()
	{
		System.out.print(GetIP.getLocalIpAddress(true));
		System.out.print(GetIP.getLocalIpAddress(true));
	}
}
