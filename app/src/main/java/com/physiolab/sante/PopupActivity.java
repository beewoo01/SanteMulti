package com.physiolab.sante;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.santemulti.R;

public class PopupActivity  extends AppCompatActivity {
    private int requestType=-1;
    private ScreenSize screen=null;
    private boolean[] enableAxis=null;
    private LinearLayout linearLayout;

    private int deviceNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();

        setContentView(R.layout.dialog_acceleration);

        screen = new ScreenSize();
        screen.getStandardSize(this);

        Intent intent = getIntent();
        requestType = intent.getIntExtra("Type", BTService.REQUEST_EMG_FILTER);
        deviceNum = intent.getIntExtra("DeviceNum", 1);

        try
        {
            enableAxis = intent.getBooleanArrayExtra("EnableAxis");
        }
        catch(Exception e)
        {
            e.printStackTrace();

            enableAxis = new boolean[3];
        }
        if (enableAxis==null) enableAxis = new boolean[3];

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

    private void InitControl()
    {
        Button btn;
        TextView tb;
        EditText et;
        CheckBox cb;
        Spinner spinDevice;
        SpinnerAdapter adapterDevice;
        SanteApp app = (SanteApp)this.getApplication();
        float div;

        div = (float)getResources().getInteger(R.integer.txt_popup_title);
        tb = (TextView)findViewById(R.id.txt_axis_title);
//        tb.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);
        if (requestType==BTService.REQUEST_Acc_FILTER) tb.setText(R.string.txt_title_acc);
        else if (requestType==BTService.REQUEST_Gyro_FILTER) tb.setText(R.string.txt_title_gyro);
        else if (requestType==BTService.REQUEST_EMG_FILTER) tb.setText(R.string.txt_title_emg);
        else tb.setText(R.string.txt_title_emg);

//        div = (float)getResources().getInteger(R.integer.btn_popup);

        btn = (Button)findViewById(R.id.btn_ok);
//        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Spinner spinDevice;
                float minValue;
                float maxValue;
                float tempValue;
                String str="";
                TextView tb;

                tb = (TextView)findViewById(R.id.edit_range_min);
                str = tb.getText().toString();
                minValue = Float.parseFloat(str);
                tb = (TextView)findViewById(R.id.edit_range_max);
                maxValue = Float.parseFloat(tb.getText().toString());
                if (maxValue<minValue)
                {
                    tempValue=maxValue;
                    maxValue=minValue;
                    minValue=tempValue;
                }

                SanteApp[] app = new SanteApp[]{(SanteApp)getApplication(), (SanteApp)getApplication()};
                //SanteApp app = (SanteApp)getApplication();
                for (int i = 0; i < deviceNum; i++){
                    if (requestType==BTService.REQUEST_Acc_FILTER)
                    {
                        linearLayout = (LinearLayout)findViewById(R.id.spinner_layout_notch);
                        linearLayout.setVisibility(View.GONE);

                        spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                        app[i].SetAccHPF(spinDevice.getSelectedItemPosition(), i);

                        spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                        app[i].SetAccLPF(spinDevice.getSelectedItemPosition(), i);

                        minValue = minValue*10.0f;
                        minValue = (float)Math.floor(minValue);
                        minValue = minValue/10.0f;
                        maxValue = maxValue*10.0f;
                        maxValue = (float)Math.floor(maxValue);
                        maxValue = maxValue/10.0f;

                        if (maxValue>app[i].GetFullAccMax()) maxValue=app[i].GetFullAccMax();
                        if (minValue<app[i].GetFullAccMin()) minValue=app[i].GetFullAccMin();

                        if (maxValue-minValue<app[i].GetAccMinimunRange())
                        {
                            if (maxValue<app[i].GetFullAccMin() + app[i].GetAccMinimunRange())
                            {
                                maxValue=app[i].GetFullAccMin() + app[i].GetAccMinimunRange();
                            }
                            minValue = maxValue-app[i].GetAccMinimunRange();
                        }
                        app[i].SetAccMax(maxValue, i);
                        app[i].SetAccMin(minValue, i);
                    }
                    else if (requestType==BTService.REQUEST_Gyro_FILTER)
                    {
                        linearLayout = (LinearLayout)findViewById(R.id.spinner_layout_notch);
                        linearLayout.setVisibility(View.GONE);

                        spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                        app[i].SetGyroHPF(spinDevice.getSelectedItemPosition(), i);

                        spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                        app[i].SetGyroLPF(spinDevice.getSelectedItemPosition(), i);

                        minValue = minValue*10.0f;
                        minValue = (float)Math.floor(minValue);
                        minValue = minValue/10.0f;
                        maxValue = maxValue*10.0f;
                        maxValue = (float)Math.floor(maxValue);
                        maxValue = maxValue/10.0f;

                        if (maxValue > app[i].GetFullGyroMax()) maxValue=app[i].GetFullGyroMax();
                        if (minValue < app[i].GetFullGyroMin()) minValue=app[i].GetFullGyroMin();

                        if (maxValue-minValue < app[i].GetGyroMinimunRange())
                        {
                            if (maxValue<app[i].GetFullGyroMin() + app[i].GetGyroMinimunRange())
                            {
                                maxValue=app[i].GetFullGyroMin() + app[i].GetGyroMinimunRange();
                            }
                            minValue = maxValue-app[i].GetGyroMinimunRange();
                        }
                        app[i].SetGyroMax(maxValue, i);
                        app[i].SetGyroMin(minValue, i);
                    }
                    else if (requestType==BTService.REQUEST_EMG_FILTER)
                    {
                        spinDevice = (Spinner)findViewById(R.id.spin_notch);
                        app[i].SetEMGNotch(spinDevice.getSelectedItemPosition(), i);



                        spinDevice = (Spinner)findViewById(R.id.spin_hpf);

                        //Toast.makeText(PopupActivity.this, "스피너 아이탬 포지션?" + spinDevice.getSelectedItemPosition(), Toast.LENGTH_LONG).show();
                        app[i].SetEMGHPF(spinDevice.getSelectedItemPosition(), i);

                        spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                        app[i].SetEMGLPF(spinDevice.getSelectedItemPosition(), i);

                        spinDevice = (Spinner)findViewById(R.id.spin_rms);
                        app[i].SetEMGRMS(spinDevice.getSelectedItemPosition(), i);


                        minValue = (float)Math.floor(minValue);
                        maxValue = (float)Math.floor(maxValue);

                        if (maxValue > app[i].GetFullEMGMax()) maxValue=app[i].GetFullEMGMax();
                        if (minValue < app[i].GetFullEMGMin()) minValue=app[i].GetFullEMGMin();

                        if (maxValue-minValue < app[i].GetEMGMinimunRange())
                        {
                            if (maxValue < app[i].GetFullEMGMin() + app[i].GetEMGMinimunRange())
                            {
                                maxValue=app[i].GetFullEMGMin() + app[i].GetEMGMinimunRange();
                            }
                            minValue = maxValue-app[i].GetEMGMinimunRange();
                        }
                        app[i].SetEMGMax(maxValue, i);
                        app[i].SetEMGMin(minValue, i);
                    }

                    Intent result = new Intent();
                    result.putExtra("EnableAxis",enableAxis);
                    setResult(BTService.RESULT_OK,result);

                    finish();
                }




                /*if (requestType==BTService.REQUEST_Acc_FILTER)
                {
                    linearLayout = (LinearLayout)findViewById(R.id.spinner_layout_notch);
                    linearLayout.setVisibility(View.GONE);

                    spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                    app.SetAccHPF(spinDevice.getSelectedItemPosition());

                    spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                    app.SetAccLPF(spinDevice.getSelectedItemPosition());

                    minValue = minValue*10.0f;
                    minValue = (float)Math.floor(minValue);
                    minValue = minValue/10.0f;
                    maxValue = maxValue*10.0f;
                    maxValue = (float)Math.floor(maxValue);
                    maxValue = maxValue/10.0f;

                    if (maxValue>app.GetFullAccMax()) maxValue=app.GetFullAccMax();
                    if (minValue<app.GetFullAccMin()) minValue=app.GetFullAccMin();

                    if (maxValue-minValue<app.GetAccMinimunRange())
                    {
                        if (maxValue<app.GetFullAccMin()+app.GetAccMinimunRange())
                        {
                            maxValue=app.GetFullAccMin()+app.GetAccMinimunRange();
                        }
                        minValue = maxValue-app.GetAccMinimunRange();
                    }
                    app.SetAccMax(maxValue);
                    app.SetAccMin(minValue);
                }
                else if (requestType==BTService.REQUEST_Gyro_FILTER)
                {
                    linearLayout = (LinearLayout)findViewById(R.id.spinner_layout_notch);
                    linearLayout.setVisibility(View.GONE);
                    spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                    app.SetGyroHPF(spinDevice.getSelectedItemPosition());

                    spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                    app.SetGyroLPF(spinDevice.getSelectedItemPosition());

                    minValue = minValue*10.0f;
                    minValue = (float)Math.floor(minValue);
                    minValue = minValue/10.0f;
                    maxValue = maxValue*10.0f;
                    maxValue = (float)Math.floor(maxValue);
                    maxValue = maxValue/10.0f;

                    if (maxValue>app.GetFullGyroMax()) maxValue=app.GetFullGyroMax();
                    if (minValue<app.GetFullGyroMin()) minValue=app.GetFullGyroMin();

                    if (maxValue-minValue<app.GetGyroMinimunRange())
                    {
                        if (maxValue<app.GetFullGyroMin()+app.GetGyroMinimunRange())
                        {
                            maxValue=app.GetFullGyroMin()+app.GetGyroMinimunRange();
                        }
                        minValue = maxValue-app.GetGyroMinimunRange();
                    }
                    app.SetGyroMax(maxValue);
                    app.SetGyroMin(minValue);
                }
                else if (requestType==BTService.REQUEST_EMG_FILTER)
                {

                    Log.d("POPUPACTIVITY1", String.valueOf(requestType));
                    Log.d("POPUPACTIVITY2", String.valueOf(requestType));
                    Log.d("POPUPACTIVITY3", String.valueOf(requestType));
                    Log.d("POPUPACTIVITY4", String.valueOf(requestType));
                    Log.d("POPUPACTIVITY5", String.valueOf(requestType));
                    spinDevice = (Spinner)findViewById(R.id.spin_notch);
                    app.SetEMGNotch(spinDevice.getSelectedItemPosition());

                    // TODO: 4/23/21 여기 가 EMG GPF 설정 부분

                    spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                    Log.wtf("스피너 아이탬 포지션?", spinDevice.getSelectedItemPosition() +"");
                    Toast.makeText(PopupActivity.this, "스피너 아이탬 포지션?" + spinDevice.getSelectedItemPosition(), Toast.LENGTH_LONG).show();
                    app.SetEMGHPF(spinDevice.getSelectedItemPosition());

                    spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                    app.SetEMGLPF(spinDevice.getSelectedItemPosition());

                    minValue = (float)Math.floor(minValue);
                    maxValue = (float)Math.floor(maxValue);

                    if (maxValue>app.GetFullEMGMax()) maxValue=app.GetFullEMGMax();
                    if (minValue<app.GetFullEMGMin()) minValue=app.GetFullEMGMin();

                    if (maxValue-minValue<app.GetEMGMinimunRange())
                    {
                        if (maxValue<app.GetFullEMGMin()+app.GetEMGMinimunRange())
                        {
                            maxValue=app.GetFullEMGMin()+app.GetEMGMinimunRange();
                        }
                        minValue = maxValue-app.GetEMGMinimunRange();
                    }
                    app.SetEMGMax(maxValue);
                    app.SetEMGMin(minValue);
                }

                Intent result = new Intent();
                result.putExtra("EnableAxis",enableAxis);
                setResult(BTService.RESULT_OK,result);

                finish();*/
            }
        });

        btn = (Button)findViewById(R.id.btn_cancle);
