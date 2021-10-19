package com.physiolab.santemulti;

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
import android.util.Log;
import android.util.Pair;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.santemulti.databinding.ActivityMainTestBinding;
import com.physiolab.santemulti.databinding.SearchDeviceItemBinding;

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

public class MainTestActivity extends AppCompatActivity {

    private static final String TAG = "Activity-MainTest";
    private static final boolean D = true;
    private ActivityMainTestBinding binding;


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

    private String[] deviceAddress;


    private final int[] recvPktCnt = new int[2];

    private final int PERMISSION_STORAGE = 1;
    private final int PERMISSION_BLUETOOTH = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        CheckPermission();
        InitControl();
        updateUI();


        Intent intent = new Intent(
                MainTestActivity.this, // 현재 화면
                BTService.class); // 다음넘어갈 컴퍼넌트

        bindService(intent, // intent 객체
                conn, // 서비스와 연결에 대한 정의
                Context.BIND_AUTO_CREATE);

        initView();

        if (D) Log.d(TAG, "onCreate");

    }


    private void CheckPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 다시 보지 않기 버튼을 만드려면 이 부분에 바로 요청을 하도록 하면 됨 (아래 else{..} 부분 제거)

            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨 -> else{..}의 요청으로 넘어감
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한이 거부되었습니다.\n측정결과가 저장되지 않습니다.\n사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            }
        }
    }

    private void InitControl() {
        btn_connect[0] = binding.btnRightDeviceOpen;
        btn_connect[1] = binding.btnLeftDeviceOpen;

        btn_disConnect[0] = binding.btnRightDeviceClose;
        btn_disConnect[1] = binding.btnLeftDeviceClose;


        btn_connect[0].setOnClickListener(v -> {
            Log.wtf("btn_connet 0" , "onCLICK");
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

        btn_disConnect[0].setOnClickListener( v -> {
            if (isState[0] == STATE_CONNECTED){
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

        btn_disConnect[1].setOnClickListener( v -> {
            if (isState[1] == STATE_CONNECTED){
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

        /*StopSave(0);
        StopSave(1);

        stopTimerTask();*/
    }

    ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            if (D) Log.d(TAG, "onServiceConnected");
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // 서비스가 제공하는 메소드 호출하여
            btService.Init();
            btService.SetMainHandler(new MessageHandler(0), 0);
            btService.SetMainHandler(new MessageHandler(1), 1);

            isService = true;
            // 서비스쪽 객체를 전달받을수 있슴

            BTCheck();
        }

        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            if (D) Log.d(TAG, "onServiceDisconnected");
            isService = false;
        }
    };

    public void BTCheck() {

        if (btService.getDeviceState())    //장치가 블루투스를 지원하는지 확인
        {
            if (btService.enableBlueTooth(this)) {
                UpdateSerial();
            }
        } else {
            if (D) Log.d(TAG, "Bluetooth is not supported");
            finish();
        }
    }


    private void UpdateSerial() {
        Log.d(TAG, "UpdateSerial Start");


        Set<BluetoothDevice> devices = btService.GetPairedDeviceList();
        deviceAddress = new String[devices.size()];
        int cnt = 0;

        AdapterView.OnItemSelectedListener onSelChanged = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
            BluetoothDevice f = it.next();
            deviceAddress[cnt++] = f.getAddress();
        }

        /*float div = (float) getResources().getInteger(R.integer.spin_device_size);*/
        //spinDevice[0] = findViewById(R.id.spin_right_device);
        SpinnerAdapter adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, deviceAddress);
        adapterDevice.SetTextSize(16);
        spinDevice[0].setAdapter(adapterDevice);
        spinDevice[0].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length > 0) spinDevice[0].setSelection(0);

        spinDevice[1].setAdapter(adapterDevice);
        spinDevice[1].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length > 1) spinDevice[1].setSelection(1);


        Log.d(TAG, "UpdateSerial Stop");
    }


    private void initView() {
        /*deviceAddress*/

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = null;
        ArrayList<Pair<String, String>> arrayList = new ArrayList<>();
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("2", "이번"));
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("1", "일번"));

        binding.deviceRe.setLayoutManager(new LinearLayoutManager(this));
        binding.deviceRe.setAdapter(new SearchDeviceAdapter(arrayList));
    }


    private boolean checkCoarseLocationPermassion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 11);

            return false;
        } else {
            return true;
        }
    }


    private class SearchDeviceAdapter extends RecyclerView.Adapter<SearchDeviceAdapter.ViewHolder> {

        private final ArrayList<Pair<String, String>> pairs;


        public SearchDeviceAdapter(ArrayList<Pair<String, String>> pairs) {
            this.pairs = pairs;
        }

        public void addList() {

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            SearchDeviceItemBinding binding = SearchDeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.holderBinding.txvAddress.setText(pairs.get(position).first);
            holder.holderBinding.txvName.setText(pairs.get(position).second);
        }

        @Override
        public int getItemCount() {
            return pairs.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private SearchDeviceItemBinding holderBinding;

            public ViewHolder(@NonNull SearchDeviceItemBinding holderBinding) {
                super(holderBinding.getRoot());
                this.holderBinding = holderBinding;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        for (int i = 0; i < 2; i++) {
            //Log.wtf("isSTATE i " + i, String.valueOf(isState[i]));
            if (isState[i] == STATE_NONE) {

                btn_connect[i].setEnabled(true);
                btn_disConnect[i].setEnabled(false);
                spinDevice[i].setEnabled(true);


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

                int batt = Math.round(battLevel[i] * 20);

                battTxv[i].setText(batt + "%");


                if (batt > 90)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_100));
                else if (batt > 75)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_75));
                else if (batt > 50)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_50));
                else if (batt > 25)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_25));
                else if (batt > 10)
                    battImv[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.battery_0));


            }
        }

        if (isState[0] == STATE_NONE && isState[1] == STATE_NONE){
            binding.btnDeviceMeasure.setEnabled(false);
        }else {
            binding.btnDeviceMeasure.setEnabled(true);
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
                    updateUI();
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
}
