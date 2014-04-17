package org.bitman.project.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;
import org.bitman.project.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-17.
 */
public class MyTimePicker extends Dialog {

    private static final String TAG = "MyTimePicker";

    private DatePicker datePicker;
    private TimePicker timePicker;

    private Button okButton;
    private Button cancelButton;

    private Context context;

    private Calendar selectedTime;

    public interface CallbackListener {
        public void onSelectTimeFinish(String time);
    }

    private CallbackListener myListener = null;

    public MyTimePicker(Context context) {
        super(context);

        this.context = context;

        this.setContentView(R.layout.picker_time);

        datePicker = (DatePicker)findViewById(R.id.welcome_page2_chooseDate);
        timePicker = (TimePicker)findViewById(R.id.welcome_page2_chooseTime);
        okButton = (Button)findViewById(R.id.welcome_page2_chooseTimeOk);
        cancelButton = (Button)findViewById(R.id.welcome_page2_chooseTimeCancel);

        okButton.setOnClickListener(okListener);
        cancelButton.setOnClickListener(cancelListener);

        selectedTime = Calendar.getInstance();

        Time time = new Time("GTM+8");
        time.setToNow();

        datePicker.init(time.year, time.month, time.monthDay, dateChangedListener);

        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(time.hour);
        timePicker.setCurrentMinute(time.minute);
        timePicker.setOnTimeChangedListener(timeChangedListener);
    }

    public void setCallbackListener(CallbackListener listener) {
        myListener = listener;
    }

    private View.OnClickListener okListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            makeToast("choose time ok");
            SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd-HH-mm");

            myListener.onSelectTimeFinish(format.format(selectedTime));

            dismiss();
        }
    };

    private View.OnClickListener cancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dismiss();
        }
    };

    @Override
    public void show() {
        if (myListener == null)
            throw new IllegalStateException("you must set the callback listener before show this dialog.");
        super.show();
    }


    private DatePicker.OnDateChangedListener dateChangedListener = new DatePicker.OnDateChangedListener() {
        @Override
        public void onDateChanged(DatePicker view, int year,
                                  int monthOfYear, int dayOfMonth) {
            selectedTime.set(year, monthOfYear, dayOfMonth);
        }
    };

    private TimePicker.OnTimeChangedListener timeChangedListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            selectedTime.set(Time.HOUR, hourOfDay);
            selectedTime.set(Time.MINUTE, minute);
        }
    };

    private void makeToast(String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }
}
