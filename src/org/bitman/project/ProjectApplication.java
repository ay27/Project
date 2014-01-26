package org.bitman.project;

import android.app.Application;
import android.content.res.Configuration;

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

        Locale.setDefault(Locale.CHINA);
        Configuration config = new Configuration();
        config.locale = Locale.CHINA;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }
}
