package org.bitman.project.ui.user;

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
import org.bitman.project.R;
import org.bitman.project.ui.SettingsActivity;

public class UserActivity extends FragmentActivity {

    private static final String TAG = "UserActivity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.user_layout);

        SectionPagerAdapter mAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.user_pager);
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
                    return new UserPage1();
                case 1:
                    return new UserPage2();
                case 2:
                    return new UserPage3();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getResources().getString(R.string.user_page1_title);
                case 1: return getResources().getString(R.string.user_page2_title);
                case 2: return getResources().getString(R.string.user_page3_title);
                default:
                    return null;
            }
        }

    }

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
                intent = new Intent(this.getBaseContext(), SettingsActivity.class);
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
