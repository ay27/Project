package org.bitman.project.ui.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.ui.SettingsActivity;
import org.bitman.project.ui.user.UserActivity;

public class WelcomeActivity extends FragmentActivity {

    private static final String TAG = "WelcomeActivity";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.welcome_layout);

        if ((ProjectApplication.getUserName()==null) || (ProjectApplication.getUserName().equals("")) || (ProjectApplication.getPassword()==null) || (ProjectApplication.getPassword().equals(""))) {
            Intent intent = new Intent(WelcomeActivity.this, UserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        getActionBar().setTitle("user "+ ProjectApplication.getUserName());

        SectionPagerAdapter mAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.welcome_pager);
        viewPager.setAdapter(mAdapter);
	}

    class SectionPagerAdapter extends FragmentPagerAdapter {

        public SectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new WelcomePage1();
                case 1:
                    return new WelcomePage2();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getResources().getString(R.string.welcome_page1_title);
                case 1: return getResources().getString(R.string.welcome_page2_title);
                default:
                    return null;
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_welcome, menu);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.logout), 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.options:
                intent = new Intent(this.getBaseContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.quit:
                this.onBackPressed();
                return true;
            case R.id.logout:
                ProjectApplication.setUser(null, null);
                intent = new Intent(WelcomeActivity.this, UserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "on destroy");
        android.os.Process.killProcess(Process.myPid());
    }
}
