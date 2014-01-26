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

public class OptionActivity extends PreferenceActivity {

    private static final String TAG = "OptionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        final SharedPreferences.Editor editor = settings.edit();

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
        Session session = Session.getInstance();
        videoFramerate.setValue(String.valueOf(videoQuality.getFramerate()));
        videoBitrate.setValue(String.valueOf(videoQuality.getBitrate()));
        videoResolution.setValue(videoQuality.getResX()+"x"+videoQuality.getResY());
        rtsp_port.setText(String.valueOf(session.getRtsp_port()));
        server_ip.setText(HttpServer.getInstance().getServer_ip());

        videoResolution.setSummary(videoResolution.getValue()+"px");
        videoFramerate.setSummary(videoFramerate.getValue()+"fps");
        videoBitrate.setSummary(videoBitrate.getValue()+"kbps");
        rtsp_port.setSummary(rtsp_port.getText());
        server_ip.setSummary(server_ip.getText());

        videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //SharedPreferences.Editor editor = settings.edit();
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
                editor.putInt("video_framerate", Integer.parseInt((String)newValue));
                editor.commit();
                videoFramerate.setSummary(newValue+"fps");
                return true;
            }
        });

        videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                editor.putInt("video_bitrate", Integer.parseInt((String)newValue));
                editor.commit();
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
                editor.putInt("rtsp_port", port);
                editor.commit();
                rtsp_port.setSummary((String)newValue);
                return true;
            }
        });

        server_ip.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!GetIP.isIpAddress((String)newValue))
                    return false;
                editor.putString("server_address", (String)newValue);
                editor.commit();
                server_ip.setSummary((String)newValue);
                return true;
            }
        });

    }
}
