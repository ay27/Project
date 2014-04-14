package org.bitman.project.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.GetIP;
import org.bitman.project.record.camera.CameraWorker;
import org.bitman.project.record.rtsp.RtspServer;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-14.
 */
public class RecordPage2 extends Fragment {

    private static final String TAG = "RecordPage2";

    private SurfaceView surfaceView;
    private CameraWorker cameraWorker = CameraWorker.getInstance();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.record_page2, null);

        TextView ipView = (TextView) root.findViewById(R.id.textView_ip);
        String ip = null;
        try {
            ip = GetIP.getLocalIpAddress(true).getHostAddress();
            ipView.setText("rtsp://"+ip+ ":"+ PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance).getString("rtsp_port", "8554"));
            Log.i(TAG, ip);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Internet is not accessible. Please check your internet.", Toast.LENGTH_LONG).show();
        }

        surfaceView = (SurfaceView) root.findViewById(R.id.surface_record);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        cameraWorker.setPreviewDisplay(holder);

        return root;
    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().bindService(new Intent(getActivity(), RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRtspServer.stop();
        getActivity().unbindService(mRtspServiceConnection);
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
