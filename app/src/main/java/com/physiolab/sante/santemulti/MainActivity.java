package com.physiolab.sante.santemulti;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ST_DATA_PROC;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Activity-Main";
    private static final boolean D = true;

    private BTService btService = null;
    private boolean isService = false;
    private DataSaveThread[] saveThread = new DataSaveThread[2];

    private int[] isState = new int[]{BTService.STATE_NONE,BTService.STATE_NONE};

    private float[] battLevel = new float[] {0.0f,0.0f};
    private int[] powerStatus = new int[] {BTService.POWER_NONE,BTService.POWER_NONE};
    private boolean[] isMeasure = new boolean[] {false,false};
    private boolean[] isSave = new boolean[] {false,false};
    private int[] valueUpdateCnt=new int[]{10,10};
    private int[] battUpdateCnt=new int[]{3,3};

    private int[] recvPktCnt = new int[2];

    private final int PERMISSION_STORAGE=1;
    private final int PERMISSION_BLUETOOTH=2;

    private Button[] btn_connect = new Button[2];
    private Spinner[] spinDevice = new Spinner[2];
    private TextView[] txtConnStatus = new TextView[2];
    private TextView[] txtPowerStatus = new TextView[2];
    private TextView[] txtBattLevel = new TextView[2];
    private Button[] btn_acquire = new Button[2];
    private Button[] btn_record = new Button[2];

    private TextView[] txtEMGSignal = new TextView[2];
    private TextView[] txtEMGRMS = new TextView[2];
    private TextView[] txtAccX = new TextView[2];
    private TextView[] txtAccY = new TextView[2];
    private TextView[] txtAccZ = new TextView[2];
    private TextView[] txtGyroX = new TextView[2];
    private TextView[] txtGyroY = new TextView[2];
    private TextView[] txtGyroZ = new TextView[2];

    private GraphView[] graphView = new GraphView[2];

    private Button btn_connect_all;
    private Button btn_acquire_all;

    ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            if (D) Log.d(TAG,"onServiceConnected");
            BTService.BTBinder mb = (BTService.BTBinder) service;
            btService = mb.getService(); // 서비스가 제공하는 메소드 호출하여
            btService.Init();
            btService.SetMainHandler(new MessageHandler(0),0);
            btService.SetMainHandler(new MessageHandler(1),1);

            isService = true;
            // 서비스쪽 객체를 전달받을수 있슴

            BTCheck();
        }
        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            if (D) Log.d(TAG,"onServiceDisconnected");
            isService = false;
        }
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        CheckPermission();
        InitControl();
        UpdateUI();

        Intent intent = new Intent(
                MainActivity.this, // 현재 화면
                BTService.class); // 다음넘어갈 컴퍼넌트

        bindService(intent, // intent 객체
                conn, // 서비스와 연결에 대한 정의
                Context.BIND_AUTO_CREATE);

        if (D) Log.d(TAG,"onCreate");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        StopSave(0);
        StopSave(1);

        stopTimerTask();
    }

    private void CheckPermission()
    {
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
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            }
        }
        else
        {
            CreateFolder();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    CreateFolder();
                } else {
                    Toast.makeText(MainActivity.this, "측정결과가 저장되지 않습니다.\n저장소 사용권한을 활성화 하셔야 합니다.", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (D) Log.d(TAG, "onActivityResult " + resultCode);

        switch (requestCode) {
            case BTService.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    UpdateSerial();
                } else {
                    if (D) Log.d(TAG, "Bluetooth is not enabled");
                    finish();
                }
                break;
            default:
                //if (D) Log.d(TAG, "Unexpected value: " + requestCode);
        }
    }

    public void BTCheck()
    {
        if (btService.getDeviceState())    //장치가 블루투스를 지원하는지 확인
        {
            if (btService.enableBlueTooth(this))
            {
                UpdateSerial();
            }
        }
        else
        {
            if (D) Log.d(TAG, "Bluetooth is not supported");
            finish();
        }
    }

    private class MessageHandler extends Handler
    {
        private int deviceIndex=-1;

        public MessageHandler(int index)
        {
            super();
            deviceIndex=index;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case BTService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            if (D) Log.d(TAG,"Device Connect");
                            isState[deviceIndex] = BTService.STATE_CONNECTED;
                            isMeasure[deviceIndex] = false;
                            break;
                        case BTService.STATE_CONNECTING:
                            isState[deviceIndex] = BTService.STATE_CONNECTING;
                            break;
                        case BTService.STATE_NONE:
                            isState[deviceIndex] = BTService.STATE_NONE;
                            if (D) Log.d(TAG,"Device Close");
                            break;
                    }
                    StopSave(deviceIndex);
                    UpdateUI();
                    break;
                case BTService.MESSAGE_DEVICE_INFO:
                    battLevel[deviceIndex] = ((float)msg.arg1)/1000.0f;

                    if (msg.arg2==1) //배터리 전원 사용
                    {
                        powerStatus[deviceIndex] = BTService.POWER_BATT;
                    }
                    else if (msg.arg2==2)   //USB 전원사용 - 완충됨
                    {
                        powerStatus[deviceIndex] = BTService.POWER_USB_FULL_CHARGE;
                    }
                    else if (msg.arg2==3)   //USB 전원사용 - 충전중
                    {
                        powerStatus[deviceIndex] = BTService.POWER_USB_CHARGING;
                    }
                    battUpdateCnt[deviceIndex]--;
                    if (battUpdateCnt[deviceIndex]<=0) {
                        UpdateUI();
                        battUpdateCnt[deviceIndex]=3;
                    }

                    break;

                case BTService.MESSAGE_DATA_RECEIVE:
                    while(true)
                    {
                        ST_DATA_PROC data = new ST_DATA_PROC();
                        byte ret  = btService.GetData(data,deviceIndex);
                        if (ret!=1)
                        {
                            break;
                        }
                        else
                        {
                            valueUpdateCnt[deviceIndex]--;
                            if (valueUpdateCnt[deviceIndex]<=0) {
                                txtEMGSignal[deviceIndex].setText(String.format("Signal : %.2f",data.Filted[0]));
                                txtEMGRMS[deviceIndex].setText(String.format("RMS : %.2f",data.RMS[0]));

                                txtAccX[deviceIndex].setText(String.format("X : %.2f",data.Acc[0][0]));
                                txtAccY[deviceIndex].setText(String.format("Y : %.2f",data.Acc[1][0]));
                                txtAccZ[deviceIndex].setText(String.format("Z : %.2f",data.Acc[2][0]));

                                txtGyroX[deviceIndex].setText(String.format("X : %.2f",data.Gyro[0][0]));
                                txtGyroY[deviceIndex].setText(String.format("Y : %.2f",data.Gyro[1][0]));
                                txtGyroZ[deviceIndex].setText(String.format("Z : %.2f",data.Gyro[2][0]));
                                valueUpdateCnt[deviceIndex]=10;
                            }

                            recvPktCnt[deviceIndex]++;

                            if (isSave[deviceIndex]) saveThread[deviceIndex].Add(data);
                        }
                    }

                    break;

                case BTService.MESSAGE_COMMAND_RECEIVE:
                    switch (msg.arg1) {
                        case BTService.CMD_BLU_DEV_INITIALIZE:
                            if (D) Log.d(TAG,"Received Initialize COMMAND");
                            break;
                        case BTService.CMD_BLU_D2P_DATA_REALTIME_START :
                            if (D) Log.d(TAG,"Received Data Transfer Start COMMAND");
                            isMeasure[deviceIndex] = true;
                            UpdateUI();
                            break;
                        case BTService.CMD_BLU_D2P_DATA_STOP :
                            if (D) Log.d(TAG,"Received Data Transfer Stop COMMAND");
                            StopSave(deviceIndex);
                            isMeasure[deviceIndex] = false;
                            UpdateUI();
                            break;
                        case BTService.CMD_BLU_COMM_START :
                            if (D) Log.d(TAG,"Received Communication Start COMMAND");
                            break;
                    }
                    break;

                case BTService.MESSAGE_DATA_OVERFLOW:
                    if (msg.arg1==0)
                    {
                        if (D) Log.d(TAG,"Device Queue OverFlow");
                    }
                    else
                    {
                        if (D) Log.d(TAG,"Receive Queue OverFlow");
                    }
                    StopSave(deviceIndex);
                    if (btService!=null) btService.Close(deviceIndex);
                    break;

            }
        }
    }

    private void InitControl()
    {
        btn_connect[0] = findViewById(R.id.btn_connect_0);
        btn_connect[1] = findViewById(R.id.btn_connect_1);

        btn_connect[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TextView 클릭될 시 할 코드작성
                if (isState[0]==BTService.STATE_NONE)
                {
                    String deviceAddress="";
                    Object selObj=null;

                    selObj=spinDevice[0].getSelectedItem();
                    if (selObj!=null) deviceAddress=selObj.toString();


                    if (isState[1]!=BTService.STATE_NONE && deviceAddress==btService.getDeviceNum(1)) return;

                    if (btService!=null) btService.Connect(deviceAddress,0);
                }
                else
                {
                    if (btService!=null) btService.Close(0);
                }

                UpdateUI();
            }
        });

        btn_connect[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TextView 클릭될 시 할 코드작성
                if (isState[1]==BTService.STATE_NONE)
                {
                    String deviceAddress="";
                    Object selObj=null;

                    selObj=spinDevice[1].getSelectedItem();
                    if (selObj!=null) deviceAddress=selObj.toString();


                    if (isState[0]!=BTService.STATE_NONE && deviceAddress==btService.getDeviceNum(0)) return;

                    if (btService!=null) btService.Connect(deviceAddress,1);
                }
                else
                {
                    if (btService!=null) btService.Close(1);
                }

                UpdateUI();
            }
        });

        spinDevice[0] = (Spinner) findViewById(R.id.spin_device_0);
        spinDevice[1] = (Spinner) findViewById(R.id.spin_device_1);

        txtConnStatus[0] = findViewById(R.id.txt_connect_status_0);
        txtConnStatus[1] = findViewById(R.id.txt_connect_status_1);

        txtPowerStatus[0] = findViewById(R.id.txt_power_status_0);
        txtPowerStatus[1] = findViewById(R.id.txt_power_status_1);

        txtBattLevel[0] = findViewById(R.id.txt_batt_level_0);
        txtBattLevel[1] = findViewById(R.id.txt_batt_level_1);

        btn_acquire[0] = findViewById(R.id.btn_acquire_0);
        btn_acquire[1] = findViewById(R.id.btn_acquire_1);

        btn_record[0] = findViewById(R.id.btn_record_0);
        btn_record[1] = findViewById(R.id.btn_record_1);

        btn_acquire[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMeasure[0])
                {
                    if (btService!=null) btService.Start(0);
                    graphView[0].Clear();
                    recvPktCnt[0]=0;
                }
                else
                {
                    if (btService!=null) btService.Stop(0);
                }
            }
        });

        btn_acquire[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMeasure[1])
                {
                    if (btService!=null) btService.Start(1);
                    graphView[1].Clear();
                    recvPktCnt[1]=0;
                }
                else
                {
                    if (btService!=null) btService.Stop(1);
                }
            }
        });

        btn_record[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSave[0])
                {
                    StartSave(0);
                }
                else
                {
                    StopSave(0);
                }
                UpdateUI();
            }
        });

        btn_record[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSave[1])
                {
                    StartSave(1);
                }
                else
                {
                    StopSave(1);
                }
                UpdateUI();
            }
        });
        
        txtEMGSignal[0] = findViewById(R.id.txt_emg_signal_0);
        txtEMGRMS[0] = findViewById(R.id.txt_emg_rms_0);
        txtAccX[0] = findViewById(R.id.txt_acc_x_0);
        txtAccY[0] = findViewById(R.id.txt_acc_y_0);
        txtAccZ[0] = findViewById(R.id.txt_acc_z_0);
        txtGyroX[0] = findViewById(R.id.txt_gyro_x_0);
        txtGyroY[0] = findViewById(R.id.txt_gyro_y_0);
        txtGyroZ[0] = findViewById(R.id.txt_gyro_z_0);

        txtEMGSignal[1] = findViewById(R.id.txt_emg_signal_1);
        txtEMGRMS[1] = findViewById(R.id.txt_emg_rms_1);
        txtAccX[1] = findViewById(R.id.txt_acc_x_1);
        txtAccY[1] = findViewById(R.id.txt_acc_y_1);
        txtAccZ[1] = findViewById(R.id.txt_acc_z_1);
        txtGyroX[1] = findViewById(R.id.txt_gyro_x_1);
        txtGyroY[1] = findViewById(R.id.txt_gyro_y_1);
        txtGyroZ[1] = findViewById(R.id.txt_gyro_z_1);

        graphView[0] = (GraphView)findViewById(R.id.view_graph_0);
        graphView[1] = (GraphView)findViewById(R.id.view_graph_1);

        graphView[0].Clear();
        graphView[1].Clear();

        btn_connect_all = findViewById(R.id.btn_connect_all);
        btn_connect_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isState[0]==BTService.STATE_NONE)
                {
                    String deviceAddress="";
                    Object selObj=null;

                    selObj=spinDevice[0].getSelectedItem();
                    if (selObj!=null) deviceAddress=selObj.toString();

                    if (isState[1]==BTService.STATE_NONE || deviceAddress!=btService.getDeviceNum(1))
                    {
                        if (btService!=null) btService.Connect(deviceAddress,0);
                    }
                }

                if (isState[1]==BTService.STATE_NONE)
                {
                    String deviceAddress="";
                    Object selObj=null;

                    selObj=spinDevice[1].getSelectedItem();
                    if (selObj!=null) deviceAddress=selObj.toString();

                    if (isState[0]==BTService.STATE_NONE || deviceAddress!=btService.getDeviceNum(0))
                    {
                        if (btService!=null) btService.Connect(deviceAddress,1);
                    }
                }

                UpdateUI();
            }
        });

        btn_acquire_all = findViewById(R.id.btn_acquire_all);
        btn_acquire_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0;i<2;i++)
                {
                    if (isState[i]==BTService.STATE_CONNECTED && !isMeasure[i])
                    {
                        if (btService!=null) btService.Start(i);
                        graphView[i].Clear();
                        recvPktCnt[0]=0;
                    }
                }
                UpdateUI();
            }
        });

        startTimerTask();
    }

    private void UpdateUI()
    {
        for(int i=0;i<2;i++)
        {
            if (isState[i]==BTService.STATE_NONE)
            {
                btn_connect[i].setText("Connect");
                spinDevice[i].setEnabled(true);

                txtConnStatus[i].setText("Status : Disconnect");
                txtPowerStatus[i].setText("Power : -");
                txtBattLevel[i].setText("Batt. Level : -");

                btn_acquire[i].setText("Acquire Start");
                btn_acquire[i].setEnabled(false);
                btn_record[i].setText("Record Start");
                btn_record[i].setEnabled(false);
            }
            else if (isState[i]==BTService.STATE_CONNECTING)
            {
                btn_connect[i].setText("Disconnect");
                spinDevice[i].setEnabled(false);

                txtConnStatus[i].setText("Status : Connecting");
                txtPowerStatus[i].setText("Power : -");
                txtBattLevel[i].setText("Batt. Level : -");

                btn_acquire[i].setText("Acquire Start");
                btn_acquire[i].setEnabled(false);
                btn_record[i].setText("Record Start");
                btn_record[i].setEnabled(false);
            }
            else if (isState[i]==BTService.STATE_CONNECTED)
            {
                btn_connect[i].setText("Disconnect");
                spinDevice[i].setEnabled(false);

                txtConnStatus[i].setText("Status : Connect");
                switch (powerStatus[i])
                {
                    case BTService.POWER_BATT:
                        txtPowerStatus[i].setText("Power : Battery");
                        break;
                    case BTService.POWER_USB_CHARGING:
                        txtPowerStatus[i].setText("Power : USB-Charging");
                        break;
                    case BTService.POWER_USB_FULL_CHARGE:
                        txtPowerStatus[i].setText("Power : USB-Full Charge");
                        break;
                    default:
                        txtPowerStatus[i].setText("Power : -");
                        break;
                }

                txtBattLevel[i].setText(String.format("Batt. Level : %.2fV",battLevel[i]));

                if (!isMeasure[i])
                {
                    btn_acquire[i].setText("Acquire Start");
                    btn_acquire[i].setEnabled(true);
                    btn_record[i].setText("Record Start");
                    btn_record[i].setEnabled(false);
                }
                else
                {
                    btn_acquire[i].setText("Acquire Stop");
                    btn_acquire[i].setEnabled(true);

                    if (!isSave[i])
                    {
                        btn_record[i].setText("Record Start");
                        btn_record[i].setEnabled(true);
                    }
                    else
                    {
                        btn_record[i].setText("Record Stop");
                        btn_record[i].setEnabled(true);
                    }

                }

            }
        }
    }

    private void UpdateSerial() {
        Log.d(TAG, "UpdateSerial Start");

        Set<BluetoothDevice> devices = btService.GetPairedDeviceList();
        String[] deviceAddress = new String[devices.size()];
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

        SpinnerAdapter adapterDevice = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, deviceAddress);
        adapterDevice.SetTextSize(16);
        spinDevice[0].setAdapter(adapterDevice);
        spinDevice[0].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length>0) spinDevice[0].setSelection(0);

        spinDevice[1].setAdapter(adapterDevice);
        spinDevice[1].setOnItemSelectedListener(onSelChanged);
        if (deviceAddress.length>1) spinDevice[1].setSelection(1);
        else if (deviceAddress.length>1) spinDevice[1].setSelection(1);

        Log.d(TAG, "UpdateSerial Stop");
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
            //tv.setTextColor((getResources().getColor(R.color.SpinTextDisable));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,txtSize);
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
            //tv.setTextColor(getResources().getColor(R.color.SpinTextEnable));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,txtSize);
            return convertView;
        }
    }

    private TimerTask timerTask;
    private Timer timer = new Timer();
    private long prevTime=0;

    private void startTimerTask()
    {
        stopTimerTask();

        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                long curTime = System.currentTimeMillis();
                float diffTime = (float)(curTime-prevTime)/1000.0f;
                prevTime=curTime;
                for(int i=0;i<2;i++)
                {
                    if (isState[i]==BTService.STATE_CONNECTED && isMeasure[i]) graphView[i].Add((float)recvPktCnt[i]/diffTime);

                    recvPktCnt[i]=0;
                }
            }
        };
        timer.schedule(timerTask,0 ,1000);
    }

    private void stopTimerTask()
    {
        if(timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void CreateFolder()
    {
        File f = new File(Environment.getExternalStorageDirectory(),"/SanteMulti/");

        if (f.exists())
        {
            if (f.isDirectory()==false)
            {
                return;
            }
        }
        else
        {
            try {
                boolean ret = f.mkdirs();
                if (ret) Log.d(TAG,"Folder Create");
                else Log.d(TAG,"Folder Not Create");
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return;
            }
        }
    }

    private void StartSave(int index)
    {
        StopSave(index);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"저장소 접근 권한이 없어서\n데이터가 저장되지 않았습니다.",Toast.LENGTH_SHORT).show();
            return;
        }

        saveThread[index] = new DataSaveThread(new Date(),index);
        saveThread[index].start();
        isSave[index]=true;
    }

    private void StopSave(int index)
    {
        isSave[index]=false;
        if (saveThread[index]!=null)
        {
            saveThread[index].cancle();

            if (saveThread[index].isAlive())
            {
                try {
                    saveThread[index].join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            saveThread[index]=null;
        }
    }
}
