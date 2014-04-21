package org.bitman.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-21.
 */
public class UserPage1 extends Fragment {

    private static final String TAG = "UserPage1";

    private EditText userName, passwd;
    private Button logIn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.user_page1, null);

        userName = (EditText)root.findViewById(R.id.user_page1_username_edit);
        passwd = (EditText)root.findViewById(R.id.user_page1_passwd_edit);
        logIn = (Button)root.findViewById(R.id.user_page1_login);

        logIn.setOnClickListener(clickListener);

        return root;
    }

    private String userNameStr, passwdStr;
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
                //************************************************************************//
                //************************************************************************//
                // Get the user name.
                Editable userEditable = userName.getText();
                if (userEditable == null || userEditable.toString().equals("")) {
                    toastMessage(getString(R.string.inputYourUserName));
                    return;
                }
                userNameStr = userEditable.toString();
                if (!Checker.isUserName(userNameStr)) {
                    toastMessage(getString(R.string.inputSuitableUserName));
                    return;
                }
                Log.i(TAG, "get the user name: " + userNameStr);


                //************************************************************************//
                //************************************************************************//
                // Get the passwd.
                Editable passwdEditable = passwd.getText();
                if (passwdEditable == null || passwdEditable.toString().equals("")) {
                    toastMessage(getString(R.string.inputYourUserPasswd));
                    return;
                }
                passwdStr = passwdEditable.toString();
                if (!Checker.isPasswd(passwdStr)) {
                    toastMessage(getString(R.string.inputSuitableUserPasswd));
                    return;
                }
                Log.i(TAG, "get the user Passwd: "+passwdStr);

            AsyncInetClient client = AsyncInetClient.getInstance();
            client.login(userNameStr, passwdStr, loginResponseHandler);
            }
    };

    private AsyncHttpResponseHandler loginResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String receive = new String(responseBody);
            if (!receive.equals(AsyncInetClient.LoginOK)) {
                toastMessage("error in login receive: "+receive);
                return;
            }
            ProjectApplication.setUser(userNameStr, passwdStr);
            Intent intent = new Intent(getActivity(), WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            toastMessage("error in login.");
            error.printStackTrace();
        }
    };

    private void toastMessage(String str) {
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }
}
