package com.physiolab.sante.santemulti;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.SearchDeviceAdapter;
import com.physiolab.sante.UserInfo;
import com.physiolab.sante.santemulti.databinding.Activity2chMainBinding;

import java.util.ArrayList;
import java.util.Iterator;
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

public class Main2chActivity extends AppCompatActivity {

    private static final String TAG = "Activity-MainTest";
    //private static final boolean D = true;
    private Activity2chMainBinding binding;


    private BTService btService = null;
    private boolean isService = false;
    private final int[] isState = new int[]{STATE_NONE, STATE_NONE};

    private final float[] battLevel = new float[]{0.0f, 0.0f};
    private final int[] powerStatus = new int[]{BTService.POWER_NONE, BTService.POWER_NONE};
    private final boolean[] isMeasure = new boolean[]{false, false};
    private final boolean[] isSave = new boolean[]{false, false};
    private final int[] valueUpdateCnt = new int[]{10, 10};
    private final int[] battUpdateCnt = new int[]{3, 3};

    private final Button[] btn_connect = new Button[2];
    private final Spinner[] spinDevice = new Spinner[2];

    private final ImageView[] battImv = new ImageView[2];
    private final TextView[] battTxv = new TextView[2];

    private final Button[] btn_disConnect = new Button[2];


    private final int[] recvPktCnt = new int[2];

    private final int PERMISSION_STORAGE = 1;

    private BluetoothAdapter bluetoothAdapter;
    private String addedDeviceAddress = null;
    private final int[] batt = new int[]{0, 0};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = Activity2chMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        CheckPermission();
        InitControl();
        updateUI();

        binding.btnDeviceMeasure.setOnClickListener(v -> {
            isVail();

        });


        Intent intent = new Intent(
                Main2chActivity.this, // ?????? ??????
                BTService.class); // ??????????????? ????????????

        bindService(intent, // intent ??????
                conn, // ???????????? ????????? ?????? ??????
                Context.BIND_AUTO_CREATE);

