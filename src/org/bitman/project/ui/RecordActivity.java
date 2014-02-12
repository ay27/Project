package org.bitman.project.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import org.bitman.project.R;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtsp.RtspServer;

public class RecordActivity extends Activity {

    private static final String TAG = "RecordActivity";


    private SurfaceView surfaceView;
    private CameraWorker cameraWorker = CameraWorker.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.record_layout);

        surfaceView = (SurfaceView) findViewById(R.id.surface_record);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        cameraWorker.setPreviewDisplay(holder);

        bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);

//        InputStream is = cameraWorker.getStream();
//        byte[] bytes = new byte[1024];
//        try {
//            is.read(bytes);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.i(TAG, new String(bytes));

    }

    @Override
    protected void onStart() {
        super.onStart();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //cameraWorker.start();
    }

    private RtspServer mRtspServer;
    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    public void onBackPressed() {
        cameraWorker.stop();
        onDestroy();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mRtspServer != null)
            mRtspServer.stop();
        super.onDestroy();
    }
}
