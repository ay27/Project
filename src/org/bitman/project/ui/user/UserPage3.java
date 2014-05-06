package org.bitman.project.ui.user;

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
import org.bitman.project.ui.utilities.Checker;
import org.bitman.project.ui.welcome.WelcomeActivity;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-21.
 */
public class UserPage3 extends Fragment {

    private static final String TAG = "UserPage3";

    private EditText userName, passwd, newPasswd, confirm;
    private Button update;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.user_page3, null);

        userName = (EditText)root.findViewById(R.id.user_page3_username_edit);
        passwd = (EditText)root.findViewById(R.id.user_page3_passwd_edit);
        newPasswd = (EditText)root.findViewById(R.id.user_page3_passwd_newPasswd_edit);
        confirm = (EditText)root.findViewById(R.id.user_page3_passwd_confirm_edit);
        update = (Button)root.findViewById(R.id.user_page3_remew_button);

        update.setOnClickListener(clickListener);

        return root;
    }

    private String userNameStr, passwdStr, newPasswdStr;
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

            //************************************************************************//
            //************************************************************************//
            Editable newPasswdEditable = newPasswd.getText();
            if (newPasswdEditable == null || newPasswdEditable.toString().equals("")) {
                toastMessage(getString(R.string.inputNewPasswd));
                return;
            }
            newPasswdStr = newPasswdEditable.toString();
            if (!Checker.isPasswd(newPasswdStr)) {
                toastMessage(getString(R.string.inputSuitableUserPasswd));
                return;
            }

            //************************************************************************//
            //************************************************************************//
            // confirm the passwd.
            Editable passwdConfirmEditable = confirm.getText();
            if (passwdConfirmEditable == null
                    || passwdConfirmEditable.toString().equals("")) {
                toastMessage(getString(R.string.inputYourUserPasswdAgain));
                return;
            }
            String passwdConfirm = passwdConfirmEditable.toString();
            if (!passwdConfirm.equals(newPasswdStr)) {
                toastMessage(getString(R.string.inputSamePasswd));
                return;
            }
            Log.i("Register", "get the userPasswd: " + passwdStr);

           AsyncInetClient.getInstance().rePasswd(userNameStr, passwdStr, newPasswdStr, updateResponseHandler);

        }
    };

    private AsyncHttpResponseHandler updateResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String receive = new String(responseBody);
            if (!receive.equals(AsyncInetClient.UpdateOK)) {
                toastMessage("error in sign up, receive: "+receive);
                return;
            }
            ProjectApplication.setUser(userNameStr, newPasswdStr, true);
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
