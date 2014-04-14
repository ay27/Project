package org.bitman.project.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
public class RecordPage1 extends Fragment {

    private static final String TAG = "RecordPage1";

    private static final String RecordOK = "Record_OK";

    private EditText addressEdit;
    private Button searchButton;
    private EditText timeEdit;
    private Button startButton;
    private TextView myHint;
    private AsyncInetClient httpClient = AsyncInetClient.getInstance();
    private ArrayList<String> cityListName;
    private ArrayList<Integer> cityListId;
    private int selectedCity = -1;
    private boolean cityChoice = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        InputMethodManager imm =
                (InputMethodManager)getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);

        View root = inflater.inflate(R.layout.record_page1, null);

        addressEdit = (EditText)root.findViewById(R.id.record_page1_addressEdit);
        // hide the input window
        imm.hideSoftInputFromWindow(addressEdit.getWindowToken(), 0);

        timeEdit = (EditText)root.findViewById(R.id.record_page1_timeEdit);
        imm.hideSoftInputFromWindow(timeEdit.getWindowToken(), 0);

        addressEdit.setOnKeyListener(keyListener);
        timeEdit.setOnKeyListener(keyListener);

        myHint = (TextView)root.findViewById(R.id.record_page1_hint);

        searchButton = (Button)root.findViewById(R.id.record_page1_search);
        startButton = (Button)root.findViewById(R.id.record_page1_start);
        startButton.setOnClickListener(clickListener);
        searchButton.setOnClickListener(clickListener);


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
                httpClient.searchCity(ProjectApplication.IMEI, str, searchCityResponseHandler);
            }
            else if (view.getId() == startButton.getId()) {
                if (cityChoice) {
                    httpClient.record(ProjectApplication.IMEI, Integer.toString(cityListId.get(selectedCity)), ProjectApplication.instance.getRtspUrl(), recordResponseHandler);
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
            if (str.equals(RecordOK)) {
                makeToast(getResources().getString(R.string.startRecord));
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(error.toString());
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
                makeToast(e.toString());
                return;
            }

            DialogInterface.OnClickListener chooseCityListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int position) {
                    selectedCity = position;
                    cityChoice = true;
                    myHint.setTextColor(Color.BLUE);
                    myHint.setText(getResources().getString(R.string.choosedCity)+" "+cityListName.get(position));
                }
            };

            AlertDialog.Builder chooseCityDialog = new AlertDialog.Builder(getActivity());
            chooseCityDialog.setSingleChoiceItems(cityListName.toArray(new String[cityListName.size()]), 0, chooseCityListener).show();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError)+" "+error.toString());
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
