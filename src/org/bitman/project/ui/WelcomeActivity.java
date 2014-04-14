package org.bitman.project.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;

public class WelcomeActivity extends Activity {

    private static final String TAG = "WelcomeActivity";

    private static WelcomeActivity instance;

    private ToggleButton openHttpButton;
    private Button record_direct;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_layout);
//        setContentView(R.layout.record_page1);


        openHttpButton = (ToggleButton) findViewById(R.id.open_http);
        openHttpButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                if (value) {
                    AsyncInetClient.getInstance().sendMessage(AsyncInetClient.Type.Online, new AsyncInetClient.SendData().setIMEI(ProjectApplication.instance.IMEI), checkOnLineHandler);
                }
            }
        });

        record_direct = (Button) findViewById(R.id.button_record_direct);
        record_direct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(instance, RecordActivity.class);
                startActivityForResult(intent, 0);
            }
        });
        instance = this;
	}

    private AsyncHttpResponseHandler checkOnLineHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            Toast.makeText(WelcomeActivity.this, getResources().getString(R.string.checkOnlineOK), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Toast.makeText(WelcomeActivity.this, getResources().getString(R.string.checkOnlineFailure)+". the reason is "+statusCode, Toast.LENGTH_SHORT).show();
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST+1:
                Intent intent = new Intent(WelcomeActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                return true;
            case Menu.FIRST+2:
                instance.onBackPressed();
                return true;
            default:
                return false;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST+1, 1, "settings");
        menu.add(Menu.NONE, Menu.FIRST + 2, 2, "quit");
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "on destroy");
        android.os.Process.killProcess(Process.myPid());
    }
}
