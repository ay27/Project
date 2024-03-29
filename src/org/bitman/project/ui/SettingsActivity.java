package org.bitman.project.ui;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.IP_Utilities;
import org.bitman.project.record.VideoQuality;


@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "SettingsActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        final ListPreference videoResolution = (ListPreference) findPreference("video_resolution");
        final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");
        final ListPreference videoFramerate = (ListPreference) findPreference("video_framerate");
        final EditTextPreference rtsp_port = (EditTextPreference) findPreference("rtsp_port");
        final EditTextPreference server_address = (EditTextPreference) findPreference("server_address");
        final EditTextPreference phoneId = (EditTextPreference) findPreference("phone_id");

        videoResolution.setEnabled(true);
        videoBitrate.setEnabled(true);
        videoFramerate.setEnabled(true);
        rtsp_port.setEnabled(true);
        server_address.setEnabled(true);
        phoneId.setEnabled(false);

        VideoQuality videoQuality = VideoQuality.getInstance();
        // set the default value
        videoFramerate.setValue(String.valueOf(videoQuality.framerate));
        videoBitrate.setValue(String.valueOf(videoQuality.bitrate));
        videoResolution.setValue(videoQuality.resX + "x" + videoQuality.resY);

        // set the summary
        videoResolution.setSummary(videoResolution.getValue()+"px");
        videoFramerate.setSummary(videoFramerate.getValue()+"fps");
        videoBitrate.setSummary(videoBitrate.getValue()+"kbps");
        rtsp_port.setSummary(rtsp_port.getText());
        server_address.setSummary(server_address.getText());

        phoneId.setSummary(ProjectApplication.IMEI);
        phoneId.setText(ProjectApplication.IMEI);

        videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                videoResolution.setSummary(newValue + "px");
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

        server_address.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!IP_Utilities.isIpAddress((String) newValue))
                    return false;
                server_address.setSummary((String) newValue);
                return true;
            }
        });

    }

}
