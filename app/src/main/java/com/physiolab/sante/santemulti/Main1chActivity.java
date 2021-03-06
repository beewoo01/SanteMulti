package com.physiolab.sante.santemulti;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ScreenSize;
import com.physiolab.sante.UserInfo;
import com.physiolab.sante.santemulti.databinding.Activity1chMainBinding;


import java.util.Map;
import java.util.Set;

import static com.physiolab.sante.BlueToothService.BTService.MESSAGE_COMMAND_RECEIVE;
import static com.physiolab.sante.BlueToothService.BTService.MESSAGE_DATA_OVERFLOW;
import static com.physiolab.sante.BlueToothService.BTService.MESSAGE_DATA_RECEIVE;
import static com.physiolab.sante.BlueToothService.BTService.MESSAGE_DEVICE_INFO;
import static com.physiolab.sante.BlueToothService.BTService.MESSAGE_STATE_CHANGE;
import static com.physiolab.sante.BlueToothService.BTService.POWER_BATT;
import static com.physiolab.sante.BlueToothService.BTService.POWER_USB_CHARGING;
import static com.physiolab.sante.BlueToothService.BTService.POWER_USB_FULL_CHARGE;
import static com.physiolab.sante.BlueToothService.BTService.STATE_CONNECTED;
import static com.physiolab.sante.BlueToothService.BTService.STATE_CONNECTING;
import static com.physiolab.sante.BlueToothService.BTService.STATE_NONE;

public class Main1chActivity extends AppCompatActivity {

    private BTService btService = null;
    private boolean isService = false;
    private Activity1chMainBinding binding;
    private ScreenSize screen = null;

    private static final String TAG = "Activity-Measure";
    //private int isState = STATE_NONE;
    private final int[] isState = new int[]{STATE_NONE};
    private final int[] powerStatus = new int[]{BTService.POWER_NONE, BTService.POWER_NONE};
    //private static final boolean D = true;
    private final int PERMISSION_STORAGE = 1;

    private final float[] battLevel = new float[]{0.0f, 0.0f};
    private final int[] battUpdateCnt = new int[]{3, 3};
    private String[] deviceAddress;

    private boolean infoGender = true;
    private int batt = 0;

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // ???????????? ??????????????? ??? ???????????? ?????????
            // ????????? ????????? ??????????????? ??????
            //Log.d(TAG, "onServiceConnected");
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // ???????????? ???????????? ????????? ????????????
            //btService.SetMainHandler1(mHandler);

            btService.Init();
            //btService.SetMainHandler(mHandler);
            btService.SetMainHandler(new MessageHandler(0), 0);
            isService = true;
            // ???????????? ????????? ??????????????? ??????

            UpdateUI();

