package org.bitman.project.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.GetIP;
import org.bitman.project.http.HttpServer;
import org.bitman.project.record.Session;
import org.bitman.project.record.VideoQuality;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        settings.registerOnSharedPreferenceChangeListener(changeListener);

        final ListPreference videoResolution = (ListPreference) findPreference("video_resolution");
        final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");
        final ListPreference videoFramerate = (ListPreference) findPreference("video_framerate");
        final EditTextPreference rtsp_port = (EditTextPreference) findPreference("rtsp_port");
        final EditTextPreference server_ip = (EditTextPreference) findPreference("server_ip");

        videoResolution.setEnabled(true);
        videoBitrate.setEnabled(true);
        videoFramerate.setEnabled(true);
        rtsp_port.setEnabled(true);
        server_ip.setEnabled(true);

        VideoQuality videoQuality = VideoQuality.getInstance();
        // set the default value
        videoFramerate.setValue(String.valueOf(videoQuality.getFramerate()));
        videoBitrate.setValue(String.valueOf(videoQuality.getBitrate()));
        videoResolution.setValue(videoQuality.getResX()+"x"+videoQuality.getResY());

        // set the summary
        videoResolution.setSummary(videoResolution.getValue()+"px");
        videoFramerate.setSummary(videoFramerate.getValue()+"fps");
        videoBitrate.setSummary(videoBitrate.getValue()+"kbps");
        rtsp_port.setSummary(rtsp_port.getText());
        server_ip.setSummary(server_ip.getText());

        videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences.Editor editor = settings.edit();
                Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
                Matcher matcher = pattern.matcher((String)newValue);
                matcher.find();
                editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
                editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
                editor.commit();
                videoResolution.setSummary(newValue+"px");
                return true;
            }
        });

        videoFramerate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                videoFramerate.setSummary(newValue+"fps");
                return true;
            }
        });

        videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                videoBitrate.setSummary(newValue+"kbps");
                return true;
            }
        });

        rtsp_port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int port = Integer.parseInt((String)newValue);
                Log.i(TAG, "new port: " + port);
                if (port<=0 || port>=65535)
                    return false;
                rtsp_port.setSummary((String)newValue);
                return true;
            }
        });

        server_ip.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!GetIP.isIpAddress((String) newValue))
                    return false;
                server_ip.setSummary((String)newValue);
                return true;
            }
        });

    }

    private SharedPreferences.OnSharedPreferenceChangeListener changeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            VideoQuality videoQuality = VideoQuality.getInstance();
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
                Session.getInstance().setRtsp_port(Integer.parseInt(sharedPreferences.getString("rtsp_port", "8554")));
            }
            else if (key.equals("server_address")) {
                String ip = sharedPreferences.getString("server_address", "127.0.0.1");
                HttpServer.getInstance().setIP(ip);
            }
        }
    };
}
