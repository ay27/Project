package org.bitman.project.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import org.bitman.project.R;
import org.bitman.project.record.camera.CameraWorker;

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

    @Override
    public void onBackPressed() {
        cameraWorker.stop();
        onDestroy();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