            BTCheck();

        }

        public void onServiceDisconnected(ComponentName name) {
            // ???????????? ????????? ????????? ??? ???????????? ?????????
            //Log.d(TAG, "onServiceDisconnected");
            isService = false;
            isState[0] = STATE_NONE;
            UpdateUI();

        }
    };

    public void BTCheck() {
        if (btService.getDeviceState())    //????????? ??????????????? ??????????????? ??????
        {
            if (btService.enableBlueTooth(this)) {
                UpdateSerial();
            }
        } else {
            finish();
        }
    }

    private void UpdateSerial() {
        //Log.d(TAG, "UpdateSerial Start");


        Set<BluetoothDevice> devices = btService.GetPairedDeviceList();
        String[] deviceAddress;
        if (devices != null) {
            deviceAddress = new String[devices.size()];
        }else  {
            deviceAddress = new String[0];
        }

        this.deviceAddress = deviceAddress;
        int cnt = 0;

        AdapterView.OnItemSelectedListener onSelChanged = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        if (devices != null) {
            for (BluetoothDevice f : devices) {
                deviceAddress[cnt++] = f.getAddress();
            }
        }


        SpinnerAdapter adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, deviceAddress);
        adapterDevice.SetTextSize(16);
        binding.spinDevice.setAdapter(adapterDevice);
        binding.spinDevice.setOnItemSelectedListener(onSelChanged);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = Activity1chMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CheckPermission();
        InitControl();

        Intent intent = new Intent(
                Main1chActivity.this, // ?????? ??????
                BTService.class); // ??????????????? ????????????

        bindService(intent, // intent ??????
                conn, // ???????????? ????????? ?????? ??????
                Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.birthEdt.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onDestroy() {
        //Log.d(TAG, "onDestroy");
        super.onDestroy();

        if (isFinishing()) {
            //Log.d(TAG, "onDestroy-Finishing");
            if (isService) {
                DeviceClose();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                unbindService(conn);
            }
        }
    }

    private void InitControl() {

        screen = new ScreenSize();
        screen.getStandardSize(this);


        binding.backContainer.setOnClickListener(v -> finish());

        binding.btnMale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????

            infoGender = true;
            binding.rbMale.setChecked(true);
            binding.rbFemale.setChecked(false);
        });

        binding.btnFemale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????

            infoGender = false;
            binding.rbMale.setChecked(false);
            binding.rbFemale.setChecked(true);
        });

        binding.btnDeviceOpen.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????
            DeviceOpen();
        });

        binding.btnDeviceClose.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????
            DeviceClose();
        });

        binding.backImb.setOnClickListener(v -> {
            finish();
        });

        binding.btnMale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????
            infoGender = true;
            binding.rbMale.setChecked(true);
            binding.rbFemale.setChecked(false);
        });

        binding.btnFemale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????
            infoGender = false;
            binding.rbMale.setChecked(false);
            binding.rbFemale.setChecked(true);
        });


        binding.btnDeviceMeasure.setOnClickListener(v -> {

            isVaild();
        });

    }

    @SuppressLint("SetTextI18n")
    private void isVaild() {

        if (isState[0] == STATE_NONE) {
            showToast("????????? ??????????????????");
            return;

        } else if (batt <= 0) {
            showToast("???????????? ???????????? ??????????????????");
            return;

        } else if (TextUtils.isEmpty(binding.editName.getText().toString()) || binding.editName.getText().toString().length() < 1) {
            showToast("?????????????????? ??????????????????");
            return;
        }

        if (!TextUtils.isEmpty(binding.birthEdt.getText().toString()) && binding.birthEdt.getText().toString().length() < 10) {
            showToast("??????????????? ????????? ??????????????????");
            return;
        }

        if (TextUtils.isEmpty(binding.editHeight.getText().toString()) || binding.editHeight.getText().toString().length() < 1) {
            binding.editHeight.setText("100");
        }

        if (TextUtils.isEmpty(binding.birthEdt.getText().toString()) || binding.birthEdt.getText().toString().length() < 1) {
            binding.birthEdt.setText("1900-01-01");
        }

        if (TextUtils.isEmpty(binding.editWeight.getText().toString()) || binding.editWeight.getText().toString().length() < 1) {
            binding.editWeight.setText("10");
        }

        if (!binding.rbMale.isChecked() && !binding.rbFemale.isChecked()) {
            binding.rbMale.setChecked(true);
        }
        binding.birthEdt.removeTextChangedListener(textWatcher);
        moveMeasure();

    }

    private void moveMeasure() {
        infoGender = binding.rbMale.isChecked();
        Intent intent = new Intent(Main1chActivity.this, Measure1chActivity.class);
        UserInfo.getInstance().name = binding.editName.getText().toString();
        UserInfo.getInstance().height = binding.editHeight.getText().toString();
        UserInfo.getInstance().weight = binding.editWeight.getText().toString();
        UserInfo.getInstance().birth = binding.birthEdt.getText().toString();
        UserInfo.getInstance().memo = binding.editMemo.getText().toString();
        UserInfo.getInstance().gender = infoGender;
        startActivity(intent);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * SDK 31 Android 12
     * ????????? BLUETOOTH_CONNECT ????????? ?????? ???????????????
     * */

    /*private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT
            });
        }
    }*/

    /*private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                for (Map.Entry<String, Boolean> i : result.entrySet()) {
                    Log.wtf("i.getValue()", String.valueOf(i.getValue()));
                }

            });*/


    private void DeviceOpen() {
        if (isState[0] == STATE_NONE) {

            String deviceAddress = "";
            Object selObj = null;

            selObj = binding.spinDevice.getSelectedItem();
            if (selObj != null) deviceAddress = selObj.toString();


            if (isState[0] != STATE_NONE && deviceAddress.equals(btService.getDeviceNum(0))) return;

            if (btService != null) btService.Connect(deviceAddress, 0);
        }

        Object selObj = null;
        Spinner spinDevice = findViewById(R.id.spin_device);
        selObj = spinDevice.getSelectedItem();
        if (selObj != null) {
            deviceAddress[0] = selObj.toString();
            //btService.Init();
        }
        UpdateUI();

    }

    //???????????? ?????? ????????? ?????????
    private final TextWatcher textWatcher = new TextWatcher() {

        private int _beforeLenght = 0;


        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            _beforeLenght = s.length();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() <= 0) {
                return;
            }

            char inputChar = s.charAt(s.length() - 1);
            if (inputChar != '-' && (inputChar < '0' || inputChar > '9')) {
                binding.birthEdt.getText().delete(s.length() - 1, s.length());
                return;
            }

            int _afterLenght = s.length();

            // ?????? ???
            if (_beforeLenght > _afterLenght) {
                // ?????? ?????? ???????????? -??? ???????????? ?????????
                if (s.toString().endsWith("-")) {
                    binding.birthEdt.setText(s.toString().substring(0, s.length() - 1));
                }
            }
            // ?????? ???
            else if (_beforeLenght < _afterLenght) {
                if (_afterLenght == 5 && !s.toString().contains("-")) {
                    binding.birthEdt.setText(s.toString().subSequence(0, 4) + "-" + s.toString().substring(4, s.length()));
                } else if (_afterLenght == 8) {
                    binding.birthEdt.setText(s.toString().subSequence(0, 7) + "-" + s.toString().substring(7, s.length()));
                }
            }
            binding.birthEdt.setSelection(binding.birthEdt.length());

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void DeviceClose() {
        if (btService != null) btService.Close(0);
    }


    private class MessageHandler extends Handler {
        private int deviceIndex = -1;

        public MessageHandler(int index) {
            super(Looper.getMainLooper());
            deviceIndex = index;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            //if (D) Log.d(TAG, "Device Connect");
                            isState[deviceIndex] = STATE_CONNECTED;
                            //isMeasure[deviceIndex] = false;
                            break;
                        case STATE_CONNECTING:
                            isState[deviceIndex] = STATE_CONNECTING;
                            break;
                        case STATE_NONE:
                            isState[deviceIndex] = STATE_NONE;
                            //if (D) Log.d(TAG, "Device Close");
                            break;
                    }
                    //StopSave(deviceIndex);
                    UpdateUI();
                    break;
                case MESSAGE_DEVICE_INFO:
                    battLevel[deviceIndex] = ((float) msg.arg1) / 1000.0f;

                    if (msg.arg2 == 1) //????????? ?????? ??????
                    {
                        powerStatus[deviceIndex] = POWER_BATT;
                    } else if (msg.arg2 == 2)   //USB ???????????? - ?????????
                    {
                        powerStatus[deviceIndex] = POWER_USB_FULL_CHARGE;
                    } else if (msg.arg2 == 3)   //USB ???????????? - ?????????
                    {
                        powerStatus[deviceIndex] = POWER_USB_CHARGING;
                    }
                    battUpdateCnt[deviceIndex]--;
                    if (battUpdateCnt[deviceIndex] <= 0) {
                        UpdateUI();
                        battUpdateCnt[deviceIndex] = 3;
                    }

                    break;

                case MESSAGE_DATA_RECEIVE:

                    break;

                case MESSAGE_COMMAND_RECEIVE:

                    break;

                case MESSAGE_DATA_OVERFLOW:
                    if (msg.arg1 == 0) {
                    } else {
                    }
                    //StopSave(deviceIndex);
                    if (btService != null) btService.Close(deviceIndex);
                    break;

            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void UpdateUI() {

        if (isService) {
            switch (isState[0]) {
                case STATE_NONE:
                    binding.btnDeviceOpen.setEnabled(true);
                    binding.btnDeviceClose.setEnabled(false);
                    binding.btnDeviceMeasure.setEnabled(true);
                    binding.spinDevice.setEnabled(true);
                    binding.imgBatt.setVisibility(View.GONE);
                    binding.tvBatt.setVisibility(View.GONE);
                    //a = 0;
                    break;
                case STATE_CONNECTING:
                    binding.btnDeviceOpen.setEnabled(false);
                    binding.btnDeviceClose.setEnabled(true);
                    binding.btnDeviceMeasure.setEnabled(true);
                    binding.spinDevice.setEnabled(false);


                    break;
                case STATE_CONNECTED:
                    binding.btnDeviceOpen.setEnabled(false);
                    binding.btnDeviceClose.setEnabled(true);
                    binding.btnDeviceMeasure.setEnabled(true);
                    binding.spinDevice.setEnabled(false);
                    binding.tvBatt.setVisibility(View.VISIBLE);
                    binding.imgBatt.setVisibility(View.VISIBLE);

                    //batt = Math.round(battLevel[0] * 20);

                    binding.tvBatt.setText(batt + "%");
                    /*
                     * 4.2 >= 100
                     * 4.1 = 90
                     * 4.0 = 80
                     * 3.9 = 70
                     * 3.8 = 60
                     * 3.7 = 50
                     * 3.6 = 40
                     * 3.5 = 30    4.29 ~ 3.50 ?????? ??????
                     * 3.4 = 20 --> 3.49 ?????? 0% ????????????
                     * 3.3 = 10
                     * 3.2 = 0
                     *
                     *
                     *
                     * 3.99 - 0.3 = 3.69   69
                     * 3.89
                     * 3.79
                     * 3.69
                     * */

                    float min = 3.400F;
                    float max = 4.200F;

                    if (battLevel[0] >= 4.200) {
                        //100
                        batt = 100;
                    } else {
                        //99??????
                        batt = (int) ((battLevel[0] - min) / (max - min) * 100);

                        if (batt < 0) {
                            batt = 0;
                        }
                    }


                    /*if (battLevel[0] >= 4.0) {
                        //4
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));

                    } else if (battLevel[0] >= 3.55) {
                        //3
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));

                    } else if (battLevel[0] >= 3.49) {
                        //2
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));

                    } else if (battLevel[0] >= 3.45 || battLevel[0] >= 3.25) {
                        //1
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));

                    } else if (battLevel[0] <= 3.25) {
                        //0
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));
                    }*/

                    if (batt >= 100) {
                        //4 100%
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));

                    } else if (batt > 74) {
                        //3 99 ~ 75
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));

                    } else if (batt > 49) {
                        //2 74 ~ 50
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));

                    } else if (batt > 24) {
                        //1 49 ~ 25
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));

                    } else {
                        //0 24 ~ 0
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));
                    }

                    binding.tvBatt.setText(batt + "%");

                    break;
            }
        } else {
            binding.btnDeviceOpen.setEnabled(false);
            binding.btnDeviceClose.setEnabled(false);
            binding.btnDeviceMeasure.setEnabled(false);
            binding.spinDevice.setEnabled(false);
        }

    }

    private void CheckPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ?????? ?????? ?????? ????????? ???????????? ??? ????????? ?????? ????????? ????????? ?????? ??? (?????? else{..} ?????? ??????)
