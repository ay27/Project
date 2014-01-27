package org.bitman.project;

import android.app.Application;
import android.content.res.Configuration;
import android.telephony.TelephonyManager;
import org.bitman.project.http.HttpServer;

import java.util.Locale;

/**
 * Created by ay27 on 14-1-27.
 */
public class ProjectApplication extends Application {

    public static ProjectApplication instance;

    @Override
    public void onCreate()
    {
        instance = this;

        // The meid must be set at first.
        String meid = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();
        HttpServer.ShareData.setMEID(meid);

        Locale.setDefault(Locale.CHINA);
        Configuration config = new Configuration();
        config.locale = Locale.CHINA;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }
}
