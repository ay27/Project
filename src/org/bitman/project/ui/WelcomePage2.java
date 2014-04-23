package org.bitman.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-15.
 */
public class WelcomePage2 extends Fragment {

    private static final String TAG = "WelcomePage2";

    private EditText addressEdit;
    private Button searchButton;

    private LinearLayout chooseCityLayout;
    private Spinner chooseCitySpinner;

    private LinearLayout chooseModeLayout;
    private RadioGroup modeGroup;
    private RadioButton modeChooseOld;
    private RadioButton modeChooseNow;

    private LinearLayout chooseDeviceLayout;
    private Spinner chooseDeviceSpinner;

    private LinearLayout chooseFileLayout;
    private Spinner chooseFileSpinner;

    private Button playButton;

//    private MyTimePicker timePickerDialog;

    private TextView myHint;

    private AsyncInetClient httpClient = AsyncInetClient.getInstance();

    private static class Status {
        public static int selectedCity_index = -1;
        public static long selectedMode_id = -1;
        public static int selectedDevice_index = -1;
        public static int selectedFile_index = -1;
//        public static String selectedTime_str = null;
        public static String rtspUrl = null;
    }

    private ArrayList<Integer> cityId;
    private ArrayList<String> cityName;
    private ArrayList<String> deviceList;
    private ArrayList<String> fileList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.welcome_page2, container, false);

        InputMethodManager imm =
                (InputMethodManager)getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);


        /* ************************* register some view **************************** */

        addressEdit = (EditText)root.findViewById(R.id.welcome_page2_addressEdit);
        imm.hideSoftInputFromWindow(addressEdit.getWindowToken(), 0);
        searchButton = (Button)root.findViewById(R.id.welcome_page2_search);

        chooseCityLayout = (LinearLayout)root.findViewById(R.id.welcome_page2_chooseCity_layout);
        chooseCitySpinner = (Spinner)root.findViewById(R.id.welcome_page2_chooseCity_spinner);

        chooseModeLayout = (LinearLayout)root.findViewById(R.id.welcome_page2_chooseMode_layout);
        modeGroup = (RadioGroup)root.findViewById(R.id.welcome_page2_radioGroup);
        modeChooseOld = (RadioButton)root.findViewById(R.id.welcome_page2_choose_old);
        modeChooseNow = (RadioButton)root.findViewById(R.id.welcome_page2_choose_now);

        chooseDeviceLayout = (LinearLayout)root.findViewById(R.id.welcome_page2_chooseDevice_layout);
        chooseDeviceSpinner = (Spinner)root.findViewById(R.id.welcome_page2_chooseDevice_spinner);

        chooseFileLayout = (LinearLayout)root.findViewById(R.id.welcome_page2_chooseFile_layout);
        chooseFileSpinner = (Spinner)root.findViewById(R.id.welcome_page2_chooseFile_spinner);

        playButton = (Button)root.findViewById(R.id.welcome_page2_play_button);

        myHint = (TextView)root.findViewById(R.id.welcome_page2_hint);

        /* ************************* end of register some view **************************** */

        /* ************************ register the listener  ******************************* */
        searchButton.setOnClickListener(buttonClickListener);
        /* ************************ end of register ****************************** */

        /* ****************************** set the visibility ************************** */
        chooseCityLayout.setVisibility(View.INVISIBLE);
        chooseModeLayout.setVisibility(View.INVISIBLE);
        chooseDeviceLayout.setVisibility(View.INVISIBLE);
        chooseFileLayout.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.INVISIBLE);
        /* ************************ end of set ********************************* */

        return root;
    }

    private long nowID = 0;

    /* ************************ set the Visibility dynamic & register the Listener dynamic ******************** */
    private void handleUIVisibility() {
        if (nowID == searchButton.getId()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, cityName);
            chooseCitySpinner.setAdapter(adapter);
            chooseCitySpinner.setOnItemSelectedListener(chooseCitySpinnerListener);
            chooseCityLayout.setVisibility(View.VISIBLE);
        }
        else if (nowID == chooseCitySpinner.getId()) {
            modeGroup.setOnCheckedChangeListener(modeChangeListener);
            chooseModeLayout.setVisibility(View.VISIBLE);
        }
        else if (nowID == modeGroup.getId()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, deviceList);
            chooseDeviceSpinner.setAdapter(adapter);
            chooseDeviceSpinner.setOnItemSelectedListener(chooseDeviceListener);
            chooseDeviceLayout.setVisibility(View.VISIBLE);
        }
        else if (nowID == chooseDeviceLayout.getId()) {
            if (Status.selectedMode_id == modeChooseOld.getId()) {
//                if (Status.selectedPhone) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, fileList);
                chooseFileSpinner.setAdapter(adapter);
                chooseFileSpinner.setOnItemSelectedListener(chooseFileListener);
                chooseFileLayout.setVisibility(View.VISIBLE);
//                }
//                else if (Status.selectedCam) {
//                    timePickerDialog = new MyTimePicker(getActivity());
//                    timePickerDialog.setCallbackListener(pickTimeCallback);
//                    timePickerDialog.show();
//                }
            } else if (Status.selectedMode_id == modeChooseNow.getId()) {
                playButton.setOnClickListener(buttonClickListener);
                playButton.setVisibility(View.VISIBLE);
            }
        }
        else if (nowID == chooseFileLayout.getId()) {
            playButton.setOnClickListener(buttonClickListener);
            playButton.setVisibility(View.VISIBLE);
        }
    }

