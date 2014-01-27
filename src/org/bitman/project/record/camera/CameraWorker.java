package org.bitman.project.record.camera;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import org.bitman.project.record.VideoQuality;

import java.io.IOException;
import java.util.Random;

public class CameraWorker {

    private static final String TAG = "CameraWorker";

    private SurfaceHolder surfaceHolder = null;
    private VideoQuality videoQuality = VideoQuality.getInstance();
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private static CameraWorker instance;
    private CameraWorker() {
        /*try {
            createSockets();
        } catch (IOException e) {
            Log.e(TAG, "in CameraWorker(): "+e.toString());
        }*/
    }
    public static CameraWorker getInstance() {
        if (instance == null)
            return (instance = new CameraWorker());
        else
            return instance;
    }

    //public void setSurfaceHolder(SurfaceHolder holder) { surfaceHolder = holder; }

    protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
    public synchronized void setPreviewDisplay(SurfaceHolder holder) {
        if (mSurfaceHolderCallback != null && holder != null) {
            holder.removeCallback(mSurfaceHolderCallback);
        }
        if (holder != null) {
            mSurfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    start();
                }
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.d(TAG,"Surface Changed !");
                }
            };
            this.surfaceHolder = holder;
            surfaceHolder.addCallback(mSurfaceHolderCallback);
        }
    }

    public void start()
    {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters params = camera.getParameters();
        camera.setParameters(params);
        camera.setDisplayOrientation(videoQuality.orientation);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //camera.startPreview();

        camera.unlock();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setMaxDuration(0);
        mediaRecorder.setVideoEncoder(videoQuality.encoder);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setVideoSize(videoQuality.getResX(), videoQuality.getResY());
        mediaRecorder.setVideoFrameRate(videoQuality.getFramerate());
        mediaRecorder.setVideoEncodingBitRate(videoQuality.getBitrate());

        final String TESTFILE = Environment.getExternalStorageDirectory().getPath()+"/test.mp4";
        mediaRecorder.setOutputFile(TESTFILE);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "in mediaRecorder.prepare(): "+e.toString());
        }
        mediaRecorder.start();
    }

    public void stop()
    {
        Log.i(TAG, "stop");
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        try {
            camera.reconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.stopPreview();
        camera.release();
        camera = null;
    }



    private LocalServerSocket lss;
    private LocalSocket receiver;
    private LocalSocket sender;
    private void createSockets() throws IOException {

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
    }

    private void closeSockets() {
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