package org.bitman.project.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import org.bitman.project.R;


public class RecordActivity extends FragmentActivity {

    private static final String TAG = "RecordActivity";

    private ViewPager mViewPager;
    private SectionPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // here must be set, because the ActionBar.
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.record);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.record_pager);
        mViewPager.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        else
           return super.onOptionsItemSelected(item);
    }

    class SectionPagerAdapter extends FragmentPagerAdapter {

        public SectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    RecordPage1 fragment = new RecordPage1();
                    return fragment;
                case 1:
                    RecordPage2 fragment1 = new RecordPage2();
                    return fragment1;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        public RecordPage1 getRecordPage1() {
            return (RecordPage1) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.record_pager+":0");
        }

        public RecordPage2 getRecordPage2() {

            return (RecordPage2) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.record_pager+":1");
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getResources().getString(R.string.record_page1_title);
                case 1: return getResources().getString(R.string.record_page2_title);
            }
            return null;
        }

    }
}
