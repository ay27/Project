package org.bitman.project.record.camera;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.SurfaceHolder;
import org.bitman.project.record.VideoQuality;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class CameraWorker {

    private static final String TAG = "CameraWorker";

    private static class Status {
        public static boolean streaming = false;
        public static boolean surfaceReady = false;
        public static boolean socketReady = false;
        public static boolean cameraLock = true;
        public static boolean cameraWorking = false;
    }

    private SurfaceHolder surfaceHolder = null;
    private VideoQuality videoQuality = VideoQuality.getInstance();
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private static CameraWorker instance;
    private CameraWorker() { }
    public static CameraWorker getInstance() {
        if (instance == null)
            return (instance = new CameraWorker());
        else
            return instance;
    }

    protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
    public synchronized void setPreviewDisplay(SurfaceHolder holder) {
        if (mSurfaceHolderCallback != null && holder != null) {
            holder.removeCallback(mSurfaceHolderCallback);
        }
        if (holder != null) {
            mSurfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    Status.surfaceReady = false;
                }
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Status.surfaceReady = true;
                    Log.i(TAG, "surface ready");
                }
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }
            };
            this.surfaceHolder = holder;
            surfaceHolder.addCallback(mSurfaceHolderCallback);
            Status.surfaceReady = true;
        }
    }

    public synchronized void start()
    {
        if (Status.streaming)
        {
            Log.i(TAG, "you can not start the camera again");
            throw new IllegalStateException("Illegal state in CameraWorker->start()");
        }

        try {
            createSockets();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters params = camera.getParameters();
        camera.setParameters(params);
        camera.setDisplayOrientation(videoQuality.orientation);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        unlockCamera();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setMaxDuration(0);
        mediaRecorder.setVideoEncoder(videoQuality.encoder);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setVideoSize(videoQuality.resX, videoQuality.resY);
        mediaRecorder.setVideoFrameRate(videoQuality.framerate);
        mediaRecorder.setVideoEncodingBitRate(videoQuality.bitrate);

        mediaRecorder.setOutputFile(sender.getFileDescriptor());

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "in mediaRecorder.prepare(): "+e.toString());
        }
        mediaRecorder.start();
        Status.streaming = true;
        Status.cameraWorking = true;
    }

    public InputStream getStream()
    {
        if (!Status.streaming)
            throw new IllegalStateException("You must start() the CameraWorker before getStream()");
        try {
            return sender.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "getStream(): "+e.toString());
        }
        finally {
            return null;
        }
    }

    public void stop()
    {
        stopStream();
        lockCamera();
        stopCamera();
        closeSockets();
    }
    private void lockCamera()
    {
        if (!Status.cameraLock)
        {
            try {
                camera.reconnect();
            } catch (IOException e) { }
            Status.cameraLock = true;
        }
    }
    private void unlockCamera()
    {
        if (Status.cameraLock)
        {
            camera.unlock();
            Status.cameraLock = false;
        }
    }
    private void stopStream()
    {
        if (!Status.streaming)
            return;
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        Status.streaming = false;
    }
    private void stopCamera()
    {
        if (!Status.cameraWorking)
            return;
        camera.stopPreview();
        camera.release();
        camera = null;
        Status.cameraWorking = false;
    }

    private LocalServerSocket lss;
    private LocalSocket receiver;
    private LocalSocket sender;
    private void createSockets() throws IOException {
        if (Status.socketReady)
            return;

        final String LOCAL_ADDR = "org.bitman.streaming-";

        int mSocketId = 0;

        for (int i=0;i<10;i++) {
            try {
                mSocketId = new Random().nextInt();
                lss = new LocalServerSocket(LOCAL_ADDR+mSocketId);
                break;
            } catch (IOException e1) {}
        }

        receiver = new LocalSocket();
        receiver.connect( new LocalSocketAddress(LOCAL_ADDR+mSocketId) );
        receiver.setReceiveBufferSize(500000);
        sender = lss.accept();
        sender.setSendBufferSize(500000);

        Status.socketReady = true;
    }

    private void closeSockets() {
        if (!Status.socketReady)
            return;
        try {
            sender.close();
            sender = null;
            receiver.close();
            receiver = null;
            lss.close();
            lss = null;
        } catch (Exception ignore) {}
    }

}