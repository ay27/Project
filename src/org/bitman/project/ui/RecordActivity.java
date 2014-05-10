package org.bitman.project.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtsp.RtspServer;
import org.bitman.project.ui.utilities.OnlineSender;

import java.util.Timer;
import java.util.TimerTask;


public class RecordActivity extends FragmentActivity {

    private static final String TAG = "RecordActivity";

    private AsyncInetClient httpClient = AsyncInetClient.getInstance();

    private int time = 0;

    private OnlineSender sender;

    private RecordActivity mySelf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.record_layout);

        mySelf = this;

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


        sender = new OnlineSender(AsyncInetClient.Type.Record);
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
                    closeHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Log.e("schedule", e.toString());
                    Toast.makeText(RecordActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        };
        timer.schedule(task, time);
    }

    private Handler closeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mySelf.onBackPressed();
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
        sender.start();
    }

    @Override
    public void onBackPressed() {

        sender.stopSending();
        httpClient.close(AsyncInetClient.Type.Record, null);

//        mRtspServer.stop();
//        unbindService(mRtspServiceConnection);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        android.os.Process.killProcess(android.os.Process.myPid());

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