//        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP,screen.standardSize_X / div);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(BTService.RESULT_CANCLE);
                finish();
            }
        });

        div = (float)getResources().getInteger(R.integer.spin_popup_filter);

        tb = findViewById(R.id.txt_range_min);
//        tb.setTextSize((float) (screen.standardSize_X / div));

        tb = findViewById(R.id.txt_range_max);
//        tb.setTextSize((float) (screen.standardSize_X / div));

        for (int i = 0; i < deviceNum; i++){
            if (requestType==BTService.REQUEST_Acc_FILTER)
            {
                View layout = findViewById(R.id.spinner_layout_notch);
                layout.setVisibility(View.INVISIBLE);


                spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"HPF None","HPF 0.5Hz","HPF 1Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetAccHPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"LPF None","LPF 10Hz","LPF 20Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetAccLPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                et = findViewById(R.id.edit_range_min);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.1f",app.GetAccMin(i)));

                et = findViewById(R.id.edit_range_max);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.1f",app.GetAccMax(i)));
            }
            else if (requestType==BTService.REQUEST_Gyro_FILTER)
            {
                View layout = findViewById(R.id.spinner_layout_notch);
                layout.setVisibility(View.INVISIBLE);

                spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"HPF None","HPF 0.5Hz","HPF 1Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetGyroHPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"LPF None","LPF 10Hz","LPF 20Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetGyroLPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                et = findViewById(R.id.edit_range_min);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.1f",app.GetGyroMin(i)));

                et = findViewById(R.id.edit_range_max);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.1f",app.GetGyroMax(i)));
            }
            else if (requestType==BTService.REQUEST_EMG_FILTER)
            {

                View layout = findViewById(R.id.spinner_layout_notch);
                layout.setVisibility(View.VISIBLE);

                spinDevice = (Spinner)findViewById(R.id.spin_notch);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"Notch Off","Notch On"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetEMGNotch(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                spinDevice = (Spinner)findViewById(R.id.spin_hpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"HPF None","HPF 3Hz","HPF 20Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetEMGHPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                spinDevice = (Spinner)findViewById(R.id.spin_lpf);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item,new String[] {"LPF None","LPF 250Hz","LPF 500Hz"});
//            adapterDevice.SetTextSize((float) (screen.standardSize_X / div));
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetEMGLPF(i));
                //spinDevice.setOnItemSelectedListener(onSelChanged);

                spinDevice = findViewById(R.id.spin_rms);
                adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, new String[] {"0.05s", "0.1s", "0.3s"});
                //RMS 설정 Spinner 0.005 = 0, 0.1 = 1, 0.3 = 2, 0.5 = 3, 1 = 4
                spinDevice.setAdapter(adapterDevice);
                spinDevice.setSelection(app.GetEMGRMS(i));

                et = findViewById(R.id.edit_range_min);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.0f",app.GetEMGMin(i)));

                et = findViewById(R.id.edit_range_max);
//            et.setTextSize((float) (screen.standardSize_X / div));
                et.setText(String.format("%.0f",app.GetEMGMax(i)));
            }
        }



        cb = findViewById(R.id.chk_x_axis);
