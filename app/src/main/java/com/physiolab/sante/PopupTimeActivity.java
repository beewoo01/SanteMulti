package com.physiolab.sante;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.santemulti.R;

public class PopupTimeActivity extends AppCompatActivity {

    private ScreenSize screen=null;
    private int deviceLenth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();

        setContentView(R.layout.dialog_time);

        screen = new ScreenSize();
        screen.getStandardSize(this);
        deviceLenth = getIntent().getIntExtra("deviceLength", 1);

        InitControl();
    }

    private final Handler mHideHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            hideSystemUI();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action. When the window gains focus,
        // hide the system UI.
        //Log.d(TAG,"onWindowFocusChanged");

        if (hasFocus) {
            delayedHide(100);
        } else {
            mHideHandler.removeMessages(0);
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LOW_PROFILE |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeMessages(0);
        mHideHandler.sendEmptyMessageDelayed(0, delayMillis);
    }

    @SuppressLint("DefaultLocale")
    private void InitControl()
    {
        Button btn;
        TextView tb;
        EditText et;
        SanteApp[] santeApps;
        if (deviceLenth == 1){
            santeApps = new SanteApp[]{(SanteApp)this.getApplication()};
        }else {
            santeApps = new SanteApp[]{(SanteApp)this.getApplication(), (SanteApp)this.getApplication()};
        }

        float div;

        div = (float)getResources().getInteger(R.integer.txt_popup_title);

        tb = (TextView)findViewById(R.id.txt_title_time);
        tb.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);

        div = (float)getResources().getInteger(R.integer.spin_popup_filter);

        tb = findViewById(R.id.txt_time_range);
        tb.setTextSize((float) (screen.standardSize_X / div));

        et = findViewById(R.id.edit_time_range);
        et.setTextSize((float) (screen.standardSize_X / div));
        et.setText(String.format("%.1f",santeApps[0].GetTimeRange(0)));

        div = (float)getResources().getInteger(R.integer.btn_popup);

        btn = (Button)findViewById(R.id.btn_ok);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //SanteApp[] santeApps = new SanteApp[]{(SanteApp)getApplication(), (SanteApp)getApplication()};

                TextView tb = (TextView)findViewById(R.id.edit_time_range);
                String str = tb.getText().toString();
                for (int i = 0; i < deviceLenth; i++){
                    float value;
                    try{
                        value = Float.parseFloat(str);
                    }
                    catch(Exception e)
                    {
                        value = santeApps[i].GetTimeRange(i);
                    }

                    if (value>300.0f) value=300.0f;
                    if (value<santeApps[i].GetTimeMinimunRange()) value=santeApps[i].GetTimeMinimunRange();
                    santeApps[i].SetTimeRange(value, i);
                }


                setResult(BTService.RESULT_OK);
                finish();
            }
        });

        btn = (Button)findViewById(R.id.btn_cancle);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(BTService.RESULT_CANCLE);
                finish();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            if (x < 0 || y < 0)
                return false;

            View popup = (View)findViewById(R.id.view_main);
            Rect areaPopup = new Rect(popup.getLeft(),popup.getTop(),popup.getRight(),popup.getBottom());

            if (!areaPopup.contains(x,y)) {
                setResult(BTService.RESULT_CANCLE);
                finish();
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

}

