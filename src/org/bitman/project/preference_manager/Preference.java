package org.bitman.project.preference_manager;

import android.content.SharedPreferences;
import android.util.Log;
import org.bitman.project.ProjectApplication;
import org.bitman.project.http.HttpServer;
import org.bitman.project.record.Session;
import org.bitman.project.record.VideoQuality;

public class Preference {

    private static final String TAG = "Preference";

    private SharedPreferences settings;
    public VideoQuality videoQuality;

    private static Preference instance;

    private Preference()
    {
        videoQuality = VideoQuality.getInstance();
        settings = android.preference.PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        settings.registerOnSharedPreferenceChangeListener(changeListener);
    }

    public static Preference getInstance()
    {
        if (instance == null)
            return (instance = new Preference());
        else
            return instance;
    }


    private SharedPreferences.OnSharedPreferenceChangeListener changeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("video_resX") || key.equals("video_resY")) {
                videoQuality.setResX(sharedPreferences.getInt("video_resX", 176));
                videoQuality.setResY(sharedPreferences.getInt("video_resY", 144));
            }
            else if (key.equals("video_framerate")) {
                videoQuality.setFramerate(Integer.parseInt(sharedPreferences.getString("video_framerate", "8")));
            }
            else if (key.equals("video_bitrate")) {
                videoQuality.setBitrate(Integer.parseInt(sharedPreferences.getString("video_bitrate", "100")));
            }
            else if (key.equals("rtsp_port")) {
                Session.getInstance().setRtsp_port(Integer.parseInt(key));
            }
            else if (key.equals("server_address")) {
                String ip = sharedPreferences.getString("server_address", null);
                try {
                    HttpServer.getInstance().setIP(ip);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    };
}