//             ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_CAMERA);

            // ?????? ???????????? if()?????? ????????? false??? ?????? ??? -> else{..}??? ???????????? ?????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("??????")
                        .setMessage("????????? ????????? ?????????????????????.\n??????????????? ???????????? ????????????.\n????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                        .setNeutralButton("??????", (dialogInterface, i) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setPositiveButton("??????", (dialogInterface, i) -> {
                            //finish();
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            }
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.wtf("?????????", "11111111");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 11);
        }
    }


    public class SpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        String[] items = new String[]{};
        float txtSize = 10.0f;

        public SpinnerAdapter(final Context context,
                              final int textViewResourceId, final String[] objects) {
            super(context, textViewResourceId, objects);
            this.items = objects;
            this.context = context;
        }

        public void SetTextSize(float s) {
            txtSize = s;
        }

        /**
         * ????????? ????????? ???????????? View??? ??????
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
            //tv.setTextColor((getResources().getColor(R.color.SpinTextDisable));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, txtSize);
            return convertView;
        }

        /**
         * ?????? ????????? View ??????
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(
                        android.R.layout.simple_spinner_item, parent, false);
            }

            TextView tv = (TextView) convertView
                    .findViewById(android.R.id.text1);
            tv.setText(items[position]);
            //tv.setTextColor(getResources().getColor(R.color.SpinTextEnable));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, txtSize);
            return convertView;
        }
    }

}