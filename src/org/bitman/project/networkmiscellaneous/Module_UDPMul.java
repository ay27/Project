/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.io.IOException;
//import java.net.Inet4Address;
//import java.net.InetAddress;
import java.net.InetSocketAddress;
//import java.net.NetworkInterface;
//import java.net.StandardProtocolFamily;
//import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author ���
 */
public class Module_UDPMul {

    private DatagramChannel channelUDP;
    private Selector selector;
    private ByteBuffer bufferThisChannel;
    private CharsetDecoder decoderThisChannel;
    private String ipDst;
    private int portDst;
    private InetSocketAddress addressDst;
    private InetSocketAddress addressLocal;
    private static final int RECV_TIMEOUT = 3000;

    public Module_UDPMul() throws IOException {
        ipDst = new String("239.255.255.250");
        portDst = 1900;
        addressDst = new InetSocketAddress(ipDst, portDst);
        addressLocal = new InetSocketAddress(0);
        selector = Selector.open();
        channelUDP.socket().bind(addressLocal);      //bind to local:randomPORT
        channelUDP.configureBlocking(false);
        channelUDP.register(selector, SelectionKey.OP_READ);
        decoderThisChannel = Charset.defaultCharset().newDecoder();
        bufferThisChannel = ByteBuffer.allocate(50000); //this capacity can buffer about 30 packet
        bufferThisChannel.clear();
    }

    /**
     * Use this method to send data over this channel.
     *
     * @param String dat: The data want to send
     * @return boolean isSendSuccessfully
     */
    public boolean Send(String dat) throws IOException{
        bufferThisChannel.clear();
        bufferThisChannel.put(dat.getBytes());
        bufferThisChannel.flip();
        try {
            while (bufferThisChannel.hasRemaining()) {
                channelUDP.send(bufferThisChannel, addressDst);
            }
            return true;
        } catch (IOException ex) {
            throw ex;
        }
    }

    /**
     * This method is un-blocking and it will return a Vector contian revcieve data
     * those arrive interval less then RECV_TIMEOUT ms.
     *
     * @return Vector RecieveData
     */
    public Vector RecieveBundle() throws IOException{
        Vector<String> rtnPtr = new Vector<String>();
        Set<SelectionKey> selectionKeysSet = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectionKeysSet.iterator();

        try {
            while (0 != selector.select(RECV_TIMEOUT)) {
                selectionKeysSet = selector.selectedKeys();
                keyIterator = selectionKeysSet.iterator();
                if (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        bufferThisChannel.clear();
                        ((DatagramChannel) key.channel()).receive(bufferThisChannel);
                        bufferThisChannel.flip();
                        rtnPtr.add(Charset.forName("UTF-8").newDecoder().decode(bufferThisChannel).toString());
                    }
                    keyIterator.remove();
                }
            }
            return rtnPtr;
        } catch (IOException ex) {
            keyIterator.remove();
            throw ex;
        }
    }

    /**
     * Use this method to release all resource this class use.
     */
    public void ReleaseRes() throws IOException{
        try {
            this.selector.close();
            this.channelUDP.close();
        } catch (IOException ex) {
            throw ex;
        }
        this.channelUDP = null;
        this.bufferThisChannel = null;
        this.decoderThisChannel = null;
        this.selector = null;
        this.ipDst = null;
    }
}
