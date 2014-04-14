package org.bitman.project;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.bitman.project.http.AsyncInetClient;
import org.bitman.project.record.VideoQuality;
import org.bitman.project.record.rtsp.RtspServer;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ay27 on 14-1-27.
 */
public class ProjectApplication extends Application {

    private static final String TAG = "ProjectApplication";

    public static ProjectApplication instance;

    private SharedPreferences sharedPreferences;
    private VideoQuality videoQuality = VideoQuality.getInstance();

    public String IMEI;

    @Override
    public void onCreate()
    {
        instance = this;

        IMEI = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();

        Locale.setDefault(Locale.CHINA);
        Configuration config = new Configuration();
        config.locale = Locale.CHINA;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);

        readPreference();
    }

    private String getPath(String ip) {
        return "http://"+ip+":8080/Servlet/";
    }

    private void readPreference() {
        Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
        Matcher matcher = pattern.matcher((String)sharedPreferences.getString("video_resolution", "176x144"));
        matcher.find();
        videoQuality.resX = Integer.parseInt(matcher.group(1));
        videoQuality.resY = Integer.parseInt(matcher.group(2));

        videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "100"));
        videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "8"));

        RtspServer.setRtspPort(Integer.parseInt(sharedPreferences.getString("rtsp_port", "8554")));

        String ip = sharedPreferences.getString("server_address", "127.0.0.1");
        AsyncInetClient.getInstance().setServer(getPath(ip));

    }

    private SharedPreferences.OnSharedPreferenceChangeListener changeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.i(TAG, key);

            if (key.equals("video_resolution")) {
                Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
                Matcher matcher = pattern.matcher((String)sharedPreferences.getString("video_resolution", "176x144"));
                matcher.find();
                videoQuality.resX = Integer.parseInt(matcher.group(1));
                videoQuality.resY = Integer.parseInt(matcher.group(2));
            }
            else if (key.equals("video_framerate")) {
                videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "8"));
            }
            else if (key.equals("video_bitrate")) {
                videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "100"));
            }
            else if (key.equals("rtsp_port")) {
                RtspServer.setRtspPort(Integer.parseInt(sharedPreferences.getString("rtsp_port", "8554")));
            }
            else if (key.equals("server_address")) {
                String ip = sharedPreferences.getString("server_address", "127.0.0.1");
                AsyncInetClient.getInstance().setServer(getPath(ip));
            }
        }
    };
}
