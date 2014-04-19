/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.io.IOException;

/**
 *
 * @author 骜
 */
public class OrgBitmanNetworkmiscellaneous {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, IllegalStateException, InterruptedException {
        System.out.println("ok");
        UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
        System.out.println("ok");
        UPnP_PortMapper portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
        System.out.println("ok");
        System.out.println("Extern IP:" + portMapper.GetExternalIPAddress());
        /*        for (int port = 30000;port < 30100;++port){
         portMapper.AddPortMapping(port, port, 3600, UPnPControlPoint.Protocol.TCP);
         portMapper.AddPortMapping(port, port, 3600, UPnPControlPoint.Protocol.UDP);
         }*/
        portMapper.AddPortMapping(30000, 30000, 3600, UPnP_PortMapper.Protocol.TCP);
        Thread.sleep(10000);
        UPnP_PortMapper.UPnP_PM_Supplier.ReleaseAllPort();
        portMapper.AddPortMapping(30001, 30001, 3600, UPnP_PortMapper.Protocol.UDP);
        portMapper.DeleteExistedPortMapping();
//        portMapper.AddPortMapping(8086, 8086, 3600, UPnPControlPoint.Protocol.TCP);
//        portMapper.AddPortMapping(11117, 11117, 3600, Protocol.TCP);
//        portMapper.AddPortMapping(11117, 11117, 3600, Protocol.UDP);
//
//        portMapper.AddPortMapping(8087, 8087, 3600, Protocol.TCP);
//        portMapper.AddPortMapping(8088, 8088, 3600, Protocol.TCP);
//        portMapper.DeletePortMapping(8087, Protocol.TCP);
//        Thread.sleep(2000);
//        portMapper.DeleteExistedPortMapping();
//        RTSP_Client client = new RTSP_Client("rtsp://10.50.2.33:554/video0.sdp", "D:\\temp.sdp");
//        client.Play();
//        Thread.sleep(2000);
//        Scanner sc = new Scanner(System.in);
//        while(!sc.nextLine().endsWith("ok"));
//        client.Teardown();
        UPnP_PortMapper.UPnP_PM_Supplier.ReleaseAllPort();
    }
}
