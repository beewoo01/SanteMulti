package com.physiolab.sante.santemulti;

import static com.physiolab.sante.BlueToothService.BTService.SAMPLE_RATE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
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
import com.physiolab.sante.dialog.SaveDialogDialog;
import com.physiolab.sante.listener.SaveFileDialogListener;
import com.physiolab.sante.santemulti.databinding.ActivityMeasureOneBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Measure1chActivity extends AppCompatActivity implements SaveFileListener {

    private SanteApp santeApps;

    private BTService btService = null;
    private boolean isService = false;
    private MeasureFragment fragMeasure = null;
    private boolean isPreview = false;
    private boolean isStart = false;

    private int isState = BTService.STATE_NONE;
    private int powerStatus = BTService.POWER_NONE;

    private float avgLeadoff = 0;
    private float thresholdLeadoff = 0;

    private int cntIgnore = 0;
    private int cntWatch = 0;

    private ToneGenerator tone;
    private int beepNum = 0;

    private boolean isAlarm = true;
    private boolean isWatch = false;

    private boolean hasData = false;


    private TimeThread timeThread;
    private GestureDetector gestureDetector = null;

    private int ope_cnt = 3;

    private int device;
    private SoundPool sPool;

    int handleflag = 0;// 0 - > ????????? ?????? // 1 ????????? // 2 ???????????? ?????????

    private boolean isFirst = true;
    //private boolean timeLabStart = false;
    //private DataSaveThread saveThread = null;
    private DataSaveThread2 saveThread = null;

    private final boolean[] isSave = new boolean[]{false, false};

    private ActivityMeasureOneBinding binding;
    private boolean isDestroy = false;

    private String saveFileName = null;


    //private File file = null;

    private final Spinner_Re_Adapter recordAdapter = new Spinner_Re_Adapter(new ArrayList<>());
    private SaveDialogDialog saveDialog = null;
    private SaveFileDialogListener saveFileListener = null;
    private boolean isSaveDiClick = false;
    private boolean isSaveDone = false;
    private int savePercent = 0;

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // ???????????? ??????????????? ??? ???????????? ?????????
            // ????????? ????????? ??????????????? ??????
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // ???????????? ???????????? ????????? ????????????
            btService.SetMonitorHandler(new MessageHandler(), device);

            isService = true;

            isState = btService.getState(device);

            if (isPreview) {
                if (powerStatus == BTService.POWER_BATT || powerStatus == BTService.POWER_USB_FULL_CHARGE) {
                    Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                    isPreview = false;
                    santeApps.SetPreview(isPreview, device);
                }
                /*if (powerStatus != BTService.POWER_BATT) {
                    Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                    isPreview = false;
                    santeApps.SetPreview(isPreview, device);
                }*/
                else if (isState == BTService.STATE_CONNECTED && !isStart) {

                    cntIgnore = 25;
                    SetWatch(0);
                    fragMeasure.Init();
                    hasData = false;

                    SetTimeRange();

                    btService.SetEMGFilter(santeApps.GetEMGNotch(device),
                            santeApps.GetEMGHPF(device), santeApps.GetEMGLPF(device), device);
                    btService.SetAccFilter(santeApps.GetAccHPF(device), santeApps.GetAccLPF(device), device);
                    btService.SetGyroFilter(santeApps.GetGyroHPF(device), santeApps.GetGyroLPF(device), device);

                    btService.Start(device);

                }

            }

            UpdateUI();

        }

        public void onServiceDisconnected(ComponentName name) {
            // ???????????? ????????? ????????? ??? ???????????? ?????????
            isService = false;
            //isState = STATE_NONE;
            UpdateUI();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMeasureOneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        device = getIntent().getIntExtra("device", 0);

        ScreenSize screen = new ScreenSize();
        screen.getStandardSize(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //binding.backContainer.setOnClickListener(v -> finish());
        binding.backImb.setOnClickListener(v -> {
            if (isStart | isPreview) {
                btService.Stop(device);
                deleteFile();
                isStart = false;
                isPreview = false;
            }
            santeApps.SetPreview(false, device);
            isDestroy = true;
            finish();

        });

        binding.dropdownMenuBtn.setVisibility(View.VISIBLE);
        findViewById(R.id.dropdown_menu_btn).setVisibility(View.VISIBLE);

        binding.recordRecyclerview.setAdapter(recordAdapter);
        binding.recordRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recordRecyclerview.setHasFixedSize(true);

        binding.btnSectionRecord.setOnClickListener(v -> {
            binding.recordBackground.setBackground(ContextCompat.getDrawable(this, R.drawable.button_08));
            binding.txtWatchSecond.setTextColor(ContextCompat.getColor(this, R.color.mainColor));
            binding.txtWatchMillisecond.setTextColor(ContextCompat.getColor(this, R.color.mainColor));
            binding.dropdownMenuBtn.setVisibility(View.VISIBLE);
            String time = binding.txtWatchSecond.getText().toString() + binding.txtWatchMillisecond.getText().toString();
            recordAdapter.addTime(time);
        });


        binding.recordBackground.setOnClickListener(v -> {
            showRecord(binding.spinnerLayoutBackground.getVisibility() == View.VISIBLE);
        });

        santeApps = (SanteApp) this.getApplication();


        InitControl();

        BeepInit();

        isAlarm = santeApps.GetAlarm(device);

        isPreview = santeApps.GetPreview(device);

        hasData = false;

        thresholdLeadoff = santeApps.GetLeadOff(device) * 100.0f + 200.0f;


        Intent intent = new Intent(
                Measure1chActivity.this, // ?????? ??????
                BTService.class); // ??????????????? ????????????

        bindService(intent, // intent ??????
                conn, // ???????????? ????????? ?????? ??????
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (isStart) {
            MeasureStop(true);
        }
        isDestroy = true;
        finish();
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void SetTimeRange() {
        if (!hasData) {
            binding.txtTimeMin.setText("0.00");
            binding.txtTimeMax.setText(String.format("%.2f", fragMeasure.GetTimeRange()));

        } else {
            /*if (fragMeasure.GetTimeStart() > 0.01F) {
                Log.wtf("GetTimeStart>0.01", String.format("%.2f", fragMeasure.GetTimeStart()));
            }*/
            //Log.wtf("GetTimeStart", String.valueOf(fragMeasure.GetTimeStart()));
            binding.txtTimeMin.setText(String.format("%.2f", fragMeasure.GetTimeStart()));
            binding.txtTimeMax.setText(String.format("%.2f", fragMeasure.GetTimeStart() + fragMeasure.GetTimeRange()));

        }


    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void SetWatch(int cnt) {

        int hour = 0;
        int minute = 0;
        int second = 0;
        int milliSecond = 0;

        if (cnt < 0) {
            binding.txtWatchSecond.setText("00:00");
            binding.txtWatchMillisecond.setText(":00");
            return;
        }


        milliSecond = (int) Math.floor((double) cnt / (double) SAMPLE_RATE * 100.0);
        second = (int) Math.floor((double) milliSecond / 100.0);
        milliSecond = milliSecond % 100;
        minute = (int) Math.floor((double) second / 60.0);
        second = second % 60;

        if (cntWatch >= 2000) {
            /*binding.txtWatchSecond.setText(String.format("%02d:%02d", minute, second));
            binding.txtWatchMillisecond.setText(String.format(":%02d", milliSecond));*/
            //Log.wtf("SetWatch", String.valueOf(cntWatch));
        }

        binding.txtWatchSecond.setText(String.format("%02d:%02d", minute, second));
        binding.txtWatchMillisecond.setText(String.format(":%02d", milliSecond));
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

        if (isFinishing()) {
            if (isService) {
                boolean tmp = isPreview;
                if (isStart | isPreview) {
                    btService.Stop(device);
                }
                //MeasureStop();

                santeApps.SetPreview(tmp, device);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                unbindService(conn);
            }
        }

        try {
            timeThread.removeMessages(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            timeThread.removeMessages(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            timeThread.removeMessages(2);

        } catch (Exception e) {
            e.printStackTrace();
        }

        binding = null;
        fragMeasure = null;
        timeThread = null;
        santeApps = null;
        saveThread = null;

    }

    @SuppressLint("DefaultLocale")
    private void UpdateUI() {
        if (isDestroy) {
            return;
        }

        binding.btnAlarm.setSelected(isAlarm);
        if (isAlarm)
            binding.btnAlarm.setBackground(ContextCompat.getDrawable(this, R.drawable.button_01));
        else
            binding.btnAlarm.setBackground(ContextCompat.getDrawable(this, R.drawable.button_02));

        binding.btnPreview.setSelected(isPreview);
        binding.btnWatchstop.setSelected(isWatch);

        binding.txtLeadoff.setVisibility(View.INVISIBLE);

        if (isService) {
            switch (isState) {
                case BTService.STATE_NONE: /*{
                    binding.btnStart.setEnabled(false);
                    binding.btnWatchstop.setEnabled(false);
                    binding.btnAllstop.setEnabled(false);
                    binding.btnPreview.setEnabled(true);
                    break;
                }*/


                case BTService.STATE_CONNECTING: {
                    binding.btnStart.setEnabled(false);
                    binding.btnWatchstop.setEnabled(false);
                    binding.btnAllstop.setEnabled(false);
                    binding.btnPreview.setEnabled(true);
                    break;
                }

                case BTService.STATE_CONNECTED: {

                    if (isStart) {

                        binding.btnStart.setEnabled(false);
                        binding.btnWatchstop.setEnabled(true);
                        binding.btnAllstop.setEnabled(true);
                        binding.btnPreview.setEnabled(false);
                        binding.btnSectionRecord.setEnabled(true);
                    } else {
                        binding.btnStart.setEnabled(true);
                        binding.btnWatchstop.setEnabled(false);
                        binding.btnAllstop.setEnabled(false);
                        binding.btnPreview.setEnabled(true);
                        binding.btnSectionRecord.setEnabled(false);
                    }
                    break;
                }
//
            }
        } else {

//            devState.setBackgroundColor(getResources().getColor(R.color.DeviceStateDisconnect));


            binding.btnStart.setEnabled(false);
            binding.btnWatchstop.setEnabled(false);
            binding.btnAllstop.setEnabled(false);
            binding.btnPreview.setEnabled(true);
        }

        if (fragMeasure.GetEnable(0)) {

            binding.txtEnableAcc.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextSel));
        } else {
            binding.txtEnableAcc.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
        }
        if (fragMeasure.GetEnable(1)) {
            binding.txtEnableGyro.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextSel));
        } else {
            binding.txtEnableGyro.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
        }
        if (fragMeasure.GetEnable(2)) {
            binding.txtEnableEmg.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextSel));
        } else {
            binding.txtEnableEmg.setTextColor(
                    ContextCompat.getColor(this, R.color.EnableBtnTextNotSel));
        }

        binding.txtAccMax.setText(String.format("%.1f", fragMeasure.GetAccMax()));
        binding.txtAccMin.setText(String.format("%.1f", fragMeasure.GetAccMin()));
        binding.txtGyroMax.setText(String.format("%.1f", fragMeasure.GetGyroMax()));
        binding.txtGyroMin.setText(String.format("%.1f", fragMeasure.GetGyroMin()));

        binding.txtEmgMax.setText(String.format("%.0f", fragMeasure.GetEMGMax()));
        binding.txtEmgMin.setText(String.format("%.0f", fragMeasure.GetEMGMin()));

        SetTimeRange();
    }


    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void InitControl() {
        fragMeasure = (MeasureFragment) getSupportFragmentManager().findFragmentById(R.id.frag_graph_measure);
        fragMeasure.SetAccRange(santeApps.GetAccMax(device), santeApps.GetAccMin(device));
        fragMeasure.SetGyroRange(santeApps.GetGyroMax(device), santeApps.GetGyroMin(device));
        fragMeasure.SetEMGRange(santeApps.GetEMGMax(device), santeApps.GetEMGMin(device));
        fragMeasure.SetTimeRange(santeApps.GetTimeRange(device));


        MeasureView mView = (MeasureView) fragMeasure.getView();
        if (mView != null) {
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

        binding.btnStart.setOnClickListener(v -> { /// ????????????

            recordAdapter.removeAllItem();
            timeThread = new TimeThread();
            UserInfo.getInstance().measureTime = new Date(System.currentTimeMillis());

            if (powerStatus != BTService.POWER_BATT) {
                Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
            } else if (isState == BTService.STATE_CONNECTED) {
                if (isPreview && !isStart) {
                    btService.Stop(device);
                }
                isPreview = false;

                isStart = true;
                isFirst = true;

                hasData = true;

                /*binding.btnAccAxis.setEnabled(false);
                binding.btnGyroAxis.setEnabled(false);
                binding.btnEmgAxis.setEnabled(false);*/

                if (isAlarm) cntWatch = SAMPLE_RATE * -3;
                else cntWatch = 0;

                isWatch = true;
                isSaveDone = false;
                isSaveDiClick = false;
                savePercent = 0;

                //baseTime = SystemClock.elapsedRealtime();
                //timerHandler.sendEmptyMessage(0);
                SetWatch(cntWatch);
                fragMeasure.Init();
                SetTimeRange();

                long now = System.currentTimeMillis();
                UserInfo.getInstance().measureTime = new Date(now);
                UserInfo.getInstance().alarm = isAlarm;
                UserInfo.getInstance().watchCnt = 0;
                fragMeasure.setRMSFilte(device);

                cntIgnore = 25;

                btService.SetEMGFilter(santeApps.GetEMGNotch(device),
                        santeApps.GetEMGHPF(device), santeApps.GetEMGLPF(device), device);
                btService.SetAccFilter(santeApps.GetAccHPF(device), santeApps.GetAccLPF(device), device);
                btService.SetGyroFilter(santeApps.GetGyroHPF(device), santeApps.GetGyroLPF(device), device);

                btService.Start(device);
                binding.txtWatchSecond.setText("00:00");
                binding.txtWatchMillisecond.setText(":00");
                binding.testNameEdt.setEnabled(false);

                if (isAlarm) {
                    timeThread = new TimeThread();
                    handleflag = 0;
                    timeThread.sendEmptyMessage(0);

                } else {

                    //BeepPlay();
                }

            }
            UpdateUI();


        });


        binding.btnWatchstop.setSelected(isWatch);
        binding.btnWatchstop.setOnClickListener(v -> {

            isWatch = !isWatch;
            UpdateUI();

        });


        binding.btnAllstop.setSelected(true);
        binding.btnAllstop.setOnClickListener(v -> {
            //isFirst = true;
            //?????? ????????????
            Log.wtf("StopSave", "btnAllstop");
            StopSave(0);
            MeasureStop(true);
        });
        //btnAlarm = (Button) findViewById(R.id.btn_alarm);
        binding.btnAlarm.setSelected(isAlarm);
        binding.btnAlarm.setOnClickListener(v -> {
            isAlarm = !isAlarm;
            santeApps.SetAlarm(isAlarm, device);
            UpdateUI();
        });

        binding.btnPreview.setSelected(isPreview);
        binding.btnPreview.setOnClickListener(v -> {
            isPreview = !isPreview;
            santeApps.SetPreview(isPreview, device);


            if (isPreview) {

                if (powerStatus != BTService.POWER_BATT) {
                    Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                    isPreview = false;
                    santeApps.SetPreview(isPreview, device);
                } else if (isState == BTService.STATE_CONNECTED && !isStart) {
                    cntIgnore = 25;
                    SetWatch(0);
                    fragMeasure.Init();

                    hasData = false;

                    SetTimeRange();

                    btService.SetEMGFilter(santeApps.GetEMGNotch(device),
                            santeApps.GetEMGHPF(device), santeApps.GetEMGLPF(device), device);
                    btService.SetAccFilter(santeApps.GetAccHPF(device), santeApps.GetAccLPF(device), device);
                    btService.SetGyroFilter(santeApps.GetGyroHPF(device), santeApps.GetGyroLPF(device), device);

                    btService.Start(device);
                }
            } else {
                if (isState == BTService.STATE_CONNECTED && !isStart) {
                    btService.Stop(device);
                }
            }


            UpdateUI();
        });


        binding.btnAccAxis.setOnClickListener(v -> {
            if (isStart || isPreview) {
                showToast("?????? ????????? ????????? ??? ????????????.");
                return;
            }
            boolean[] enableAxis = new boolean[3];
            fragMeasure.GetEnable(0, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Acc_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_Acc_FILTER);
        });

        binding.btnGyroAxis.setOnClickListener(v -> {
            if (isStart || isPreview) {
                showToast("?????? ????????? ????????? ??? ????????????.");
                return;
            }
            boolean[] enableAxis = new boolean[3];
            fragMeasure.GetEnable(1, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Gyro_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_Gyro_FILTER);
        });

        binding.btnEmgAxis.setOnClickListener(v -> {
            if (isStart || isPreview) {
                showToast("?????? ????????? ????????? ??? ????????????.");
                return;
            }
            boolean[] enableAxis = new boolean[3];
            fragMeasure.GetEnable(2, enableAxis);

            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("Type", BTService.REQUEST_EMG_FILTER);
            intent.putExtra("EnableAxis", enableAxis);
            intent.putExtra("DeviceNum", 2);

            startActivityForResult(intent, BTService.REQUEST_EMG_FILTER);
        });


        binding.txtEnableEmg.setOnClickListener(v -> {
            fragMeasure.ToggleEnable(2);
            UpdateUI();
        });

        binding.txtEnableAcc.setOnClickListener(v -> {
            fragMeasure.ToggleEnable(0);
            UpdateUI();
        });

        binding.txtEnableGyro.setOnClickListener(v -> {
            fragMeasure.ToggleEnable(1);
            UpdateUI();
        });


        SetTimeRange();


        binding.txtDefaultRange.setOnClickListener(v -> {
            santeApps.SetAccMax(santeApps.GetDefaultAccMax(), device);
            santeApps.SetAccMin(santeApps.GetDefaultAccMin(), device);

            santeApps.SetGyroMax(santeApps.GetDefaultGyroMax(), device);
            santeApps.SetGyroMin(santeApps.GetDefaultGyroMin(), device);

            santeApps.SetEMGMax(santeApps.GetDefaultEMGMax(), device);
            santeApps.SetEMGMin(santeApps.GetDefaultEMGMin(), device);

            fragMeasure.SetAccRange(santeApps.GetAccMax(device), santeApps.GetAccMin(device));
            fragMeasure.SetGyroRange(santeApps.GetGyroMax(device), santeApps.GetGyroMin(device));
            fragMeasure.SetEMGRange(santeApps.GetEMGMax(device), santeApps.GetEMGMin(device));

            UpdateUI();
        });

        //txtFullRange = findViewById(R.id.txt_full_range);
        binding.txtFullRange.setOnClickListener(v -> {
            santeApps.SetAccMax(santeApps.GetFullAccMax(), device);
            santeApps.SetAccMin(santeApps.GetFullAccMin(), device);

            santeApps.SetGyroMax(santeApps.GetFullGyroMax(), device);
            santeApps.SetGyroMin(santeApps.GetFullGyroMin(), device);

            santeApps.SetEMGMax(santeApps.GetFullEMGMax(), device);
            santeApps.SetEMGMin(santeApps.GetFullEMGMin(), device);

            fragMeasure.SetAccRange(santeApps.GetAccMax(device), santeApps.GetAccMin(device));
            fragMeasure.SetGyroRange(santeApps.GetGyroMax(device), santeApps.GetGyroMin(device));
            fragMeasure.SetEMGRange(santeApps.GetEMGMax(device), santeApps.GetEMGMin(device));

            UpdateUI();
        });


        //txtTimeLabel = (TextView) findViewById(R.id.txt_time_label);
        binding.txtTimeLabel.setOnClickListener(v -> {
            Intent intent = new Intent(Measure1chActivity.this, PopupTimeActivity.class);
            intent.putExtra("Type", BTService.REQUEST_Time_RANGE);
            intent.putExtra("deviceLength", 1);
            startActivityForResult(intent, BTService.REQUEST_Time_RANGE);
        });

        binding.btnPrevPage.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure.PrevPage();

            SetTimeRange();
        });


        binding.btnPrevSec.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure.PrevSec();

            SetTimeRange();
        });


        binding.btnNextSec.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure.NextSec();

            SetTimeRange();
        });


        binding.btnNextPage.setOnClickListener(v -> {
            if (isPreview || isStart) return;
            if (!hasData) return;
            fragMeasure.NextPage();

            SetTimeRange();
        });

    }

    private void showToast(String msg) {
        Toast.makeText(Measure1chActivity.this, msg, Toast.LENGTH_SHORT).show();
    }


    private void MeasureStop(boolean isNormalPlace) {
        if (isStart | isPreview) {
            btService.Stop(device);
        }

        if (isStart) {
            //timerHandler.removeMessages(0);
            if (handleflag == 2 || !isAlarm) {
                //?????? ????????????
                Log.wtf("StopSave", "MeasureStop");
                StopSave(0);
                DefaultDialog defaultDialog = new DefaultDialog(this, isSave -> {
                    if (isSave) {
                        UserInfo.getInstance().watchCnt = cntWatch;
                        UserInfo.getInstance().spacial = binding.testNameEdt.getText().toString();
                        fragMeasure.SaveData("ch1", Measure1chActivity.this, recordAdapter.getItems(), santeApps);

                        if (isSaveDone) {
                            showToast("?????? ????????? ?????????????????????.");
                        } else {
                            isSaveDiClick = true;
                        }

                    } else {
                        deleteFile();
                    }
                }, "??????", "??????????????? ?????????????????????????");

                defaultDialog.show();
            }
        }

        isStart = false;
        isPreview = false;

        binding.testNameEdt.setEnabled(isNormalPlace);


        santeApps.SetPreview(isPreview, device);
        UpdateUI();
    }

    @Override
    public void onSuccess(int device, int percent) {
        savePercent = percent;
        if (savePercent >= 100) isSaveDone = true;

        runOnUiThread(() -> {

            if (saveFileListener != null) {
                saveFileListener.onPercent(savePercent);
            }

            if (isSaveDiClick) {
                if (savePercent >= 100) {
                    if (saveDialog != null) {
                        saveDialog.dismiss();
                        saveFileListener = null;
                        saveDialog = null;
                    }
                    showToast("?????? ????????? ?????????????????????.");
                } else {
                    if (saveDialog == null) {
                        saveDialog = new SaveDialogDialog(Measure1chActivity.this, savePercent);
                        saveFileListener = saveDialog;
                        saveDialog.show();
                    }
                }
            }

        });

    }

    @Override
    public void onFail() {
        Toast.makeText(getApplicationContext(), "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void wait(int device) {
        Log.wtf("saveThread", "wait");
    }

    //????????? ????????? ?????????
    class TimeThread extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == 0) {
                try {//????????? ??????
                    if (handleflag == 0) {
                        handleflag = 1;
                        ope_cnt = 3;

                    }

                    int count = fragMeasure.getConunt();
                    //Log.wtf("count", count + "");
                    float[][] gyro = fragMeasure.getGyroData();
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

                    sum = sum / 3 / count;
                    if (sum > 4) {
                        MeasureStop(false);
                        fragMeasure.setGyroData(count);

                        Toast toast = Toast.makeText(getApplicationContext(), "message", Toast.LENGTH_SHORT);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.setText("????????? ?????? ??? ???????????? ?????????????????????.\n?????? ???????????? ???????????? ????????? ?????????.");
                        toast.show();

//                        Toast.makeText(app, "????????? ?????? ??? ???????????? ?????????????????????. ?????? ???????????? ???????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                        sendEmptyMessageDelayed(1, 0);

                    } else if (ope_cnt == 0) {
                        handleflag = 2;

                        sendEmptyMessageDelayed(2, 0);
                    } else {
                        ope_cnt = ope_cnt - 1;

                        sendEmptyMessageDelayed(0, 1000);

                        //timerHandler.sendEmptyMessageDelayed(0, 1000);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (msg.what == 1) {// ???????????? ??????



                if (powerStatus != BTService.POWER_BATT) {
                    Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                } else if (isState == BTService.STATE_CONNECTED) {
                    if (isPreview && !isStart) {
                        btService.Stop(device);
                    }
                    isPreview = false;

                    isStart = true;

                    hasData = true;

                    if (isAlarm) cntWatch = SAMPLE_RATE * -3;
                    else cntWatch = 0;

                    isWatch = true;
                    isSaveDone = false;
                    isSaveDiClick = false;
                    savePercent = 0;

                    SetWatch(cntWatch);
                    fragMeasure.Init();
                    SetTimeRange();

                    long now = System.currentTimeMillis();
                    UserInfo.getInstance().measureTime = new Date(now);
                    UserInfo.getInstance().alarm = isAlarm;
                    UserInfo.getInstance().watchCnt = 0;
                    fragMeasure.setRMSFilte(device);

                    cntIgnore = 25;

                    btService.SetEMGFilter(santeApps.GetEMGNotch(device), santeApps.GetEMGHPF(device)
                            , santeApps.GetEMGLPF(device), device);
                    btService.SetAccFilter(santeApps.GetAccHPF(device), santeApps.GetAccLPF(device), device);
                    btService.SetGyroFilter(santeApps.GetGyroHPF(device), santeApps.GetGyroLPF(device), device);

                    btService.Start(device);

                    binding.txtWatchSecond.setText("00:00");
                    binding.txtWatchMillisecond.setText(":00");
                    binding.testNameEdt.setEnabled(false);

                    if (isAlarm) {
                        timeThread = new TimeThread();
                        handleflag = 0;
                        timeThread.sendEmptyMessage(0);

                    }

                }
                //binding.txtWatchSecond.setText(getTime());
                UpdateUI();

                handleflag = 0;
                ope_cnt = 3;
                sendEmptyMessageDelayed(0, 1000);

            } else if (msg.what == 2) {
                //?????? ?????? ??????
                //BeepPlay();
                /*tone.startTone(ToneGenerator.TONE_DTMF_S, 100);
                sPool.play(beepNum, 1f, 1f, 0, 0, 1f);*/
                removeMessages(0);

            }

        }
    }

    private void BeepInit() {

        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MIN_VOLUME);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(8).build();
        beepNum = sPool.load(getApplicationContext(), R.raw.beep, 1);

    }

    private void BeepPlay() {
        tone.startTone(ToneGenerator.TONE_DTMF_S, 100);
        sPool.play(beepNum, 1f, 1f, 0, 0, 1f);
    }


    @SuppressLint("HandlerLeak")
    private class MessageHandler extends Handler {
        private final int deviceIndex = device;

        public MessageHandler() {
            super();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case BTService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            isState = BTService.STATE_CONNECTED;
                            //isMeasure[deviceIndex] = false;
                            break;
                        case BTService.STATE_CONNECTING:
                            isState = BTService.STATE_CONNECTING;
                            if (isStart | isPreview) {
                                MeasureStop(true);
                            }
                            break;
                        case BTService.STATE_NONE:
                            isState = BTService.STATE_NONE;
                            if (isStart | isPreview) {
                                MeasureStop(true);
                            }
                            break;
                    }
                    //StopSave(deviceIndex);
                    UpdateUI();
                    break;
                case BTService.MESSAGE_DEVICE_INFO:
                    //battLevel[deviceIndex] = ((float)msg.arg1)/1000.0f;

                    if (msg.arg2 == 1) //????????? ?????? ??????
                    {
                        //isUSBPower = false;
                        powerStatus = BTService.POWER_BATT;

                    } else if (msg.arg2 == 2)   //USB ???????????? - ?????????
                    {
                        //isUSBPower = true;
                        powerStatus = BTService.POWER_USB_FULL_CHARGE;
                    } else if (msg.arg2 == 3)   //USB ???????????? - ?????????
                    {
                        //isUSBPower = true;
                        powerStatus = BTService.POWER_USB_CHARGING;
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
                                avgLeadoff = 0;
                                for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
                                    avgLeadoff += data.BPF_DC[i];
                                }
                                avgLeadoff /= (float) BTService.PACKET_SAMPLE_NUM;
                                if (avgLeadoff > thresholdLeadoff && (isPreview || isStart) && fragMeasure.GetEnable(2)) {

                                    binding.txtLeadoff.setVisibility(View.VISIBLE);
                                    UserInfo.getInstance().leadoff = true;

                                } else binding.txtLeadoff.setVisibility(View.INVISIBLE);


                                if (isFirst && !isPreview) {
                                    isFirst = false;
                                    long time = System.currentTimeMillis();
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
                                    String firstDataTime = sdf.format(time);
                                    fragMeasure.SetFirstDataTime(firstDataTime);
                                    UserInfo.getInstance().spacial = binding.testNameEdt.getText().toString();
                                    //?????? ????????????
                                    StartSave(deviceIndex);
                                }

                                if (!fragMeasure.Add(data)) {
                                    MeasureStop(true);
                                } else {
                                    if (isSave[deviceIndex] && saveThread != null) {
                                        //?????? ????????????
                                        saveThread.Add(data);
                                    }
                                }

                                if (isStart && (isWatch || cntWatch <= 0)) {
                                    cntWatch += 40;
                                    if (cntWatch == -160 && isAlarm) BeepPlay();
                                    else if (cntWatch == 40 && !isAlarm) BeepPlay();


                                    SetWatch(cntWatch);
                                    SetTimeRange();
                                }

                                if (powerStatus != BTService.POWER_BATT) {
                                    Toast.makeText(santeApps, "USB??????????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                                    MeasureStop(true);
                                }

                            }

                        }
                    }

                    break;

                case BTService.MESSAGE_COMMAND_RECEIVE:
                    switch (msg.arg1) {
                        case BTService.CMD_BLU_DEV_INITIALIZE:
                            break;
                        case BTService.CMD_BLU_D2P_DATA_REALTIME_START:
                            //isMeasure[deviceIndex] = true;
                            //UpdateUI();
                            break;
                        case BTService.CMD_BLU_D2P_DATA_STOP:
                            //StopSave(deviceIndex);
                            //isMeasure[deviceIndex] = false;
                            //UpdateUI();
                            break;
                        case BTService.CMD_BLU_COMM_START:
                            break;
                    }
                    break;

                case BTService.MESSAGE_DATA_OVERFLOW:
                    /*if (msg.arg1 == 0) {

                    } else {

                    }*/
                    //StopSave(deviceIndex);
                    if (btService != null) btService.Close(deviceIndex);
                    break;

            }
        }

    }

    private void deleteFile() {
        Log.wtf("deleteFile", "deleteFile");
        //File file = new File(Measure1chActivity.this.getExternalFilesDir(null), "/I-Motion Lab/" + saveFileName);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);
        if (file.exists()) {
            file.delete();
            saveFileName = null;
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void StartSave(int index) {

        Log.wtf("StopSave", "StartSave");
        StopSave(index);
        if (saveThread != null) {
            saveThread = null;
        }
        Log.wtf("Measure1chAct", "StartSave");
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "????????? ?????? ????????? ?????????\n???????????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show();
            return;
        }

        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
        String firstDataTime = sdf.format(time);
        boolean isDirExist = createFolder();


        if (isDirExist) {
            saveFileName = UserInfo.getInstance().name;
            saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", new Date()).toString();
            saveFileName += UserInfo.getInstance().spacial + "_";
            int device = index + 1;
            saveFileName += "ch" + device + ".csv";
            //File file = new File(Measure1chActivity.this.getExternalFilesDir(null), "/I-Motion Lab/" + saveFileName);
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);

            saveThread = new DataSaveThread2(file, index, santeApps, firstDataTime, this);
            isSave[index] = true;
            saveThread.setDaemon(true);
            saveThread.start();

        }
    }

    private boolean createFolder() {
        boolean ret = false;
        //File f = new File(Measure1chActivity.this.getExternalFilesDir(null), "/I-Motion Lab/");
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/");

        if (f.exists()) {
            ret = f.isDirectory();
        } else {
            try {
                ret = f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                ret = false;
            }
        }
        return ret;
    }


    private void StopSave(int index) {
        isSave[index] = false;
        Log.wtf("StopSave", "StopSave");
        if (saveThread != null) {
            saveThread.cancle();

            if (saveThread.isAlive()) {
                try {
                    saveThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //saveThread = null;

            //isFirst = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == BTService.RESULT_CANCLE) return;

        if (requestCode == BTService.REQUEST_EMG_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetEMGFilter(app.GetEMGNotch(), app.GetEMGHPF(), app.GetEMGLPF());
            fragMeasure.SetEMGRange(santeApps.GetEMGMax(device), santeApps.GetEMGMin(device));
            fragMeasure.SetEnable(2, enableAxis[0], enableAxis[1], enableAxis[2]);
        } else if (requestCode == BTService.REQUEST_Acc_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetAccFilter(app.GetAccHPF(), app.GetAccLPF());
            fragMeasure.SetAccRange(santeApps.GetAccMax(device), santeApps.GetAccMin(device));
            fragMeasure.SetEnable(0, enableAxis[0], enableAxis[1], enableAxis[2]);

        } else if (requestCode == BTService.REQUEST_Gyro_FILTER) {
            boolean[] enableAxis = null;
            enableAxis = data.getBooleanArrayExtra("EnableAxis");

            //btService.SetGyroFilter(app.GetGyroHPF(), app.GetGyroLPF());
            fragMeasure.SetGyroRange(santeApps.GetGyroMax(device), santeApps.GetGyroMin(device));
            fragMeasure.SetEnable(1, enableAxis[0], enableAxis[1], enableAxis[2]);

        } else if (requestCode == BTService.REQUEST_Time_RANGE) {
            fragMeasure.SetTimeRange(santeApps.GetTimeRange(device));
        }


        UpdateUI();
    }
}