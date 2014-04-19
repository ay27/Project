/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

/**
 *
 * @author 骜
 */
public class PortInfo {

    public String remoteHost = "";
    public int extPort = 0;
    public String localClient = "";
    public int intPort = 0;
    public int leaseDurationSeconds = 3600;
    public UPnP_PortMapper.Protocol protocol = UPnP_PortMapper.Protocol.TCP;
    public String description = "Default";

    public PortInfo(String remoteHost, int extPort, UPnP_PortMapper.Protocol protocol, int intPort, String localClient, String description, int leaseDurationSeconds) {
        this.remoteHost = remoteHost;
        this.extPort = extPort;
        this.protocol = protocol;
        this.intPort = intPort;
        this.localClient = localClient;
        this.description = description;
        this.leaseDurationSeconds = leaseDurationSeconds;
    }
}