/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author 骜
 */
public class Module_TCP_Link {

    private SocketChannel channelTCP;
    private Selector selector;
    private SelectionKey selKeyThisChannel;
    private ByteBuffer bufferThisChannel;
    private CharsetDecoder decoderThisChannel;
    private String ipDst;
    private int portDst;
    private InetSocketAddress addressDst;
    private boolean isConnected;
    private static final int REPLY_TIME_WAIT = 1000;
    private static final int RECV_TIMEOUT = 3000;

    public Module_TCP_Link(String TargetAddress, int TargetPort) throws IOException,IllegalStateException {
        ipDst = TargetAddress;
        portDst = TargetPort;
        isConnected = false;
        decoderThisChannel = Charset.defaultCharset().newDecoder();
        addressDst = new InetSocketAddress(ipDst, portDst);
        bufferThisChannel = ByteBuffer.allocate(50000); //this capacity can buffer about 30 packet
    }

    /**
     * This method is blocking,and it will return a String which contian the
     * reply of request.
     *
     * when this method exit,the connection to endpoint will not drop,and you
     * must establish or drop the connection by you self.
     *
     * @param byte[] dat: Request.
     * @return String Reply
     */
    public synchronized String Link(byte[] dat) throws IOException, IllegalStateException {
        if (!isConnected) {
            throw new IOException("Error:Link has be dropped.");
        }
        Send(dat);
        try {
            Thread.sleep(REPLY_TIME_WAIT);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return Recieve();
    }

    public synchronized String Recieve() throws IOException, IllegalStateException {
        if (!isConnected) {
            return "";
        }
        String rtn = new String("");
        Set<SelectionKey> selectionKeysSet = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectionKeysSet.iterator();
        try {
            if (0 == selector.select(RECV_TIMEOUT)) {
                throw new IllegalStateException("Http request time out.");
            }
            selectionKeysSet = selector.selectedKeys();
            keyIterator = selectionKeysSet.iterator();
            if (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                while (key.isReadable()) {
                    bufferThisChannel.clear();
                    switch (((SocketChannel) key.channel()).read(bufferThisChannel)) {
                        case -1:
                            keyIterator.remove();
                            DropConnection();
                            return rtn;
                        case 0:
                            keyIterator.remove();
                            return rtn;
                        default:
                            bufferThisChannel.flip();
                            rtn = rtn + decoderThisChannel.decode(bufferThisChannel).toString();  //uncertain,debug
                            break;
                    }
                }
                keyIterator.remove();
            }
            return "";

        } catch (IOException ex) {
            try {
                keyIterator.remove();
            } catch (IllegalStateException ex_in) {
            }
            throw ex;
        } catch (IllegalStateException ex) {
            try {
                keyIterator.remove();
            } catch (IllegalStateException ex_in) {
            }
            throw ex;
        }
    }

    public synchronized void Send(byte[] dat) throws IOException {
        bufferThisChannel.clear();
        bufferThisChannel.put(dat);
        bufferThisChannel.flip();
        try {
            while (bufferThisChannel.hasRemaining()) {
                channelTCP.write(bufferThisChannel);
            }
        } catch (IOException ex) {
            throw ex;
        }
    }

    public synchronized boolean EstablishConnection() throws IOException {
        if (isConnected) {
            return true;
        }
        try {
            channelTCP = SocketChannel.open();
            channelTCP.socket().bind(new InetSocketAddress(0));
            channelTCP.configureBlocking(false);
            selector = Selector.open();
            channelTCP.connect(addressDst);
            while (!channelTCP.finishConnect());
            selKeyThisChannel = channelTCP.register(selector, SelectionKey.OP_READ, bufferThisChannel);
            isConnected = true;
            return true;
        } catch (IOException ex) {
            throw ex;
        }
    }

    public synchronized boolean DropConnection() {
        if (!isConnected) {
            return true;
        }
        try {
            selector.close();
            channelTCP.close();
            selector = null;
            channelTCP = null;
            selKeyThisChannel = null;
            isConnected = false;
            return true;
        } catch (IOException ex) {
            ex.printStackTrace(); //exception_deal  this position will hold the exception 
            //because this function may be called when exception has happend
            return false;
        }
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public String getIpDst() {
        return ipDst;
    }

    public int getPortDst() {
        return portDst;
    }
}