//        cb.setTextSize((float) (screen.standardSize_X / div));
        cb.setChecked(enableAxis[0]);
        if (requestType==BTService.REQUEST_EMG_FILTER)
        {
            cb.setText(R.string.txt_check_emg_enable);
            cb.setTextColor(getResources().getColor(R.color.GraphEMG));
        }
        else if (requestType==BTService.REQUEST_Acc_FILTER)
        {
            cb.setText(R.string.txt_check_x_enable);
            cb.setTextColor(getResources().getColor(R.color.GraphAccX));
        }
        else if (requestType==BTService.REQUEST_Gyro_FILTER)
        {
            cb.setText(R.string.txt_check_x_enable);
            cb.setTextColor(getResources().getColor(R.color.GraphGyroX));
        }
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAxis[0]=isChecked;
            }
        });

        cb = findViewById(R.id.chk_y_axis);
//        cb.setTextSize((float) (screen.standardSize_X / div));
        cb.setChecked(enableAxis[1]);
        if (requestType==BTService.REQUEST_EMG_FILTER)
        {
            cb.setText(R.string.txt_check_emg_rms_enable);
            cb.setTextColor(getResources().getColor(R.color.GraphEMG));
            //cb.setVisibility(View.GONE);
        }
        else if (requestType==BTService.REQUEST_Acc_FILTER)
        {
            cb.setVisibility(View.VISIBLE);
            cb.setTextColor(getResources().getColor(R.color.GraphAccY));
        }
        else if (requestType==BTService.REQUEST_Gyro_FILTER)
        {
            cb.setVisibility(View.VISIBLE);
            cb.setTextColor(getResources().getColor(R.color.GraphGyroY));
        }
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAxis[1]=isChecked;
            }
        });

        cb = findViewById(R.id.chk_z_axis);
