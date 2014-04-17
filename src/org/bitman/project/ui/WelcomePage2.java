package org.bitman.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.Button;
import org.bitman.project.R;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-15.
 */
public class WelcomePage2 extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.welcome_page2, container, false);
//        Button button = (Button)root.findViewById(R.id.button_play);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getActivity().getBaseContext(), RecordActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                getActivity().startActivityForResult(intent, 0);
//            }
//        });

        return root;
    }
}
