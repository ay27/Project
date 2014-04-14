package org.bitman.project.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import org.bitman.project.R;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-14.
 */
public class RecordPage1 extends Fragment {

    private EditText addressEdit;
    private Button searchButton;
    private EditText timeEdit;
    private Button startButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.record_page1, null);

        addressEdit = (EditText)root.findViewById(R.id.record_page1_addressEdit);
        searchButton = (Button)root.findViewById(R.id.record_page1_search);
        timeEdit = (EditText)root.findViewById(R.id.record_page1_timeEdit);
        startButton = (Button)root.findViewById(R.id.record_page1_start);

        searchButton.setOnClickListener(clickListener);

        addressEdit.setOnKeyListener(keyListener);
        timeEdit.setOnKeyListener(keyListener);

        return root;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            addressEdit.clearFocus();
            timeEdit.clearFocus();
            if (view.getId() == searchButton.getId()) {
                System.out.println("search click");
            }
            else if (view.getId() == startButton.getId()) {
                getFragmentManager().beginTransaction();
            }
        }
    };

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (view.getId() == addressEdit.getId())
                    searchButton.callOnClick();
                else if (view.getId() == timeEdit.getId())
                    startButton.callOnClick();
                return false;
            }
            return false;
        }
    };
}
