package org.bitman.project.ui;

import android.content.Intent;
import android.os.*;
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
import android.widget.Button;
import android.widget.ToggleButton;
import org.bitman.project.R;

public class WelcomeActivity extends FragmentActivity {

    private static final String TAG = "WelcomeActivity";

    private ViewPager viewPager;
    private SectionPagerAdapter mAdapter;

    private ToggleButton openHttpButton;
    private Button record_direct;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.welcome_layout);

//        Intent intent = new Intent(this, RecordActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivityForResult(intent, 0);

        mAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager)findViewById(R.id.welcome_pager);
        viewPager.setAdapter(mAdapter);
//        viewPager.setSaveEnabled(false);
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

//        public WelcomePage1 getWelcomePage1() {
//            return (WelcomePage1) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.welcome_pager+":0");
//        }
//
//        public WelcomePage2 getWelcomePage2() {
//
//            return (WelcomePage2) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.welcome_pager+":1");
//        }

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


//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case Menu.FIRST+1:
//                Intent intent = new Intent(WelcomeActivity.this, SettingsActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivityForResult(intent, 0);
//                return true;
//            case Menu.FIRST+2:
//                this.onBackPressed();
//                return true;
//            default:
//                return false;
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(Menu.NONE, Menu.FIRST+1, 1, "settings");
//        menu.add(Menu.NONE, Menu.FIRST + 2, 2, "quit");
//        return true;
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.options:
                // Starts QualityListActivity where user can change the streaming quality
                intent = new Intent(this.getBaseContext(), SettingsActivity.class);
//                startActivityFromFragment(mAdapter.getWelcomePage1(), intent, 0);
                startActivity(intent);
                return true;
            case R.id.quit:
                this.onBackPressed();
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
