package com.physiolab.sante.santemulti;

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
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ScreenSize;
import com.physiolab.sante.UserInfo;
import com.physiolab.sante.dialog.DefaultDialog;
import com.physiolab.sante.santemulti.databinding.ActivityConnect1chBinding;
import com.physiolab.sante.santemulti.databinding.ActivityMeasureOneBinding;

import java.io.File;
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

public class Connect_1chActivity extends AppCompatActivity {

    private BTService btService = null;
    private boolean isService = false;
    private ActivityConnect1chBinding binding;
    private ScreenSize screen = null;

    private static final String TAG = "Activity-Measure";
    //private int isState = STATE_NONE;
    private final int[] isState = new int[]{STATE_NONE};
    private final int[] powerStatus = new int[]{BTService.POWER_NONE, BTService.POWER_NONE};
    private static final boolean D = true;
    private final int PERMISSION_STORAGE = 1;

    private final float[] battLevel = new float[]{0.0f, 0.0f};
    private final int[] battUpdateCnt = new int[]{3, 3};
    private String[] deviceAddress;

    private boolean infoGender = true;

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            //Log.d(TAG, "onServiceConnected");
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // 서비스가 제공하는 메소드 호출하여
            //btService.SetMainHandler1(mHandler);

            btService.Init();
            //btService.SetMainHandler(mHandler);
            btService.SetMainHandler(new MessageHandler(0), 0);
            isService = true;
            // 서비스쪽 객체를 전달받을수 있슴

            UpdateUI();