//        cb.setTextSize((float) (screen.standardSize_X / div));
        cb.setChecked(enableAxis[2]);
        if (requestType==BTService.REQUEST_EMG_FILTER)
        {
            cb.setVisibility(View.GONE);
        }
        else if (requestType==BTService.REQUEST_Acc_FILTER)
        {
            cb.setVisibility(View.VISIBLE);
            cb.setTextColor(getResources().getColor(R.color.GraphAccZ));
        }
        else if (requestType==BTService.REQUEST_Gyro_FILTER)
        {
            cb.setVisibility(View.VISIBLE);
            cb.setTextColor(getResources().getColor(R.color.GraphGyroZ));
        }
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAxis[2]=isChecked;
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

    public class SpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        String[] items = new String[] {};
        float txtSize = 10.0f;

        public SpinnerAdapter(final Context context,
                              final int textViewResourceId, final String[] objects) {
            super(context, textViewResourceId, objects);
            this.items = objects;
            this.context = context;
        }

        public void SetTextSize(float s)
        {
            txtSize=s;
        }

        /**
         * 스피너 클릭시 보여지는 View의 정의
         */
        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(
                        android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            TextView tv = (TextView) convertView
                    .findViewById(android.R.id.text1);
            tv.setText(items[position]);
            tv.setTextColor(getResources().getColor(R.color.PopupSpinText));
//            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,txtSize);
            return convertView;
        }

        /**
         * 기본 스피너 View 정의
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(
                        android.R.layout.simple_spinner_item  , parent, false);
            }

            TextView tv = (TextView) convertView
                    .findViewById(android.R.id.text1);
            tv.setText(items[position]);
            tv.setTextColor(getResources().getColor(R.color.PopupSpinText));
//            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,txtSize);
            return convertView;
        }
    }
}