//    private MyTimePicker.CallbackListener pickTimeCallback = new MyTimePicker.CallbackListener() {
//        @Override
//        public void onSelectTimeFinish(String time) {
//            Status.selectedTime_str = time;
//            playButton.setOnClickListener(buttonClickListener);
//            playButton.setVisibility(View.VISIBLE);
//        }
//    };


    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == searchButton.getId()) {

//                Intent intent = new Intent(getActivity(), PlayerActivity.class);
////                intent.putExtra("play_address", "rtsp://"+addressEdit.getText().toString()+":8554/");
//                intent.putExtra("play_address", "rtsp://192.168.1.108:8554/");
//                startActivity(intent);

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
            else if (view.getId() == playButton.getId()) {
                if (Status.selectedMode_id == modeChooseOld.getId())
                    httpClient.playFile(deviceList.get(Status.selectedDevice_index), fileList.get(Status.selectedFile_index), playFileResponseHandler);
                else
                    httpClient.playNow(deviceList.get(Status.selectedDevice_index), playNowResponseListener);
            }
        }
    };

    AsyncHttpResponseHandler playFileResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String str = new String(responseBody);
            if (str.startsWith(AsyncInetClient.StartOfRtsp)) {
                Status.rtspUrl = str;

                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("play_address", Status.rtspUrl);
                startActivity(intent);
            }
            else {
                makeToast("receive in play file: "+str);
                return;
            }

        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError) + " " + error.toString());
        }
    };

    AsyncHttpResponseHandler playNowResponseListener = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String str = new String(responseBody);
            if (str.startsWith(AsyncInetClient.StartOfRtsp)) {
                Status.rtspUrl = str;

                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("play_address", Status.rtspUrl);
                startActivity(intent);
            }
            else
                makeToast("receive: "+str);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(error.toString());
        }
    };

    private AsyncHttpResponseHandler searchCityResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            cityId = new ArrayList<Integer>();
            cityName = new ArrayList<String>();
            String data = new String(responseBody);
            try {
                JSONParser.parseCity(data, cityId, cityName);
            } catch (JSONException e) {
                makeToast("search city receive error: "+e.toString());
                return;
            }

            // TODO: test it, maybe it's a fault.
            nowID = searchButton.getId();
            handleUIVisibility();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            makeToast(getResources().getString(R.string.fatalError)+" "+error.toString());
        }
    };


    private AdapterView.OnItemSelectedListener chooseCitySpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            if (position>=0 && position<cityId.size())
                Status.selectedCity_index = position;
            else {
                makeToast("fatal error in selected city");
                return;
            }

            // TODO: test it
            nowID = chooseCitySpinner.getId();
            handleUIVisibility();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };


    private AdapterView.OnItemSelectedListener chooseDeviceListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, final int position, long id) {

            AsyncHttpResponseHandler listFileResponseHandler = new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    fileList = new ArrayList<String>();
                    String str = new String(responseBody);
                    try {
                        org.bitman.project.ui.JSONParser.parseFileList(str, fileList);
                    } catch (JSONException e) {
                        makeToast("receive error in list file: "+str);
                        e.printStackTrace();
                        return;
                    }

                    if (position>=0 && position<deviceList.size())
                        Status.selectedDevice_index = position;
                    else {
                        makeToast("fatal error in selected device");
                        return;
                    }

                    nowID = chooseDeviceLayout.getId();
                    handleUIVisibility();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    makeToast(getResources().getString(R.string.fatalError) + " " + error.toString());
                }
            };

            Status.selectedDevice_index = position;

            if (Status.selectedMode_id == modeChooseOld.getId()) {
                httpClient.listFile(deviceList.get(position), listFileResponseHandler);
            }
            else {
                nowID = chooseDeviceLayout.getId();
                handleUIVisibility();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    private RadioGroup.OnCheckedChangeListener modeChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final RadioGroup radioGroup, int checkId) {
            makeToast("choose "+checkId);

            AsyncHttpResponseHandler listDeviceResponseHandler = new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    deviceList = new ArrayList<String>();
                    String str = new String(responseBody);
                    try {
                        org.bitman.project.ui.JSONParser.parseCamList(str, deviceList);
                    } catch (JSONException e) {
                        makeToast("receive error in list device: "+str);
                        e.printStackTrace();
                        return;
                    }

                    Status.selectedMode_id = radioGroup.getCheckedRadioButtonId();

                    nowID = modeGroup.getId();
                    handleUIVisibility();

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    makeToast(getResources().getString(R.string.fatalError) + " " + error.toString());
                }
            };

            if (radioGroup.getCheckedRadioButtonId() == modeChooseOld.getId()) {
                httpClient.listPast(cityId.get(Status.selectedCity_index), listDeviceResponseHandler);
            }
            else if (radioGroup.getCheckedRadioButtonId() == modeChooseNow.getId()) {
                httpClient.listNow(cityId.get(Status.selectedCity_index), listDeviceResponseHandler);
            }
        }
    };

    private AdapterView.OnItemSelectedListener chooseFileListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, final int position, long id) {

            Status.selectedFile_index = position;

            nowID = chooseFileLayout.getId();
            handleUIVisibility();

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };


    private void makeToast(String str) {
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }
}
