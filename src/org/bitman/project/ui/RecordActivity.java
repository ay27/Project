package org.bitman.project.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.*;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtsp.RtspServer;


public class RecordActivity extends FragmentActivity {

    private static final String TAG = "RecordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.record_layout);

        TextView ipView = (TextView) findViewById(R.id.textView_ip);

        String rtspUrl = ProjectApplication.instance.getRtspUrl();
        if (rtspUrl != null)
            ipView.setText(rtspUrl);
        else
            Toast.makeText(this, getResources().getString(R.string.checkYourInternet), Toast.LENGTH_SHORT).show();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_record);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        CameraWorker cameraWorker = CameraWorker.getInstance();
        cameraWorker.setPreviewDisplay(holder);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRtspServer.stop();
        unbindService(mRtspServiceConnection);
    }

    private RtspServer mRtspServer;
    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mRtspServer = null;
        }
    };
}
