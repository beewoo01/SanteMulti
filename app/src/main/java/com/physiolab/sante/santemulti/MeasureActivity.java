package com.physiolab.sante.santemulti;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.PopupActivity;
import com.physiolab.sante.PopupTimeActivity;
import com.physiolab.sante.ST_DATA_PROC;
import com.physiolab.sante.SanteApp;
import com.physiolab.sante.ScreenSize;
import com.physiolab.sante.Spinner_Re_Adapter;
import com.physiolab.sante.UserInfo;
import com.physiolab.sante.dialog.DefaultDialog;
import com.physiolab.sante.santemulti.databinding.ActivityMeasureBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MeasureActivity extends AppCompatActivity implements SaveFileListener {


    private ScreenSize screen = null;

    private SanteApp[] santeApps;

    private BTService btService = null;
    private boolean isService = false;
    private MeasureFragment[] fragMeasure = null;
    private boolean isPreview = false;
    private boolean isStart = false;

    private final int[] isState = new int[]{BTService.STATE_NONE, BTService.STATE_NONE};
    private final int[] powerStatus = new int[]{BTService.POWER_BATT, BTService.POWER_BATT};

    private final float[] avgLeadoff = new float[]{0, 0};
    private final float[] thresholdLeadoff = new float[]{0, 0};

    private int cntIgnore = 0;
    private int cntWatch = 0;

    private ToneGenerator tone;
    private int beepNum = 0;

    private boolean isAlarm = true;
    private boolean isWatch = false;

    private boolean hasData = false;

    private TextView[] txtTimeMins;
    private TextView[] txtTimeMaxes;

    private TimeThread[] timeThread = new TimeThread[2];

    private static final String TAG = "Activity-Measure";
    private static final boolean D = true;

    private DefaultDialog defaultDialog;
    private GestureDetector gestureDetector = null;

    int count;


    private SoundPool sPool;

    private boolean isFirst = true;


    int handleflag = 0;// 0 - > 쓰레드 안돔 // 1 도는중 // 2 다끝나고 진행중

    private ActivityMeasureBinding binding;
    private final TextView[] txtAccMaxes = new TextView[2];
    private final TextView[] txtAccMins = new TextView[2];
    private final TextView[] txtGyroMaxes = new TextView[2];
    private final TextView[] txtGyroMins = new TextView[2];
    private final TextView[] txtEmgMaxes = new TextView[2];
    private final TextView[] txtEmgMins = new TextView[2];

    private final TextView[] txtReadOffs = new TextView[2];

    private final Spinner_Re_Adapter recordAdapter = new Spinner_Re_Adapter(new ArrayList<>());

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // 서비스가 제공하는 메소드 호출하여
            btService.SetMonitorHandler(new MessageHandler(0), 0);
            btService.SetMonitorHandler(new MessageHandler(1), 1);

            isService = true;

            isState[0] = btService.getState(0);
            isState[1] = btService.getState(1);

            if (isPreview) {
                for (int i = 0; i < isState.length; i++) {
                    if (powerStatus[i] == BTService.POWER_USB_FULL_CHARGE || powerStatus[i] == BTService.POWER_USB_CHARGING) {
                        Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        isPreview = false;
                        santeApps[i].SetPreview(isPreview, i);
                    }
                    /*if (powerStatus[i] != BTService.POWER_BATT) {
                        Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        isPreview = false;
                        santeApps[i].SetPreview(isPreview, i);
                    }*/ else if (isState[i] == BTService.STATE_CONNECTED && !isStart) {

                        cntIgnore = 25;
                        SetWatch(0);
                        fragMeasure[i].Init();
                        hasData = false;

                        SetTimeRange(i);

                        btService.SetEMGFilter(santeApps[i].GetEMGNotch(i), santeApps[i].GetEMGHPF(i), santeApps[i].GetEMGLPF(i), i);
                        btService.SetAccFilter(santeApps[i].GetAccHPF(i), santeApps[i].GetAccLPF(i), i);
                        btService.SetGyroFilter(santeApps[i].GetGyroHPF(i), santeApps[i].GetGyroLPF(i), i);


                        btService.Start(i);

                    }
                }
            }

            UpdateUI(0);
            UpdateUI(1);

        }

        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            isService = false;
            //isState = STATE_NONE;
            UpdateUI(0);
            UpdateUI(1);
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeasureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        txtTimeMins = new TextView[]{binding.txtTimeMin, binding.txtTimeMin2};
        txtTimeMaxes = new TextView[]{binding.txtTimeMax, binding.txtTimeMax2};

        txtAccMaxes[0] = binding.txtAccMax;
        txtAccMaxes[1] = binding.txtAccMax2;

        txtAccMins[0] = binding.txtAccMin;
        txtAccMins[1] = binding.txtAccMin2;

        txtGyroMaxes[0] = binding.txtGyroMax;
        txtGyroMaxes[1] = binding.txtGyroMax2;

        txtGyroMins[0] = binding.txtGyroMin;
        txtGyroMins[1] = binding.txtGyroMin2;

        txtEmgMaxes[0] = binding.txtEmgMax;
        txtEmgMaxes[1] = binding.txtEmgMax2;

        txtEmgMins[0] = binding.txtEmgMin;
        txtEmgMins[1] = binding.txtEmgMin2;

        txtReadOffs[0] = binding.txtLeadoff;
        txtReadOffs[1] = binding.txtLeadoff2;

        screen = new ScreenSize();
        screen.getStandardSize(this);


        binding.backContainer.setOnClickListener(v -> finish());
        binding.dropdownMenuBtn.setVisibility(View.VISIBLE);

        binding.recordRecyclerview.setAdapter(recordAdapter);
        binding.recordRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recordRecyclerview.setHasFixedSize(true);

        binding.btnSectionRecord.setOnClickListener(v -> {
            binding.recordBackground.setBackground(ContextCompat.getDrawable(this, R.drawable.button_08));
            binding.txtWatchSecond.setTextColor(ContextCompat.getColor(this, R.color.mainColor));
            binding.dropdownMenuBtn.setVisibility(View.VISIBLE);
            recordAdapter.addTime(binding.txtWatchSecond.getText().toString());

        });

        binding.recordBackground.setOnClickListener(v -> {
            showRecord(binding.spinnerLayoutBackground.getVisibility() == View.VISIBLE);
        });

        santeApps = new SanteApp[]{
                (SanteApp) this.getApplication(),
                (SanteApp) this.getApplication()
        };


        InitControl();

        BeepInit();

        isAlarm = santeApps[0].GetAlarm(0) && santeApps[1].GetAlarm(1);

        isPreview = santeApps[0].GetPreview(0) && santeApps[1].GetPreview(1);

        hasData = false;
        thresholdLeadoff[0] = santeApps[0].GetLeadOff(0) * 100.0f + 200.0f;
        thresholdLeadoff[1] = santeApps[1].GetLeadOff(1) * 100.0f + 200.0f;


        Intent intent = new Intent(
                MeasureActivity.this, // 현재 화면
                BTService.class); // 다음넘어갈 컴퍼넌트

        bindService(intent, // intent 객체
                conn, // 서비스와 연결에 대한 정의
                Context.BIND_AUTO_CREATE);

        Log.d(TAG, "onCreate");

    }

    private void showRecord(boolean show) {
        ViewGroup viewGroup = findViewById(R.id.spinner_layout_parent);
        LinearLayout child = findViewById(R.id.spinner_layout_background);
        Transition transition = new Slide(Gravity.TOP);
        transition.setDuration(600);
        transition.addTarget(child);
        TransitionManager.beginDelayedTransition(viewGroup, transition);
        child.setVisibility(show ? View.GONE : View.VISIBLE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.wtf("onDestroy", "onDestroy");
        if (isFinishing()) {

            if (isService) {
                boolean tmp = isPreview;
                MeasureStop();

                santeApps[0].SetPreview(tmp, 0);
                santeApps[1].SetPreview(tmp, 1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                unbindService(conn);
            }
        }
        try {
            timeThread[0].removeMessages(0);
            timeThread[1].removeMessages(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            timeThread[0].removeMessages(1);
            timeThread[1].removeMessages(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            timeThread[0].removeMessages(2);
            timeThread[1].removeMessages(2);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("DefaultLocale")
    private void UpdateUI(int device) {
        //binding.btnHome.setEnabled(true);

        binding.btnAlarm.setSelected(isAlarm);
        if (isAlarm)
            binding.btnAlarm.setBackground(ContextCompat.getDrawable(this, R.drawable.button_01));
        else
            binding.btnAlarm.setBackground(ContextCompat.getDrawable(this, R.drawable.button_02));

        binding.btnPreview.setSelected(isPreview);
        binding.btnWatchstop.setSelected(isWatch);

        txtReadOffs[device].setVisibility(View.INVISIBLE);
        /*binding.txtLeadoff.setVisibility(View.INVISIBLE);
        binding.txtLeadoff2.setVisibility(View.INVISIBLE);*/

        if (isService) {
            switch (isState[device]) {
                case BTService.STATE_NONE:
                    Log.d(TAG, "UpdateUI - not connect");
//                    devState.setBackgroundColor(getResources().getColor(R.color.DeviceStateDisconnect));

                    binding.btnStart.setEnabled(false);
                    binding.btnWatchstop.setEnabled(false);
                    binding.btnAllstop.setEnabled(false);
                    binding.btnPreview.setEnabled(true);
                    break;
                case BTService.STATE_CONNECTING:
                    Log.d(TAG, "UpdateUI - connecting");

//                    devState.setBackgroundColor(getResources().getColor(R.color.DeviceStateConnecting1));

                    binding.btnStart.setEnabled(false);
                    binding.btnWatchstop.setEnabled(false);
                    binding.btnAllstop.setEnabled(false);
                    binding.btnPreview.setEnabled(true);
                    break;

                case BTService.STATE_CONNECTED:
//                    devState.setBackgroundColor(getResources().getColor(R.color.DeviceStateConnect));
                    if (isStart) {
                        Log.d(TAG, "UpdateUI - start");

                        binding.btnStart.setEnabled(false);
                        binding.btnWatchstop.setEnabled(true);
                        binding.btnAllstop.setEnabled(true);
                        binding.btnPreview.setEnabled(false);
                        binding.btnSectionRecord.setEnabled(true);
                    } else {
                        Log.d(TAG, "UpdateUI - ready");
                        binding.btnStart.setEnabled(true);
                        binding.btnWatchstop.setEnabled(false);
                        binding.btnAllstop.setEnabled(false);
                        binding.btnPreview.setEnabled(true);
                        binding.btnSectionRecord.setEnabled(false);
                    }
                    break;
            }
        } else {
            binding.btnStart.setEnabled(false);
            binding.btnWatchstop.setEnabled(false);
            binding.btnAllstop.setEnabled(false);
            binding.btnPreview.setEnabled(true);
        }


        if (device == 0) {
            if (fragMeasure[device].GetEnable(0)) {
                binding.txtEnableAcc.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableAcc.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
            if (fragMeasure[device].GetEnable(1)) {
                binding.txtEnableGyro.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableGyro.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
            if (fragMeasure[device].GetEnable(2)) {
                binding.txtEnableEmg.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableEmg.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
        } else {
            if (fragMeasure[device].GetEnable(0)) {
                binding.txtEnableAcc2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableAcc2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
            if (fragMeasure[device].GetEnable(1)) {
                binding.txtEnableGyro2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableGyro2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
            if (fragMeasure[device].GetEnable(2)) {
                binding.txtEnableEmg2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextSel));
            } else {
                binding.txtEnableEmg2.setTextColor(
                        ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
            }
        }


        txtAccMaxes[device].setText(String.format("%.1f", fragMeasure[device].GetAccMax()));
        txtAccMins[device].setText(String.format("%.1f", fragMeasure[device].GetAccMin()));
        txtGyroMaxes[device].setText(String.format("%.1f", fragMeasure[device].GetGyroMax()));
        txtGyroMins[device].setText(String.format("%.1f", fragMeasure[device].GetGyroMin()));
        txtEmgMaxes[device].setText(String.format("%.0f", fragMeasure[device].GetEMGMax()));
        txtEmgMins[device].setText(String.format("%.0f", fragMeasure[device].GetEMGMin()));

        SetTimeRange(device);
        //SetTimeRange(1);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void SetTimeRange(int deviceNum) {

        if (!hasData) {
            txtTimeMins[deviceNum].setText("0.00");
            txtTimeMaxes[deviceNum].setText(String.format("%.2f", fragMeasure[deviceNum].GetTimeRange()));
        } else {
            txtTimeMins[deviceNum].setText(String.format("%.2f", fragMeasure[deviceNum].GetTimeStart()));
            txtTimeMaxes[deviceNum].setText(String.format("%.2f", fragMeasure[deviceNum].GetTimeStart() + fragMeasure[deviceNum].GetTimeRange()));
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void SetWatch(int cnt) {
        int hour = 0;
        int minute = 0;
        int second = 0;
        int milliSecond = 0;

        if (cnt < 0) {

            binding.txtWatchSecond.setText("00:00.00");
//            txtWatchMilliSecond.setText(".00");
            return;
        }

        milliSecond = (int) Math.floor((double) cnt / (double) BTService.SAMPLE_RATE * 100.0);
        second = (int) Math.floor((double) milliSecond / 100.0);
        milliSecond = milliSecond % 100;
        minute = (int) Math.floor((double) second / 60.0);
        second = second % 60;

        binding.txtWatchSecond.setText(String.format("%02d:%02d.%02d", minute, second, milliSecond));
//        txtWatchMilliSecond.setText(String.format("",));
    }

    private void MeasureStop() {
        if (isStart | isPreview) {
            btService.Stop(0);
            btService.Stop(1);
        }


        if (isStart) {

            if (handleflag == 2 || !isAlarm) {
                Log.wtf("MeasureStop", "andleflag == 2 || !isAlarm");
                defaultDialog = new DefaultDialog(this, () -> {
                    UserInfo.getInstance().watchCnt = cntWatch;
                    UserInfo.getInstance().spacial = binding.testNameEdt.getText().toString();
                    fragMeasure[0].SaveData("ch1",
                            MeasureActivity.this, recordAdapter.getItems(),
                            santeApps[0]);
                }, "알림", "측정결과를 저장하시겠습니까?");
                defaultDialog.show();
            }
        }

        isStart = false;
        isPreview = false;
        isFirst = true;
        santeApps[0].SetPreview(isPreview, 0);
        santeApps[1].SetPreview(isPreview, 1);
        UpdateUI(0);
        UpdateUI(1);
    }

    @Override
    public void onSuccess(int device) {

        if (device == 1) {
            Toast.makeText(this, "파일 저장에 성공하였습니다.", Toast.LENGTH_SHORT).show();
        } else {
            //fragMeasure[1].SaveData("left", MeasureActivity.this, recordAdapter.getItems());
            fragMeasure[1].SaveData("ch2", MeasureActivity.this,
                    recordAdapter.getItems(), santeApps[1]);
        }
    }

    @Override
    public void onFail() {
        Toast.makeText(this, "파일 저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
    }


    private class MessageHandler extends Handler {
        private int deviceIndex = -1;

        public MessageHandler(int index) {
            super();
            deviceIndex = index;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case BTService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            if (D) Log.d(TAG, "Device Connect");
                            isState[deviceIndex] = BTService.STATE_CONNECTED;
                            //isMeasure[deviceIndex] = false;
                            break;
                        case BTService.STATE_CONNECTING:

                            isState[deviceIndex] = BTService.STATE_CONNECTING;
                            if (isStart | isPreview) {
                                Log.wtf("BTService.STATE_CONNECTING", "isStart | isPreview");
                                MeasureStop();
                            }
                            break;
                        case BTService.STATE_NONE:
                            isState[deviceIndex] = BTService.STATE_NONE;
                            if (D) Log.d(TAG, "Device Close");
                            if (isStart | isPreview) {
                                Log.wtf("BTService.STATE_NONE", "BTService.isStart | isPreview");
                                MeasureStop();
                            }
                            break;
                    }
                    //StopSave(deviceIndex);
                    UpdateUI(deviceIndex);
                    break;
                case BTService.MESSAGE_DEVICE_INFO:
                    //battLevel[deviceIndex] = ((float)msg.arg1)/1000.0f;

                    if (msg.arg2 == 1) //배터리 전원 사용
                    {
                        //isUSBPower = false;
                        //powerStatus[deviceIndex] = BTService.POWER_BATT;
                        //Log.wtf("MESSAGE_DEVICE_INFO", "배터리 전원 사용");
                        powerStatus[deviceIndex] = BTService.POWER_BATT;
                    } else if (msg.arg2 == 2)   //USB 전원사용 - 완충됨
                    {
                        //isUSBPower = true;
                        //Log.wtf("MESSAGE_DEVICE_INFO", "USB 전원사용 - 완충됨");
                        powerStatus[deviceIndex] = BTService.POWER_USB_FULL_CHARGE;
                    } else if (msg.arg2 == 3)   //USB 전원사용 - 충전중
                    {
                        //isUSBPower = true;
                        //Log.wtf("MESSAGE_DEVICE_INFO", "USB 전원사용 - 충전중");
                        powerStatus[deviceIndex] = BTService.POWER_USB_CHARGING;
                        //powerStatus[deviceIndex] = POWER_USB_CHARGING;
                    }
                    //battUpdateCnt[deviceIndex]--;
                    /*if (battUpdateCnt[deviceIndex]<=0) {
                        UpdateUI();
                        battUpdateCnt[deviceIndex]=3;
                    }*/

                    break;

                case BTService.MESSAGE_DATA_RECEIVE:

                    while (true) {
                        ST_DATA_PROC data = new ST_DATA_PROC();
                        byte ret = btService.GetData(data, deviceIndex);
                        if (ret != 1) {
                            break;
                        } else {
                            if (cntIgnore > 0) cntIgnore--;
                            else {


                                avgLeadoff[deviceIndex] = 0;
                                for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
                                    avgLeadoff[deviceIndex] += data.BPF_DC[i];
                                }
                                avgLeadoff[deviceIndex] /= (float) BTService.PACKET_SAMPLE_NUM;
                                if (avgLeadoff[deviceIndex] > thresholdLeadoff[deviceIndex] && (isPreview || isStart)
                                        && fragMeasure[deviceIndex].GetEnable(2)) {
                                    txtReadOffs[deviceIndex].setVisibility(View.VISIBLE);
                                    UserInfo.getInstance().leadoff = true;

                                } else {
                                    txtReadOffs[deviceIndex].setVisibility(View.INVISIBLE);
                                }

                                if (isFirst) {
                                    //Log.wtf("isFirst", String.valueOf(isFirst));
                                    long time = System.currentTimeMillis();
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSSZ");
                                    String firstDataTime = sdf.format(time);
                                    fragMeasure[deviceIndex].SetFirstDataTime(firstDataTime);

                                    isFirst = false;
                                }
                                if (!fragMeasure[deviceIndex].Add(data)) {
                                    Log.wtf("MESSAGE_DATA_RECEIVE", "!fragMeasure[deviceIndex].Add(data)");
                                    MeasureStop();
                                }

                                if (isStart && (isWatch || cntWatch <= 0)) {
//                                    if (cntWatch == -200 && isAlarm) BeepPlay();
//                                    else if (cntWatch == 0 && !isAlarm) BeepPlay();
                                    cntWatch += 40;

                                    //여기 수정한곳
                                    if (deviceIndex == 1) {
                                        SetWatch(cntWatch);
                                    }

                                    SetTimeRange(deviceIndex);
                                }

                                if (powerStatus[deviceIndex] == BTService.POWER_USB_FULL_CHARGE || powerStatus[deviceIndex] == BTService.POWER_USB_CHARGING) {
                                    Toast.makeText(santeApps[deviceIndex], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();

                                    Log.wtf("MESSAGE_DATA_RECEIVE", "USB연결중에는");
                                    MeasureStop();
                                }
                                /*if (powerStatus[deviceIndex] != BTService.POWER_BATT) {

                                }*/

                            }

                        }
                    }

                    break;

                case BTService.MESSAGE_COMMAND_RECEIVE:
                    switch (msg.arg1) {
                        case BTService.CMD_BLU_DEV_INITIALIZE:
                            if (D) Log.d(TAG, "Received Initialize COMMAND");
                            break;
                        case BTService.CMD_BLU_D2P_DATA_REALTIME_START:
                            if (D) Log.d(TAG, "Received Data Transfer Start COMMAND");
                            //isMeasure[deviceIndex] = true;
                            //UpdateUI();
                            break;
                        case BTService.CMD_BLU_D2P_DATA_STOP:
                            if (D) Log.d(TAG, "Received Data Transfer Stop COMMAND");
                            //StopSave(deviceIndex);
                            //isMeasure[deviceIndex] = false;
                            //UpdateUI();
                            break;
                        case BTService.CMD_BLU_COMM_START:
                            if (D) Log.d(TAG, "Received Communication Start COMMAND");
                            break;
                    }
                    break;

                case BTService.MESSAGE_DATA_OVERFLOW:
                    if (msg.arg1 == 0) {
                        if (D) Log.d(TAG, "Device Queue OverFlow");
                    } else {
                        if (D) Log.d(TAG, "Receive Queue OverFlow");
                    }
                    //StopSave(deviceIndex);
                    if (btService != null) btService.Close(deviceIndex);
                    break;

            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private void InitControl() {
        fragMeasure = new MeasureFragment[]{
                (MeasureFragment) getSupportFragmentManager().findFragmentById(R.id.frag_graph_measure),
                (MeasureFragment) getSupportFragmentManager().findFragmentById(R.id.frag_graph_measure2)
        };

        for (int i = 0; i < fragMeasure.length; i++) {
            fragMeasure[i].SetAccRange(santeApps[i].GetAccMax(i), santeApps[i].GetAccMin(i));
            fragMeasure[i].SetGyroRange(santeApps[i].GetGyroMax(i), santeApps[i].GetGyroMin(i));
            fragMeasure[i].SetEMGRange(santeApps[i].GetEMGMax(i), santeApps[i].GetEMGMin(i));
            fragMeasure[i].SetTimeRange(santeApps[i].GetTimeRange(i));
        }


        MeasureView[] mViews = new MeasureView[]{(MeasureView) fragMeasure[0].getView(), (MeasureView) fragMeasure[1].getView()};
        for (MeasureView mView : mViews) {
            mView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }

        gestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });

        binding.btnStart.setSelected(true);

        binding.btnStart.setOnClickListener(v -> { /// 측정시작

            UserInfo.getInstance().measureTime = new Date(System.currentTimeMillis());
            timeThread[0] = new TimeThread(0);
            timeThread[1] = new TimeThread(1);
            if (powerStatus[0] == BTService.POWER_USB_FULL_CHARGE || powerStatus[0] == BTService.POWER_USB_CHARGING
                    || powerStatus[1] == BTService.POWER_USB_FULL_CHARGE || powerStatus[1] == BTService.POWER_USB_CHARGING) {

                Toast.makeText(this, "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();

            }else if (isState[0] == BTService.STATE_CONNECTED && isState[1] == BTService.STATE_CONNECTED) {
                if (isPreview && !isStart) {
                    btService.Stop(0);
                    btService.Stop(1);
                }

                isPreview = false;

                isStart = true;

                hasData = true;

                if (isAlarm) cntWatch = BTService.SAMPLE_RATE * -3;
                else cntWatch = 0;


                isWatch = true;

                SetWatch(cntWatch);
                fragMeasure[0].Init();
                fragMeasure[1].Init();
                SetTimeRange(0);
                SetTimeRange(1);

                long now = System.currentTimeMillis();
                UserInfo.getInstance().measureTime = new Date(now);
                UserInfo.getInstance().alarm = isAlarm;
                UserInfo.getInstance().watchCnt = 0;

                cntIgnore = 25;

                btService.SetEMGFilter(santeApps[0].GetEMGNotch(0), santeApps[0].GetEMGHPF(0), santeApps[0].GetEMGLPF(0), 0);
                btService.SetEMGFilter(santeApps[1].GetEMGNotch(1), santeApps[1].GetEMGHPF(1), santeApps[1].GetEMGLPF(1), 1);
                btService.SetAccFilter(santeApps[0].GetAccHPF(0), santeApps[0].GetAccLPF(0), 0);
                btService.SetAccFilter(santeApps[1].GetAccHPF(1), santeApps[1].GetAccLPF(1), 1);
                btService.SetGyroFilter(santeApps[0].GetGyroHPF(0), santeApps[0].GetGyroLPF(0), 0);
                btService.SetGyroFilter(santeApps[1].GetGyroHPF(1), santeApps[1].GetGyroLPF(1), 1);

                btService.Start(0);
                btService.Start(1);

                if (isAlarm) {
                    timeThread[0] = new TimeThread(0);
                    timeThread[1] = new TimeThread(1);
                    handleflag = 0;
                    timeThread[0].sendEmptyMessage(0);
                    timeThread[1].sendEmptyMessage(0);

                }else {
                    BeepPlay();
                }
                UpdateUI(0);
                UpdateUI(1);
            }

            /*for (int i = 0; i < powerStatus.length; i++) {
                timeThread[i] = new TimeThread(i);
                if (powerStatus[i] == BTService.POWER_USB_FULL_CHARGE || powerStatus[i] == BTService.POWER_USB_CHARGING) {
                    Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
                *//*if (powerStatus[i] != BTService.POWER_BATT) {
                    Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }*//*
                else if (isState[i] == BTService.STATE_CONNECTED) {
                    if (isPreview && !isStart) {
                        btService.Stop(i);
                    }
                    isPreview = false;

                    isStart = true;

                    hasData = true;

                    if (isAlarm) cntWatch = BTService.SAMPLE_RATE * -3;
                    else cntWatch = 0;

                    isWatch = true;
                    SetWatch(cntWatch);
                    fragMeasure[i].Init();
                    SetTimeRange(i);

                    long now = System.currentTimeMillis();
                    UserInfo.getInstance().measureTime = new Date(now);
                    UserInfo.getInstance().alarm = isAlarm;
                    UserInfo.getInstance().watchCnt = 0;


                    cntIgnore = 25;

                    btService.SetEMGFilter(santeApps[i].GetEMGNotch(i), santeApps[i].GetEMGHPF(i), santeApps[i].GetEMGLPF(i), i);
                    btService.SetAccFilter(santeApps[i].GetAccHPF(i), santeApps[i].GetAccLPF(i), i);
                    btService.SetGyroFilter(santeApps[i].GetGyroHPF(i), santeApps[i].GetGyroLPF(i), i);

                    btService.Start(i);

                    if (isAlarm) {
                        timeThread[i] = new TimeThread(i);
                        handleflag = 0;
                        timeThread[i].sendEmptyMessage(0);

                    }

                }
                UpdateUI(i);
            }*/

        });


        binding.btnWatchstop.setSelected(isWatch);
        binding.btnWatchstop.setOnClickListener(v -> {

            isWatch = !isWatch;
            UpdateUI(0);
            UpdateUI(1);

        });


        binding.btnAllstop.setSelected(true);
        binding.btnAllstop.setOnClickListener(v -> {
            Log.wtf("btnAllstop", "btnAllstop");
            MeasureStop();

        });
        //btnAlarm = (Button) findViewById(R.id.btn_alarm);
        binding.btnAlarm.setSelected(isAlarm);
        binding.btnAlarm.setOnClickListener(v -> {
            isAlarm = !isAlarm;
            santeApps[0].SetAlarm(isAlarm, 0);
            santeApps[1].SetAlarm(isAlarm, 1);
            UpdateUI(0);
            UpdateUI(1);
        });
        //btnPreview = (Button) findViewById(R.id.btn_preview);

        binding.btnPreview.setSelected(isPreview);
        binding.btnPreview.setOnClickListener(v -> {
            isPreview = !isPreview;
            santeApps[0].SetPreview(isPreview, 0);
            santeApps[1].SetPreview(isPreview, 1);


            for (int i = 0; i < powerStatus.length; i++) {
                if (isPreview) {

                    if (powerStatus[i] == BTService.POWER_USB_FULL_CHARGE || powerStatus[i] == BTService.POWER_USB_CHARGING) {
                        Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        isPreview = false;
                        santeApps[i].SetPreview(isPreview, i);
                    }
                    /*if (powerStatus[i] != BTService.POWER_BATT) {
                        Toast.makeText(santeApps[i], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        isPreview = false;
                        santeApps[i].SetPreview(isPreview, i);
                    }*/
                    else if (isState[i] == BTService.STATE_CONNECTED && !isStart) {
                        cntIgnore = 25;
                        SetWatch(0);
                        fragMeasure[i].Init();

                        hasData = false;

                        SetTimeRange(i);

                        /*btService.SetEMGFilter(santeApps[i].GetEMGNotch(i), santeApps[i].GetEMGHPF(i), santeApps[i].GetEMGLPF(i), 0);
                        btService.SetAccFilter(santeApps[i].GetAccHPF(i), santeApps[i].GetAccLPF(i), 0);
                        btService.SetGyroFilter(santeApps[i].GetGyroHPF(i), santeApps[i].GetGyroLPF(i), 0);*/

                        //btService.Start(0);

                        btService.SetEMGFilter(santeApps[i].GetEMGNotch(i), santeApps[i].GetEMGHPF(i), santeApps[i].GetEMGLPF(i), i);
                        btService.SetAccFilter(santeApps[i].GetAccHPF(i), santeApps[i].GetAccLPF(i), i);
                        btService.SetGyroFilter(santeApps[i].GetGyroHPF(i), santeApps[i].GetGyroLPF(i), i);
                        btService.Start(i);
                    }
                } else {
                    if (isState[i] == BTService.STATE_CONNECTED && !isStart) {
                        btService.Stop(i);
                    }
                }
                UpdateUI(i);
            }

            /*UpdateUI(0);
            UpdateUI(1);*/
        });


        binding.btnAccAxis.setOnClickListener(v -> {
            boolean[] enableAxis = new boolean[3];
            fragMeasure[0].GetEnable(0, enableAxis);
            fragMeasure[1].GetEnable(0, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Acc_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_Acc_FILTER);
        });

        //btnGyroAxis = (Button) findViewById(R.id.btn_gyro_axis);
        binding.btnGyroAxis.setOnClickListener(v -> {
            boolean[] enableAxis = new boolean[3];
            fragMeasure[0].GetEnable(1, enableAxis);
            fragMeasure[1].GetEnable(1, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Gyro_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_Gyro_FILTER);
        });
        //btnEMGAxis = (Button) findViewById(R.id.btn_emg_axis);

        binding.btnEmgAxis.setOnClickListener(v -> {
            boolean[] enableAxis = new boolean[3];
            fragMeasure[0].GetEnable(2, enableAxis);
            fragMeasure[1].GetEnable(2, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_EMG_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_EMG_FILTER);
        });

//        div = (float)getResources().getInteger(R.integer.enable_text_size);

        //txtEnableEMG = (TextView) findViewById(R.id.txt_enable_emg);

        binding.txtEnableEmg.setOnClickListener(v -> {
            fragMeasure[0].ToggleEnable(2);
            UpdateUI(0);
        });

        //txtEnableAcc = (TextView) findViewById(R.id.txt_enable_acc);

        binding.txtEnableAcc.setOnClickListener(v -> {
            fragMeasure[0].ToggleEnable(0);
            UpdateUI(0);
        });

        //txtEnableGyro = (TextView) findViewById(R.id.txt_enable_gyro);
        binding.txtEnableGyro.setOnClickListener(v -> {
            fragMeasure[0].ToggleEnable(1);
            UpdateUI(0);
        });


        binding.txtEnableEmg2.setOnClickListener(v -> {
            fragMeasure[1].ToggleEnable(2);
            UpdateUI(1);
        });

        //txtEnableAcc = (TextView) findViewById(R.id.txt_enable_acc);

        binding.txtEnableAcc2.setOnClickListener(v -> {
            fragMeasure[1].ToggleEnable(0);
            UpdateUI(1);
        });

        //txtEnableGyro = (TextView) findViewById(R.id.txt_enable_gyro);
        binding.txtEnableGyro2.setOnClickListener(v -> {
            fragMeasure[1].ToggleEnable(1);
            UpdateUI(1);
        });


        /*txtAccMax = (TextView) findViewById(R.id.txt_acc_max);
        txtAccMin = (TextView) findViewById(R.id.txt_acc_min);
        txtGyroMax = (TextView) findViewById(R.id.txt_gyro_max);
        txtGyroMin = (TextView) findViewById(R.id.txt_gyro_min);
        txtEMGMax = (TextView) findViewById(R.id.txt_emg_max);
        txtEMGMin = (TextView) findViewById(R.id.txt_emg_min);
        txtTimeMax = (TextView) findViewById(R.id.txt_time_max);
        txtTimeMin = (TextView) findViewById(R.id.txt_time_min);*/

        SetTimeRange(0);
        SetTimeRange(1);


        //txtDefaultRange = (TextView) findViewById(R.id.txt_default_range);

        binding.txtDefaultRange.setOnClickListener(v -> {
            santeApps[0].SetAccMax(santeApps[0].GetDefaultAccMax(), 0);
            santeApps[0].SetAccMin(santeApps[0].GetDefaultAccMin(), 0);

            santeApps[0].SetGyroMax(santeApps[0].GetDefaultGyroMax(), 0);
            santeApps[0].SetGyroMin(santeApps[0].GetDefaultGyroMin(), 0);

            santeApps[0].SetEMGMax(santeApps[0].GetDefaultEMGMax(), 0);
            santeApps[0].SetEMGMin(santeApps[0].GetDefaultEMGMin(), 0);

            fragMeasure[0].SetAccRange(santeApps[0].GetAccMax(0), santeApps[0].GetAccMin(0));
            fragMeasure[0].SetGyroRange(santeApps[0].GetGyroMax(0), santeApps[0].GetGyroMin(0));
            fragMeasure[0].SetEMGRange(santeApps[0].GetEMGMax(0), santeApps[0].GetEMGMin(0));

            UpdateUI(0);
        });

        binding.txtDefaultRange2.setOnClickListener(v -> {
            santeApps[1].SetAccMax(santeApps[1].GetDefaultAccMax(), 1);
            santeApps[1].SetAccMin(santeApps[1].GetDefaultAccMin(), 1);

            santeApps[1].SetGyroMax(santeApps[1].GetDefaultGyroMax(), 1);
            santeApps[1].SetGyroMin(santeApps[1].GetDefaultGyroMin(), 1);

            santeApps[1].SetEMGMax(santeApps[1].GetDefaultEMGMax(), 1);
            santeApps[1].SetEMGMin(santeApps[1].GetDefaultEMGMin(), 1);

            fragMeasure[1].SetAccRange(santeApps[1].GetAccMax(1), santeApps[1].GetAccMin(1));
            fragMeasure[1].SetGyroRange(santeApps[1].GetGyroMax(1), santeApps[1].GetGyroMin(1));
            fragMeasure[1].SetEMGRange(santeApps[1].GetEMGMax(1), santeApps[1].GetEMGMin(1));

            UpdateUI(1);
        });

        //txtFullRange = findViewById(R.id.txt_full_range);
        binding.txtFullRange.setOnClickListener(v -> {
            santeApps[0].SetAccMax(santeApps[0].GetFullAccMax(), 0);
            santeApps[0].SetAccMin(santeApps[0].GetFullAccMin(), 0);

            santeApps[0].SetGyroMax(santeApps[0].GetFullGyroMax(), 0);
            santeApps[0].SetGyroMin(santeApps[0].GetFullGyroMin(), 0);

            santeApps[0].SetEMGMax(santeApps[0].GetFullEMGMax(), 0);
            santeApps[0].SetEMGMin(santeApps[0].GetFullEMGMin(), 0);

            fragMeasure[0].SetAccRange(santeApps[0].GetAccMax(0), santeApps[0].GetAccMin(0));
            fragMeasure[0].SetGyroRange(santeApps[0].GetGyroMax(0), santeApps[0].GetGyroMin(0));
            fragMeasure[0].SetEMGRange(santeApps[0].GetEMGMax(0), santeApps[0].GetEMGMin(0));

            UpdateUI(0);
        });


        binding.txtFullRange2.setOnClickListener(v -> {
            santeApps[1].SetAccMax(santeApps[1].GetFullAccMax(), 1);
            santeApps[1].SetAccMin(santeApps[1].GetFullAccMin(), 1);

            santeApps[1].SetGyroMax(santeApps[1].GetFullGyroMax(), 1);
            santeApps[1].SetGyroMin(santeApps[1].GetFullGyroMin(), 1);

            santeApps[1].SetEMGMax(santeApps[1].GetFullEMGMax(), 1);
            santeApps[1].SetEMGMin(santeApps[1].GetFullEMGMin(), 1);

            fragMeasure[1].SetAccRange(santeApps[1].GetAccMax(1), santeApps[1].GetAccMin(1));
            fragMeasure[1].SetGyroRange(santeApps[1].GetGyroMax(1), santeApps[1].GetGyroMin(1));
            fragMeasure[1].SetEMGRange(santeApps[1].GetEMGMax(1), santeApps[1].GetEMGMin(1));

            UpdateUI(1);
        });


        //txtTimeLabel = (TextView) findViewById(R.id.txt_time_label);
        binding.txtTimeLabel.setOnClickListener(v -> {
            Intent intent = new Intent(MeasureActivity.this, PopupTimeActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Time_RANGE);
            intent.putExtra("deviceLength", 2);
            startActivityForResult(intent, BTService.REQUEST_Time_RANGE);
        });

        binding.txtTimeLabel2.setOnClickListener(v -> {
            Intent intent = new Intent(MeasureActivity.this, PopupTimeActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Time_RANGE);
            intent.putExtra("deviceLength", 2);
            startActivityForResult(intent, BTService.REQUEST_Time_RANGE);
        });


        binding.btnPrevPage.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[0].PrevPage();

            SetTimeRange(0);
        });

        binding.btnPrevPage2.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[1].PrevPage();

            SetTimeRange(1);
        });

        binding.btnPrevSec.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[0].PrevSec();

            SetTimeRange(0);
        });

        binding.btnPrevSec2.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[1].PrevSec();

            SetTimeRange(1);
        });

        binding.btnNextSec.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[0].NextSec();

            SetTimeRange(0);
        });

        binding.btnNextSec2.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[1].NextSec();

            SetTimeRange(1);
        });

        binding.btnNextPage.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[0].NextPage();

            SetTimeRange(0);
        });

        binding.btnNextPage2.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure[1].NextPage();

            SetTimeRange(1);
        });
    }


    private final View.OnClickListener defaultDialogclose = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            /*progressDialog = new ProgressDialog(MeasureActivity.this);
              progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
              progressDialog.setIndeterminate(true);
              progressDialog.setCancelable(false);*/

            defaultDialog.dismiss();
            UserInfo.getInstance().watchCnt = cntWatch;
            UserInfo.getInstance().memo = binding.testNameEdt.getText().toString();
            fragMeasure[0].SaveData("ch1", MeasureActivity.this,
                    recordAdapter.getItems(), santeApps[0]);


            Log.wtf("defaultDialogclose", "1111111111111");

            /*progressDialog.show();
              progressDialog.setContentView(R.layout.progress);

              measureInfo.watchCnt = cntWatch;
              Log.wtf("defaultDialogclose", "22222222222222");


                UserInfo.getInstance().direction_of_wear = "right";
                fragMeasure[0].SaveData(this, "right", measureInfo, mContext, recordAdapter.getItems(), callbackFromMeasureView, false);*/

            /*fragMeasure[0].SaveData(this, 0, measureInfo, mContext, recordAdapter.getItems(), progressDialog, callbackFromMeasureView);
            new Runnable() {
                @Override
                public void run() {

                    fragMeasure[0].SaveData(this, 0, measureInfo, mContext, recordAdapter.getItems(), progressDialog);
                }
            };
            new Runnable() {
                @Override
                public void run() {
                    fragMeasure[1].SaveData(this, 1, measureInfo, mContext, recordAdapter.getItems(), progressDialog);
                }
            };*/


        }
    };

    private Handler timerHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }

    };



    //타이머 핸들러 김인호
    @SuppressLint("HandlerLeak")
    class TimeThread extends Handler {

        private int deviceNum = -1;
        int ope_cnt = 3;

        public TimeThread(int deviceNum) {
            this.deviceNum = deviceNum;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            Log.wtf("ope_cnt", String.valueOf(ope_cnt));
            if (msg.what == 0) {
                try {//타이머 진행
                    if (handleflag == 0) {
                        handleflag = 1;
                        ope_cnt = 3;
                        Log.wtf("쓰레드", "시작");

                    }

                    count = fragMeasure[deviceNum].getConunt();
                    Log.wtf("count", count + "");
                    float[][] gyro = fragMeasure[deviceNum].getGyroData();
                    float sum = 0;
                    for (int i = 0; i < count; i++) {
                        if (gyro[0][i] > 0)
                            sum += gyro[0][i];
                        else
                            sum -= gyro[0][i];

                        if (gyro[1][i] > 0)
                            sum += gyro[1][i];
                        else
                            sum -= gyro[1][i];

                        if (gyro[2][i] > 0)
                            sum += gyro[2][i];
                        else
                            sum -= gyro[2][i];

                    }
                    Log.wtf("쓰레드", "돈다");

                    sum = sum / 3 / count;
                    if (sum > 4) {
                        Log.wtf("sum > 4", "sum > 4");
                        MeasureStop();
                        fragMeasure[0].setGyroData(count);
                        fragMeasure[1].setGyroData(count);

                        Toast toast = Toast.makeText(getApplicationContext(), "message", Toast.LENGTH_SHORT);

                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);

                        toast.setText("소리가 나기 전 움직임이 포착되었습니다.\n계속 움직임이 포착되면 주의를 주세요.");
                        toast.show();

//                        Toast.makeText(app, "소리가 나기 전 움직임이 포착되었습니다. 계속 움직임이 포착되면 주의를 주세요.", Toast.LENGTH_SHORT).show();
                        sendEmptyMessageDelayed(1, 0);

                    } else if (ope_cnt == 0) {
                        handleflag = 2;

                        sendEmptyMessageDelayed(2, 0);
                    } else {
                        ope_cnt = ope_cnt - 1;

                        sendEmptyMessageDelayed(0, 1000);
                    }

                } catch (Exception e) {
                    Log.wtf("why log : ", e);
                }
            } else if (msg.what == 1) {// 움직여서 종료
                Log.wtf("쓰레드", "111정지");

                if (powerStatus[0] != BTService.POWER_BATT || powerStatus[1] != BTService.POWER_BATT) {

                    Toast.makeText(santeApps[deviceNum],
                            "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();

                } else if (isState[0] == BTService.STATE_CONNECTED && isState[1] == BTService.STATE_CONNECTED) {
                    if (isPreview && !isStart) {
                        btService.Stop(0);
                        btService.Stop(1);
                    }
                    isPreview = false;

                    isStart = true;

                    hasData = true;

                    if (isAlarm) cntWatch = BTService.SAMPLE_RATE * -3;
                    else cntWatch = 0;

                    isWatch = true;

                    if (deviceNum == 1) {
                        SetWatch(cntWatch);
                    }

                    /*fragMeasure[deviceNum].Init();
                    SetTimeRange(deviceNum);*/
                    Log.wtf("msg.what == 1", "fragMeasure.Init()");
                    fragMeasure[0].Init();
                    fragMeasure[1].Init();
                    SetTimeRange(0);
                    SetTimeRange(1);

                    long now = System.currentTimeMillis();
                    UserInfo.getInstance().measureTime = new Date(now);
                    UserInfo.getInstance().alarm = isAlarm;
                    UserInfo.getInstance().watchCnt = 0;


                    cntIgnore = 25;


                    /*btService.SetEMGFilter(santeApps[deviceNum].GetEMGNotch(deviceNum), santeApps[deviceNum].GetEMGHPF(deviceNum), santeApps[deviceNum].GetEMGLPF(deviceNum), deviceNum);
                    btService.SetAccFilter(santeApps[deviceNum].GetAccHPF(deviceNum), santeApps[deviceNum].GetAccLPF(deviceNum), deviceNum);
                    btService.SetGyroFilter(santeApps[deviceNum].GetGyroHPF(deviceNum), santeApps[deviceNum].GetGyroLPF(deviceNum), deviceNum);*/

                    btService.SetEMGFilter(santeApps[0].GetEMGNotch(0), santeApps[0].GetEMGHPF(0), santeApps[0].GetEMGLPF(0), 0);
                    btService.SetEMGFilter(santeApps[1].GetEMGNotch(1), santeApps[1].GetEMGHPF(1), santeApps[1].GetEMGLPF(1), 1);
                    btService.SetAccFilter(santeApps[0].GetAccHPF(0), santeApps[0].GetAccLPF(0), 0);
                    btService.SetAccFilter(santeApps[1].GetAccHPF(1), santeApps[1].GetAccLPF(1), 1);
                    btService.SetGyroFilter(santeApps[0].GetGyroHPF(0), santeApps[0].GetGyroLPF(0), 0);
                    btService.SetGyroFilter(santeApps[1].GetGyroHPF(1), santeApps[1].GetGyroLPF(1), 1);

                    btService.Start(0);
                    btService.Start(1);

                }

                UpdateUI(0);
                UpdateUI(1);

                handleflag = 0;
                ope_cnt = 3;
                sendEmptyMessageDelayed(0, 1000);

               /* if (powerStatus[deviceNum] != BTService.POWER_BATT) {
                    Toast.makeText(santeApps[deviceNum], "USB연결중에는 측정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (isState[deviceNum] == BTService.STATE_CONNECTED) {
                    if (isPreview && !isStart) {
                        btService.Stop(deviceNum);
                    }
                    isPreview = false;

                    isStart = true;

                    hasData = true;

                    if (isAlarm) cntWatch = BTService.SAMPLE_RATE * -3;
                    else cntWatch = 0;

                    isWatch = true;
                    SetWatch(cntWatch);
                    fragMeasure[deviceNum].Init();
                    SetTimeRange(deviceNum);

                    long now = System.currentTimeMillis();
                    UserInfo.getInstance().measureTime = new Date(now);
                    UserInfo.getInstance().alarm = isAlarm;
                    UserInfo.getInstance().watchCnt = 0;


                    cntIgnore = 25;

                    btService.SetEMGFilter(santeApps[deviceNum].GetEMGNotch(deviceNum), santeApps[deviceNum].GetEMGHPF(deviceNum), santeApps[deviceNum].GetEMGLPF(deviceNum), deviceNum);
                    btService.SetAccFilter(santeApps[deviceNum].GetAccHPF(deviceNum), santeApps[deviceNum].GetAccLPF(deviceNum), deviceNum);
                    btService.SetGyroFilter(santeApps[deviceNum].GetGyroHPF(deviceNum), santeApps[deviceNum].GetGyroLPF(deviceNum), deviceNum);

                    btService.Start(0);

                }

                UpdateUI(deviceNum);

                handleflag = 0;
                ope_cnt = 3;
                sendEmptyMessageDelayed(0, 1000);*/

            } else if (msg.what == 2) {//정상 진행 종료
                Log.wtf("쓰레드", "2222정지");
                if (deviceNum == 0){
                    BeepPlay();
                }

                removeMessages(0);

            }

        }
    }


    private void BeepInit() {
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);

        sPool = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);
        beepNum = sPool.load(getApplicationContext(), R.raw.beep, 1);
    }

    private void BeepPlay() {
        tone.startTone(ToneGenerator.TONE_DTMF_S, 100);

        sPool.play(beepNum, 1f, 1f, 0, 0, 1f);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == BTService.RESULT_CANCLE) return;

        if (requestCode == BTService.REQUEST_EMG_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetEMGFilter(app.GetEMGNotch(), app.GetEMGHPF(), app.GetEMGLPF());
            fragMeasure[0].SetEMGRange(santeApps[0].GetEMGMax(0), santeApps[0].GetEMGMin(0));
            fragMeasure[1].SetEMGRange(santeApps[1].GetEMGMax(1), santeApps[1].GetEMGMin(1));
            fragMeasure[0].SetEnable(2, enableAxis[0], enableAxis[1], enableAxis[2]);
            fragMeasure[1].SetEnable(2, enableAxis[0], enableAxis[1], enableAxis[2]);
        } else if (requestCode == BTService.REQUEST_Acc_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetAccFilter(app.GetAccHPF(), app.GetAccLPF());
            fragMeasure[0].SetAccRange(santeApps[0].GetAccMax(0), santeApps[0].GetAccMin(0));
            fragMeasure[1].SetAccRange(santeApps[1].GetAccMax(1), santeApps[1].GetAccMin(1));
            fragMeasure[0].SetEnable(0, enableAxis[0], enableAxis[1], enableAxis[2]);
            fragMeasure[1].SetEnable(0, enableAxis[0], enableAxis[1], enableAxis[2]);
        } else if (requestCode == BTService.REQUEST_Gyro_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetGyroFilter(app.GetGyroHPF(), app.GetGyroLPF());
            fragMeasure[0].SetGyroRange(santeApps[0].GetGyroMax(0), santeApps[0].GetGyroMin(0));
            fragMeasure[1].SetGyroRange(santeApps[1].GetGyroMax(1), santeApps[1].GetGyroMin(1));
            fragMeasure[0].SetEnable(1, enableAxis[0], enableAxis[1], enableAxis[2]);
            fragMeasure[1].SetEnable(1, enableAxis[0], enableAxis[1], enableAxis[2]);

        } else if (requestCode == BTService.REQUEST_Time_RANGE) {
            fragMeasure[0].SetTimeRange(santeApps[0].GetTimeRange(0));
            fragMeasure[1].SetTimeRange(santeApps[1].GetTimeRange(1));
        }


        UpdateUI(0);
        UpdateUI(1);
    }
}