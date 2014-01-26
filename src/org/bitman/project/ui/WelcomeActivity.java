package org.bitman.project.ui;

import org.bitman.project.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class WelcomeActivity extends Activity {

    private static WelcomeActivity instance;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_layout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

    public static WelcomeActivity getInstance()
    {
        return instance;
    }

}
