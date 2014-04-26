package org.bitman.project.ui.welcome;

import android.content.Intent;
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
import org.bitman.project.ui.utilities.JSONParser;
import org.bitman.project.ui.RecordActivity;
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

    private AsyncInetClient httpClient = AsyncInetClient.getInstance();
    private ArrayList<String> cityListName;
    private ArrayList<Integer> cityListId;

    private int selectedCity = -1;

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

        /* ************************* end of register some view **************************** */

        /* ************************* register some listener to the view ************************ */

        View.OnKeyListener keyListener = new View.OnKeyListener() {
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

        addressEdit.setOnKeyListener(keyListener);
        searchButton = (Button)root.findViewById(R.id.welcome_page1_search);
        searchButton.setOnClickListener(searchButtonListener);

        timeEdit.setOnKeyListener(keyListener);

        /* ************************* end of register some listener to the view ************************ */

        /* ******************* set the visibility *************************** */
        chooseCityLayout.setVisibility(View.INVISIBLE);
        timeLayout.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        /* ********************* end of set the visibility ***************** */

        return root;
    }

    private View.OnClickListener searchButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            chooseCityLayout.setVisibility(View.INVISIBLE);
            timeLayout.setVisibility(View.INVISIBLE);
            startButton.setVisibility(View.INVISIBLE);

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
                e.printStackTrace();
                return;
            }

            ArrayAdapter<String> adapter;
            adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, cityListName);
            chooseCitySpinner.setAdapter(adapter);
            chooseCitySpinner.setOnItemSelectedListener(chooseCitySpinnerListener);

            chooseCityLayout.setVisibility(View.VISIBLE);
            timeLayout.setVisibility(View.INVISIBLE);
            startButton.setVisibility(View.INVISIBLE);
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

            startButton.setOnClickListener(startButtonListener);
            timeLayout.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };


    private View.OnClickListener startButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (selectedCity != -1) {
                httpClient.record(cityListId.get(selectedCity), ProjectApplication.instance.getRtspUrl(), recordResponseHandler);
            }
            else {
                makeToast(getResources().getString(R.string.youMustChooseACity));
            }
        }
    };


    private AsyncHttpResponseHandler recordResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String str = new String(responseBody);

            if (str.equals(AsyncInetClient.RecordOK)) {
                makeToast(getResources().getString(R.string.startRecord));
                Intent intent = new Intent(getActivity(), RecordActivity.class);
                String timeStr = timeEdit.getText().toString();
                if (timeStr == null || timeStr.equals(""))
                    timeStr = "0";
                intent.putExtra("record_time", Integer.parseInt(timeStr));
                startActivity(intent);
            }
            else
                makeToast("error receive from server: "+str);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError)+" "+error.toString());
        }
    };

    private void makeToast(String str) {
        Toast.makeText(this.getActivity(), str, Toast.LENGTH_SHORT).show();
    }
}