            BTCheck();

        }

        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            //Log.d(TAG, "onServiceDisconnected");
            isService = false;
            isState[0] = STATE_NONE;
            UpdateUI();

        }
    };

    public void BTCheck() {
        if (btService.getDeviceState())    //장치가 블루투스를 지원하는지 확인
        {
            if (btService.enableBlueTooth(this)) {
                UpdateSerial();
            }
        } else {
            Log.d(TAG, "Bluetooth is not supported");
            finish();
        }
    }

    private void UpdateSerial() {
        //Log.d(TAG, "UpdateSerial Start");


        Set<BluetoothDevice> devices = btService.GetPairedDeviceList();
        String[] deviceAddress = new String[devices.size()];
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

        for (BluetoothDevice f : devices) {
            deviceAddress[cnt++] = f.getAddress();
        }

        SpinnerAdapter adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, deviceAddress);
        adapterDevice.SetTextSize(16);
        binding.spinDevice.setAdapter(adapterDevice);
        binding.spinDevice.setOnItemSelectedListener(onSelChanged);

        Log.d(TAG, "UpdateSerial Stop");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnect1chBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CheckPermission();
        InitControl();

        Intent intent = new Intent(
                Connect_1chActivity.this, // 현재 화면
                BTService.class); // 다음넘어갈 컴퍼넌트

        bindService(intent, // intent 객체
                conn, // 서비스와 연결에 대한 정의
                Context.BIND_AUTO_CREATE);


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

        binding.editAge.addTextChangedListener(textWatcher);


        binding.backContainer.setOnClickListener( v -> finish());

        binding.btnMale.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성

            infoGender = true;
            binding.rbMale.setChecked(true);
            binding.rbFemale.setChecked(false);
        });

        binding.btnFemale.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성

            infoGender = false;
            binding.rbMale.setChecked(false);
            binding.rbFemale.setChecked(true);
        });

        binding.btnDeviceOpen.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성
            DeviceOpen();
        });

        binding.btnDeviceClose.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성
            DeviceClose();
        });


        binding.btnMale.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성
            infoGender = true;
            binding.rbMale.setChecked(true);
            binding.rbFemale.setChecked(false);
        });

        binding.btnFemale.setOnClickListener(v -> {
            // TextView 클릭될 시 할 코드작성
            infoGender = false;
            binding.rbMale.setChecked(false);
            binding.rbFemale.setChecked(true);
        });


        binding.btnDeviceMeasure.setOnClickListener(v -> {
            isVaild();
        });

        binding.searchDevice.setOnClickListener( v -> {
            /*Intent intent = new Intent();
            String sPath =
                    getExternalFilesDir(null) + "/I-Motion Lab";*/
                    //Environment.getExternalStorageState() + "/I-Motion Lab/";

            /*Uri uri = Uri.parse(sPath);
            intent.setAction(Intent.ACTION_VIEW);*/
            //intent.setDataAndType(uri, "*/*");
            //startActivity(intent);

            Intent intent = new Intent(this, ScanActivity.class);
            intent.putExtra("pairedArray", deviceAddress);
            startActivity(intent);
        });

    }

    private void isVaild(){

        if (isState[0] == STATE_NONE){
            showToast("기기를 연결해주세요");
            return;
        }else if (TextUtils.isEmpty(binding.editName.getText().toString()) || binding.editName.getText().toString().length() < 1){
            showToast("측정자이름을 입력해주세요");
            return;
        }

        if (TextUtils.isEmpty(binding.editHeight.getText().toString()) || binding.editHeight.getText().toString().length() < 1){
            binding.editHeight.setText("100");
            //showToast("측정자 키를 입력해주세요");
        }

        if (TextUtils.isEmpty(binding.editAge.getText().toString()) || binding.editAge.getText().toString().length() < 1){
            binding.editAge.setText("19000101");
            //showToast("측정자 생년월일을 입력해주세요");
        }

        if (TextUtils.isEmpty(binding.editWeight.getText().toString()) || binding.editWeight.getText().toString().length() < 1){
            binding.editWeight.setText("10");
            //showToast("측정자 몸무게를 입력해주세요");
        }

        if (!binding.rbMale.isChecked() && !binding.rbFemale.isChecked()){
            binding.rbMale.setChecked(true);
            //showToast("측정자 성별을 선택해주세요");
        }/*else {
            moveMeasure();
        }*/
        moveMeasure();

    }

    private void moveMeasure(){
        infoGender = binding.rbMale.isChecked();
        Intent intent = new Intent(Connect_1chActivity.this, MeasureOneActivity.class);
        UserInfo.getInstance().name = binding.editName.getText().toString();
        UserInfo.getInstance().height = binding.editHeight.getText().toString();
        UserInfo.getInstance().weight = binding.editWeight.getText().toString();
        UserInfo.getInstance().birth = binding.editAge.getText().toString();
        UserInfo.getInstance().memo = binding.editMemo.getText().toString();
        UserInfo.getInstance().gender = infoGender;
        /*intent.putExtra("name", binding.editName.getText().toString());
        intent.putExtra("height", binding.editHeight.getText().toString());
        intent.putExtra("weight", binding.editWeight.getText().toString());
        intent.putExtra("birth", binding.editAge.getText().toString());
        intent.putExtra("memo", binding.editMemo.getText().toString());
        intent.putExtra("gender", infoGender);*/
        startActivity(intent);
    }

    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void DeviceOpen() {
        if (isState[0] == STATE_NONE){
            String deviceAddress="";
            Object selObj=null;

            selObj=binding.spinDevice.getSelectedItem();
            if (selObj!=null) deviceAddress=selObj.toString();


            if (isState[0] != STATE_NONE && deviceAddress.equals(btService.getDeviceNum(0))) return;

            if (btService!=null) btService.Connect(deviceAddress,0);
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

    //생년월일 자동 하이픈 채우기
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
                binding.editAge.getText().delete(s.length() - 1, s.length());
                return;
            }

            int _afterLenght = s.length();

            // 삭제 중
            if (_beforeLenght > _afterLenght) {
                // 삭제 중에 마지막에 -는 자동으로 지우기
                if (s.toString().endsWith("-")) {
                    binding.editAge.setText(s.toString().substring(0, s.length() - 1));
                }
            }
            // 입력 중
            else if (_beforeLenght < _afterLenght) {
                if (_afterLenght == 5 && !s.toString().contains("-")) {
                    binding.editAge.setText(s.toString().subSequence(0, 4) + "-" + s.toString().substring(4, s.length()));
                } else if (_afterLenght == 8) {
                    binding.editAge.setText(s.toString().subSequence(0, 7) + "-" + s.toString().substring(7, s.length()));
                }
            }
            binding.editAge.setSelection(binding.editAge.length());

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
                            if (D) Log.d(TAG, "Device Connect");
                            isState[deviceIndex] = STATE_CONNECTED;
                            //isMeasure[deviceIndex] = false;
                            break;
                        case STATE_CONNECTING:
                            isState[deviceIndex] = STATE_CONNECTING;
                            break;
                        case STATE_NONE:
                            isState[deviceIndex] = STATE_NONE;
                            if (D) Log.d(TAG, "Device Close");
                            break;
                    }
                    //StopSave(deviceIndex);
                    UpdateUI();
                    break;
                case MESSAGE_DEVICE_INFO:
                    battLevel[deviceIndex] = ((float) msg.arg1) / 1000.0f;

                    if (msg.arg2 == 1) //배터리 전원 사용
                    {
                        powerStatus[deviceIndex] = POWER_BATT;
                    } else if (msg.arg2 == 2)   //USB 전원사용 - 완충됨
                    {
                        powerStatus[deviceIndex] = POWER_USB_FULL_CHARGE;
                    } else if (msg.arg2 == 3)   //USB 전원사용 - 충전중
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

                    int batt = Math.round(battLevel[0] * 20);


                    binding.tvBatt.setText(batt + "%");

                    if (battLevel[0] >= 4.0) {
                        //100
                        batt = 100;
                    }else {
                        //99이하
                        int abc = 90;
                        batt = (int) ((battLevel[0] - 3.0) * 100);

                    }

                    if (battLevel[0] >= 4.0) {
                        //4
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));

                    } else if (battLevel[0] >= 3.55) {
                        //3
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));

                    } else if (battLevel[0] >= 3.49) {
                        //2
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));

                    }else if (battLevel[0] >= 3.45 || battLevel[0] >= 3.25) {
                        //1
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));

                    }else if (battLevel[0] <= 3.25){
                        //0
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));
                    }

                    binding.tvBatt.setText(batt + "%");

                    /*if (batt > 90) {
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));
                    } else if (batt > 75)
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));
                    else if (batt > 50)
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));
                    else if (batt > 25)
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));
                    else if (batt > 10)
                        binding.imgBatt.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));*/

                    /*int batt = Math.round(battValue * 20);
                    updateBatt(batt);*/
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
            // 다시 보지 않기 버튼을 만드려면 이 부분에 바로 요청을 하도록 하면 됨 (아래 else{..} 부분 제거)
//             ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_CAMERA);

            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨 -> else{..}의 요청으로 넘어감
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한이 거부되었습니다.\n측정결과가 저장되지 않습니다.\n사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                        .setNeutralButton("설정", (dialogInterface, i) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setPositiveButton("확인", (dialogInterface, i) -> {
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
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //Log.wtf("퍼미션", "11111111");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 11);
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
            //tv.setTextColor((getResources().getColor(R.color.SpinTextDisable));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, txtSize);
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