        initView();


    }

    @SuppressLint("SetTextI18n")
    private void isVail() {
        if (isState[0] == STATE_NONE || isState[1] == STATE_NONE) {
            showToast("????????? ??????????????????");
            return;

        } else if (batt[0] <= 0 || batt[1] <= 0) {
            showToast("???????????? ???????????? ??????????????????");
            return;

        } else if (TextUtils.isEmpty(binding.nameEdt.getText().toString()) || binding.nameEdt.getText().toString().length() < 1) {
            showToast("?????????????????? ??????????????????");
            return;
        }

        if (!TextUtils.isEmpty(binding.birthEdt.getText().toString()) && binding.birthEdt.getText().toString().length() < 10) {
            showToast("??????????????? ????????? ??????????????????");
            return;
        }

        if (TextUtils.isEmpty(binding.heightEdt.getText().toString())
                || binding.heightEdt.getText().toString().length() < 1) {
            binding.heightEdt.setText("100");
            //UserInfo.getInstance().height = "100";
        }


        if (TextUtils.isEmpty(binding.birthEdt.getText().toString()) || binding.birthEdt.getText().toString().length() < 1) {
            binding.birthEdt.setText("1900-01-01");
            //showToast("????????? ??????????????? ??????????????????");
        }

        if (TextUtils.isEmpty(binding.weightEdt.getText().toString()) || binding.weightEdt.getText().toString().length() < 1) {
            binding.weightEdt.setText("10");
            //showToast("????????? ???????????? ??????????????????");
        }

        if (!binding.rbMale.isChecked() && !binding.rbFemale.isChecked()) {
            binding.rbMale.setChecked(true);
            //showToast("????????? ????????? ??????????????????");
        }
        binding.birthEdt.removeTextChangedListener(textWatcher);

        moveMeasure();
    }

    private void moveMeasure() {
        boolean infoGender = binding.rbMale.isChecked();
        Intent intent = new Intent(Main2chActivity.this, Measure2chActivity.class);
        UserInfo.getInstance().name = binding.nameEdt.getText().toString();
        UserInfo.getInstance().height = binding.heightEdt.getText().toString();
        UserInfo.getInstance().weight = binding.weightEdt.getText().toString();
        UserInfo.getInstance().birth = binding.birthEdt.getText().toString();
        UserInfo.getInstance().memo = binding.specialEdt.getText().toString();
        UserInfo.getInstance().gender = infoGender;
        startActivity(intent);
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

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    private void CheckPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ?????? ?????? ?????? ????????? ???????????? ??? ????????? ?????? ????????? ????????? ?????? ??? (?????? else{..} ?????? ??????)

            // ?????? ???????????? if()?????? ????????? false??? ?????? ??? -> else{..}??? ???????????? ?????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("??????")
                        .setMessage("????????? ????????? ?????????????????????.\n??????????????? ???????????? ????????????.\n????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                        .setNeutralButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("??????")
                        .setMessage("?????? ????????? ?????????????????????.\n????????? ????????? ????????????.\n????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                        .setNeutralButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 11);
            }
        }
    }

    private void InitControl() {
        btn_connect[0] = binding.btnRightDeviceOpen;
        btn_connect[1] = binding.btnLeftDeviceOpen;

        btn_disConnect[0] = binding.btnRightDeviceClose;
        btn_disConnect[1] = binding.btnLeftDeviceClose;

        //binding.backContainer.setOnClickListener(v -> finish());

        binding.backImb.setOnClickListener(v -> finish());

        binding.rbMale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????

            binding.rbMale.setChecked(true);
            binding.rbFemale.setChecked(false);
        });

        binding.rbFemale.setOnClickListener(v -> {
            // TextView ????????? ??? ??? ????????????

            binding.rbMale.setChecked(false);
            binding.rbFemale.setChecked(true);
        });

        btn_connect[0].setOnClickListener(v -> {
            //Log.wtf("btn_connet 0", "onCLICK");
            if (isState[0] == STATE_NONE) {
                String deviceAddress = "";
                Object selObj = null;

                selObj = spinDevice[0].getSelectedItem();

                if (selObj != null) {
                    deviceAddress = selObj.toString();
                }

                if (isState[1] != STATE_NONE && deviceAddress.equals(btService.getDeviceNum(1)))
                    return;

                if (btService != null) {
                    btService.Connect(deviceAddress, 0);
                }

                updateUI();

            } else {
                if (btService != null) btService.Close(0);
            }

        });

        btn_disConnect[0].setOnClickListener(v -> {
            if (isState[0] == STATE_CONNECTED) {
                if (btService != null) btService.Close(0);
            }
        });


        btn_connect[1].setOnClickListener(v -> {
            if (isState[1] == STATE_NONE) {

                String deviceAddress = "";
                Object selObj = null;

                selObj = spinDevice[1].getSelectedItem();
                if (selObj != null) deviceAddress = selObj.toString();


                if (isState[0] != BTService.STATE_NONE && deviceAddress.equals(btService.getDeviceNum(0)))
                    return;


                if (btService != null) {
                    btService.Connect(deviceAddress, 1);
                }

            } else {
                if (btService != null) btService.Close(1);
            }

            updateUI();
        });

        btn_disConnect[1].setOnClickListener(v -> {
            if (isState[1] == STATE_CONNECTED) {
                if (btService != null) btService.Close(1);
            }
        });

        spinDevice[0] = binding.spinRightDevice;
        spinDevice[1] = binding.spinLeftDevice;

        battImv[0] = binding.rightImgBatt;
        battImv[1] = binding.leftImgBatt;

        battTxv[0] = binding.rightBattTxv;
        battTxv[1] = binding.leftBattTxv;

        binding.connectAll.setOnClickListener(v -> {
            if (isState[0] == STATE_NONE) {
                String deviceAddress = "";
                Object selObj = null;

                selObj = spinDevice[0].getSelectedItem();
                if (selObj != null) deviceAddress = selObj.toString();

                if (isState[1] == BTService.STATE_NONE || deviceAddress.equals(btService.getDeviceNum(1))) {
                    if (btService != null) btService.Connect(deviceAddress, 0);
                }
            }

            if (isState[1] == STATE_NONE) {

                String deviceAddress = "";
                Object selObj = null;

                selObj = spinDevice[1].getSelectedItem();
                if (selObj != null) deviceAddress = selObj.toString();

                if (isState[0] == BTService.STATE_NONE || !deviceAddress.equals(btService.getDeviceNum(0))) {
                    if (btService != null) btService.Connect(deviceAddress, 1);
                }

            }

            updateUI();
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            // ???????????? ??????????????? ??? ???????????? ?????????
            // ????????? ????????? ??????????????? ??????
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // ???????????? ???????????? ????????? ????????????
            btService.Init();
            btService.SetMainHandler(new MessageHandler(0), 0);
            btService.SetMainHandler(new MessageHandler(1), 1);

            isService = true;
            // ???????????? ????????? ??????????????? ??????

            BTCheck();
        }

        public void onServiceDisconnected(ComponentName name) {
            // ???????????? ????????? ????????? ??? ???????????? ?????????
            isService = false;
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


        Set<BluetoothDevice> devices = btService.GetPairedDeviceList();
        String[] deviceAddress;
        if (devices != null) {
            deviceAddress = new String[devices.size()];
        }else {
            deviceAddress = new String[0];
        }

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


        spinDevice[0].setAdapter(adapterDevice);
        spinDevice[0].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length > 0) spinDevice[0].setSelection(0);

        spinDevice[1].setAdapter(adapterDevice);
        spinDevice[1].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length > 1) spinDevice[1].setSelection(1);


    }


    private void initView() {
        /*deviceAddress*/

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        /*if (intentFilter != null) {
            unregisterReceiver(receiver);
        }

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);*/

        /*binding.searchTxv.setOnClickListener( v -> {
            if (intentFilter != null){
                Log.wtf("intentFilter ", "notnull");
            }else {
                Log.wtf("intentFilter ", "null!!");
            }
            if (checkCoarseLocationPermassion()){
                Log.d("searchTxv : ", "true");
                binding.deviceRe.setVisibility(View.VISIBLE);
                binding.bluetoothImv.setVisibility(View.GONE);
                binding.bluetoothTxv.setVisibility(View.GONE);
                boolean bool = bluetoothAdapter.startDiscovery();
                Log.d("Discovery : ", String.valueOf(bool));
            }else {
                Log.d("searchTxv : ", "else");
            }
        });

        showDiscoveredDevices();*/
    }


    /*private final BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //t1.setText("Searching...");
                isShowToast = true;
                binding.searchTxv.setEnabled(false);
                binding.searchTxv.setTextColor(Color.GRAY);
                Toast.makeText(getApplicationContext(), "??????????????? ?????? ????????????.", Toast.LENGTH_SHORT).show();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("ACTION_FOUND", "????????????");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().equalsIgnoreCase("TUG")) {
                    if (!pairedList.contains(device.getAddress())){
                        searchDeviceAdapter.addItem(device);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //t1.setText("Finished");

                binding.searchTxv.setEnabled(true);
                binding.searchTxv.setTextColor(ContextCompat.getColor(MainTestActivity.this, R.color.mainColor));
                if (isShowToast){
                    Toast.makeText(getApplicationContext(), "???????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                }


            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                Log.d("ACTIONBONDSTATECHANGED", "????????????");
            }

        }
    };*/

    /*@Override
    protected void onPause() {
        super.onPause();
        if (intentFilter != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }*/

    private boolean checkCoarseLocationPermassion() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("??????")
                        .setMessage("?????? ????????? ?????????????????????.\n??????????????? ????????? ????????????.\n????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                        .setNeutralButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 11);
            }


            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.birthEdt.addTextChangedListener(textWatcher);
        if (addedDeviceAddress != null) {
            UpdateSerial();
            //BTCheck();
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /*private void showDiscoveredDevices() {
        binding.deviceRe.setLayoutManager(new LinearLayoutManager(this));
        searchDeviceAdapter = new SearchDeviceAdapter(bluetoothDevice -> {
            boolean result = bluetoothDevice.createBond();
            addedDeviceAddress = bluetoothDevice.getAddress();
            Log.wtf("result", String.valueOf(result));

        });
        binding.deviceRe.setAdapter(searchDeviceAdapter);
    }*/

    /*private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth??? ???????????? ?????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                *//*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }*//*
                if (bluetoothAdapter.isDiscovering()) {
                    Toast.makeText(this, "?????? ??????????????????...", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(this, "Bluetooth is enable", Toast.LENGTH_SHORT).show();
                }
            } else {

                Intent enabvleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                resultLauncher.launch(enabvleIntent);
                //startActivityForResult(enabvleIntent, 11);
            }
        }
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    checkBluetoothState();
                }
            }
    );*/


    @SuppressLint("SetTextI18n")
    private void updateUI() {
        for (int i = 0; i < 2; i++) {
            //Log.wtf("isSTATE i " + i, String.valueOf(isState[i]));
            if (isState[i] == STATE_NONE) {

                btn_connect[i].setEnabled(true);
                btn_disConnect[i].setEnabled(false);
                spinDevice[i].setEnabled(true);
                binding.btnDeviceMeasure.setEnabled(true);

                battTxv[i].setVisibility(View.GONE);
                battImv[i].setVisibility(View.GONE);


            } else if (isState[i] == STATE_CONNECTING) {

                spinDevice[i].setEnabled(false);

            } else if (isState[i] == STATE_CONNECTED) {

                btn_connect[i].setEnabled(false);
                btn_disConnect[i].setEnabled(true);
                spinDevice[i].setEnabled(false);

                battTxv[i].setVisibility(View.VISIBLE);

                battImv[i].setVisibility(View.VISIBLE);




                /* battery version1
                if (battLevel[i] >= 4.0) {
                    //100
                    batt = 100;
                }else {
                    //99??????
                    int abc = 90;
                    batt = (int) ((battLevel[i] - 3.0) * 100);

                }

                if (battLevel[i] >= 4.0) {
                    //4
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));

                } else if (battLevel[i] >= 3.55) {
                    //3
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));

                } else if (battLevel[i] >= 3.49) {
                    //2
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));

                }else if (battLevel[i] >= 3.45 || battLevel[i] >= 3.25) {
                    //1
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));

                }else if (battLevel[i] <= 3.25){
                    //0
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));
                }


                */

                float min = 3.400F;
                float max = 4.200F;

                // battery version2
                if (battLevel[i] >= 4.200) {
                    batt[i] = 100;
                }else {
                    batt[i] = (int) ((battLevel[i] - min) / (max - min) * 100);

                    if (batt[i] <= 0) {
                        batt[i] = 0;
                    }
                }


                if (batt[i] >= 100) {
                    //4
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));

                } else if (batt[i] > 74) {
                    //3
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));

                } else if (batt[i] > 49) {
                    //2
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));

                }else if (batt[i] > 24) {
                    //1
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));

                }else {
                    //0
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));
                }



                battTxv[i].setText(batt[i] + "%");


                /*if (batt > 90)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));
                else if (batt > 75)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));
                else if (batt > 50)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));
                else if (batt > 25)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));
                else if (batt > 10)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));*/


            }
        }

        /*if (isState[0] == STATE_NONE && isState[1] == STATE_NONE){
            binding.btnDeviceMeasure.setEnabled(false);
        }else {
            binding.btnDeviceMeasure.setEnabled(true);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 11:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Access coarse location allowed. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("??????")
                            .setMessage("?????? ????????? ?????????????????????.\n??????????????? ????????? ????????????.\n" +
                                    "????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                            .setNeutralButton("??????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                }
                            })
                            .setPositiveButton("??????", (dialogInterface, i) -> {
                                //finish();
                            })
                            .setCancelable(false)
                            .create()
                            .show();
                }
                break;
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 11) {
            checkBluetoothState();
        }
    }*/

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
                            isState[deviceIndex] = STATE_CONNECTED;
                            //isMeasure[deviceIndex] = false;
                            break;
                        case STATE_CONNECTING:
                            isState[deviceIndex] = STATE_CONNECTING;
                            break;
                        case STATE_NONE:
                            isState[deviceIndex] = STATE_NONE;
                            break;
                    }
                    //StopSave(deviceIndex);
                    updateUI();
                    break;
                case MESSAGE_DEVICE_INFO:
                    battLevel[deviceIndex] = ((float) msg.arg1) / 1000.0f;

                    //Log.wtf("battLevel" + deviceIndex, String.valueOf(battLevel[deviceIndex]));

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
                        updateUI();
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
}

