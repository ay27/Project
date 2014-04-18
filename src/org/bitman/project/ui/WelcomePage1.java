package org.bitman.project.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-14.
 */
public class WelcomePage1 extends Fragment {

    private static final String TAG = "WelcomePage1";

    private EditText addressEdit;
    private Button searchButton;

    private LinearLayout chooseCityLayout;
    private Spinner chooseCitySpinner;

    private LinearLayout timeLayout;
    private EditText timeEdit;

    private Button startButton;

    private TextView myHint;


    private AsyncInetClient httpClient = AsyncInetClient.getInstance();
    private ArrayList<String> cityListName;
    private ArrayList<Integer> cityListId;

    // some flags
    private int selectedCity = -1;
    private boolean cityChoice = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        InputMethodManager imm =
                (InputMethodManager)getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);

        /* ************************* register some view **************************** */
        View root = inflater.inflate(R.layout.welcome_page1, null);

        addressEdit = (EditText)root.findViewById(R.id.welcome_page1_addressEdit);
        imm.hideSoftInputFromWindow(addressEdit.getWindowToken(), 0);
        searchButton = (Button)root.findViewById(R.id.welcome_page1_search);

        chooseCityLayout = (LinearLayout)root.findViewById(R.id.welcome_page1_chooseCity_layout);
        chooseCitySpinner = (Spinner)root.findViewById(R.id.welcome_page1_chooseCity_spinner);

        timeLayout = (LinearLayout)root.findViewById(R.id.welcome_page1_time_layout);
        timeEdit = (EditText)root.findViewById(R.id.welcome_page1_timeEdit);
        imm.hideSoftInputFromWindow(timeEdit.getWindowToken(), 0);

        startButton = (Button)root.findViewById(R.id.welcome_page1_start);

        myHint = (TextView)root.findViewById(R.id.welcome_page1_hint);

        /* ************************* end of register some view **************************** */

        /* ************************* register some listener to the view ************************ */
        addressEdit.setOnKeyListener(keyListener);
        searchButton = (Button)root.findViewById(R.id.welcome_page1_search);
        searchButton.setOnClickListener(clickListener);

        timeEdit.setOnKeyListener(keyListener);

        startButton.setOnClickListener(clickListener);
        /* ************************* end of register some listener to the view ************************ */

        /* ******************* set the visibility *************************** */
        chooseCityLayout.setVisibility(View.INVISIBLE);
        timeLayout.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        /* ********************* end of set the visibility ***************** */

        return root;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == searchButton.getId()) {

                Editable value = addressEdit.getText();
                if (value == null) {
                    makeToast(getResources().getString(R.string.fatalError));
                    return;
                }
                String str  = value.toString();
                if (str == null || str.equals("")) {
                    makeToast(getResources().getString(R.string.youMustInputSomething));
                    return;
                }
                httpClient.searchCity(str, searchCityResponseHandler);
            }
            else if (view.getId() == startButton.getId()) {
                if (cityChoice) {
                    httpClient.record(cityListId.get(selectedCity), ProjectApplication.instance.getRtspUrl(), recordResponseHandler);
                }
                else {
                    makeToast(getResources().getString(R.string.youMustChooseACity));
                }
            }
        }
    };

    private AsyncHttpResponseHandler recordResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String str = new String(responseBody);
            makeToast("receive from server: "+str);
            if (str.equals(AsyncInetClient.RecordOK)) {
                makeToast(getResources().getString(R.string.startRecord));
                Intent intent = new Intent(getActivity(), RecordActivity.class);
                startActivity(intent);
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError)+" "+error.toString());
        }
    };


    private AsyncHttpResponseHandler searchCityResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            cityListId = new ArrayList<Integer>();
            cityListName = new ArrayList<String>();
            String data = new String(responseBody);
            try {
                JSONParser.parseCity(data, cityListId, cityListName);
            } catch (JSONException e) {
                makeToast("search city receive error: "+e.toString());
                return;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.id.welcome_page1_chooseCity_spinner, cityListName);
            chooseCitySpinner.setAdapter(adapter);
            chooseCitySpinner.setOnItemSelectedListener(chooseCitySpinnerListener);
            chooseCitySpinner.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError)+" "+error.toString());
        }
    };

    private AdapterView.OnItemSelectedListener chooseCitySpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            selectedCity = position;
            makeToast(getResources().getString(R.string.choosedCity)+" "+cityListName.get(selectedCity));
            timeLayout.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };


    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (view.getId() == addressEdit.getId()) {
                    searchButton.performClick();
                }
                else if (view.getId() == timeEdit.getId()) {
                    startButton.performClick();
                }
                return true;
            }
            return false;
        }
    };

    private void makeToast(String str) {
        Toast.makeText(this.getActivity(), str, Toast.LENGTH_SHORT).show();
    }
}
