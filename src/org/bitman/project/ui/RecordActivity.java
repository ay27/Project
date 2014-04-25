package org.bitman.project.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtsp.RtspServer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;


public class RecordActivity extends FragmentActivity {

    private static final String TAG = "RecordActivity";

    private Semaphore onlineSemaphore;
    private AsyncInetClient httpClient = AsyncInetClient.getInstance();

    private int time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.record_layout);

        time = getIntent().getIntExtra("record_time", 0);
        scheduleTime(time*1000);

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

        onlineSemaphore = new Semaphore(0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (onlineSemaphore.tryAcquire()) {
                    httpClient.online(null);
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void scheduleTime(final long time)
    {
        if (time <= 0) return;

        Timer timer = new Timer();
        TimerTask task;
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    onBackPressed();
                } catch (Exception e) {
                    Log.e("schedule", e.toString());
                    Toast.makeText(RecordActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        };
        timer.schedule(task, time);
    }


    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
        onlineSemaphore.release();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRtspServer.stop();
        unbindService(mRtspServiceConnection);
        try {
            onlineSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        httpClient.close(AsyncInetClient.Type.Record, null);
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
