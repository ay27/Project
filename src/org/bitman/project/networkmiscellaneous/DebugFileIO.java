/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 骜
 */
class DebugFileIO {
    private static RandomAccessFile raFile;
    private static FileChannel channelFile;
    private static ByteBuffer buffer;
    private static String path = "D:\\debuglog.txt";
    private static int sizeBuffer = 500000;

    static {
        try {
            buffer = ByteBuffer.allocate(sizeBuffer);
            buffer.clear();
            raFile = new RandomAccessFile(path, "rw");
            channelFile = raFile.getChannel();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    /*public DebugFileIO(String path) throws FileNotFoundException, IOException {
     this.path = path;
     buffer = ByteBuffer.allocate(sizeBuffer);
     buffer.clear();
     raFile = new RandomAccessFile(path, "rw");
     channelFile = raFile.getChannel();
     }*/
    public synchronized static void println(String dat) {
        synchronized (buffer) {
            try {
                buffer.clear();
                dat = dat + "\r\n";
                buffer.put(dat.getBytes());
                buffer.flip();
                while (buffer.hasRemaining()) {
                    channelFile.write(buffer);
                }
                channelFile.force(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        return;
    }

    public synchronized static void close() {
        buffer = null;
        path = null;
        try {
            channelFile.close();
            raFile.close();
        } catch (IOException ex) {
            Logger.getLogger(DebugFileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
