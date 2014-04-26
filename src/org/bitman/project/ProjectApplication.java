package org.bitman.project;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.bitman.project.http.AsyncInetClient;
import org.bitman.project.http.IP_Utilities;
import org.bitman.project.networkmiscellaneous.UPnP_PortMapper;
import org.bitman.project.record.VideoQuality;
import org.bitman.project.record.rtsp.RtspServer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-1-27.
 */
public class ProjectApplication extends Application {

    private static final String TAG = "ProjectApplication";

    public static ProjectApplication instance;

    private SharedPreferences sharedPreferences;
    private VideoQuality videoQuality = VideoQuality.getInstance();

    public static String IMEI = null;
    private static String UserName;
    private static String Password;

    private AsyncInetClient httpClient = AsyncInetClient.getInstance();

    @Override
    public void onCreate()
    {
        instance = this;

        Locale.setDefault(Locale.CHINA);
        Configuration config = new Configuration();
        config.locale = Locale.CHINA;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        setMyIMEI();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);

        readPreference();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UPnP_PortMapper portMapper = UPnP_PortMapper.UPnP_PM_Supplier.getInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setMyIMEI() {
        String deviceId = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();
        String  hardwareId = "35" + Build.BOARD.length()%10+ Build.BRAND.length()%10 + Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 + Build.DISPLAY.length()%10 + Build.HOST.length()%10 + Build.ID.length()%10 + Build.MANUFACTURER.length()%10 + Build.MODEL.length()%10 + Build.PRODUCT.length()%10 + Build.TAGS.length()%10 + Build.TYPE.length()%10 + Build.USER.length()%10;
        String secureId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String WLAN_MAC = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
        String bluetoothMAC = BluetoothAdapter.getDefaultAdapter().getAddress();

        String longId = deviceId+hardwareId+secureId+WLAN_MAC+bluetoothMAC;
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        m.update(longId.getBytes(), 0, longId.length());
        // get md5 byt
        byte p_md5Data[] = m.digest();
        // create a hex string
        String m_szUniqueID = "";
        for (int i=0;i<p_md5Data.length;i++) {
            int b =  (0xFF & p_md5Data[i]);
            // if it is a single digit, make sure it have 0 in front (proper padding)
            if (b <= 0xF)
                m_szUniqueID+="0";
            // add number to string
            m_szUniqueID+=Integer.toHexString(b);
        }
        IMEI = m_szUniqueID.toUpperCase().substring(0, 15);
        System.out.println("IMEI = "+IMEI);

    }

    public String getRtspUrl() {
        try {
            String ip = IP_Utilities.getLocalIpAddress(true).getHostAddress();
            return "rtsp://"+ip+ ":"+ PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance).getString("rtsp_port", "8554");
        } catch (Exception e) {
            return null;
        }
    }

    private String getRecordPath(String ip) {
        return "http://"+ip+":8080/zlw/Servlet/Record";
    }

    private String getPlayPath(String ip) {
        return "http://"+ip+":8080/zlw/Servlet/Play";
    }

    private String getUserPath(String ip) {
        return "http://"+ip+":8080/zlw/Servlet/User";
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
        httpClient.setServer(getRecordPath(ip), getPlayPath(ip), getUserPath(ip));

        UserName = sharedPreferences.getString("UserName", null);
        Password = sharedPreferences.getString("Password", null);

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
                httpClient.setServer(getRecordPath(ip), getPlayPath(ip), getUserPath(ip));
            }
        }
    };

    public static String getUserName() {
        return UserName;
    }

    public static String getPassword() {
        return Password;
    }

    public static void setUser(String userName, String password) {
        UserName = userName;
        Password = password;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProjectApplication.instance);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("UserName", UserName);
        editor.putString("Password", Password);
        editor.commit();
    }
}
