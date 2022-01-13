package com.physiolab.sante.BlueToothService;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.physiolab.sante.ST_DATA_PROC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static android.net.sip.SipErrorCode.SOCKET_ERROR;

public class BTService extends Service {

    static {
        System.loadLibrary("sante");
    }

    public native long CreateBTSocketFwk(BTService ptr,int index);  //해당 index에 대해서 Class의 object를 생성하고 포인터값(ptr)을 돌려받는다.
    public native void ReleaseBTSocketFwk(long ptr);    //object를 해제한다.
    public native void SetJNIMainHandler(long ptr,Handler h); //생성된 object가 메세지를 전달할 Handler를 지정한다.
    public native void SetJNIMonitorHandler(long ptr,Handler h);
    public native void DeviceOpend(long ptr);   //장치가 연결되었을때의 초기화 작업을 요청한다.
    public native void DeviceClosed(long ptr);  //장치가 연결이 끊어졌음을 알린다.
    public native void AddCmd(long ptr,byte Cmd, int CmdParam); //장치로 전송할 명령어와 파라미터값을 추가한다. 전달된 명령어는 큐에 추가되고 TimeProgress50m함수에서 장치에 전달된다.
    public native boolean TimeProgress50ms(long ptr);   //명령어 큐에 확인하고 장치로 전송한다.
    public native void DeviceDataIn(long ptr,byte[] buf, int len);  //장치에서 수신한 데이터를 해당 object로 전달한다.
    public native byte GetJNIData(long ptr,float[] data);
    public native void SetEMGFilterJNI(long ptr,int notch,int hpf,int lpf);
    public native void SetAccFilterJNI(long ptr,int hpf,int lpf);
    public native void SetGyroFilterJNI(long ptr,int hpf,int lpf);

    private final String TAG = "BTService";
    //private static final boolean D = false;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_INFO = 2;
    public static final int MESSAGE_DATA_RECEIVE = 3;
    public static final int MESSAGE_COMMAND_RECEIVE = 4;
    public static final int MESSAGE_DATA_OVERFLOW = 5;

    public static final int POWER_NONE = 0;
    public static final int POWER_BATT = 1;
    public static final int POWER_USB_FULL_CHARGE = 2;
    public static final int POWER_USB_CHARGING = 3;

    // 현재연결 상태에 대한 상수값
    public static final int STATE_NONE = 0;       // 연결되어있지 않음
    public static final int STATE_CONNECTING = 1; // 연결시도중
    public static final int STATE_CONNECTED = 2;  // 장치와 연결이 이루어진 상태

    public static final byte CMD_BLU_NONE = 0;		// 명령 없음

    //
    //* 통신 연결 직후 보내는 명령
    //
    public static final byte CMD_BLU_COMM_START = 1;		// 통신 시작 , ID 동기용 , 이후 다른 메세지 처리함
    public static final byte CMD_BLU_DEV_INITIALIZE = 2;	 // 장치 초기화 - 통신 관련 등 초기화
    //
    //* UART 데이터 통신 명령
    //
    // D2P 데이터 관련
    public static final byte CMD_BLU_D2P_DATA_STOP = 3;					// 장치가 송신 중지
    public static final byte CMD_BLU_D2P_DATA_REALTIME_START = 4;		// 장치가 ADC/RF In/Flash 재생 데이터 실시간 송신
    //  장치의 상태(State) 및 Flash 재생 기능 수행에 따라 데이터 종류 변경됨


    // Intent request code
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public static final int PACKET_SAMPLE_NUM = 40;
    public static final int SAMPLE_RATE = 2000;
    //public static final int SAMPLE_RATE = 580000;

    public static final int REQUEST_EMG_FILTER=0;
    public static final int REQUEST_Acc_FILTER=1;
    public static final int REQUEST_Gyro_FILTER=2;


    public static final int REQUEST_Time_RANGE=3;

    public static final int RESULT_CANCLE=-1;
    public static final int RESULT_OK=0;

    //public final static long Time_Offset = 621355968000000000L+9L*60L*60L*1000L*1000L*10L;
    private long Time_Offset = 621355968000000000L+9L*60L*60L*1000L*1000L*10L;


    // RFCOMM Protocol을 위한 UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String DeviceName = "TUG";

    //동시에 연결할 장치의 최대갯수
    private static final int MaxDeviceIndex = 4;

    //각 장치에 대해서 jni호출을 위한 포인터
    private long[] btFwk = new long[MaxDeviceIndex];

    private IBinder mBinder = new BTBinder();

    private BluetoothAdapter bluetoothAdapter; //블루투스를 제어하기 위한 어댑터
    private BluetoothDevice[] bluetoothDevice = new BluetoothDevice[MaxDeviceIndex];

    private Handler[] mHandler_main = new Handler[MaxDeviceIndex];   //메세지를 전달하기 위한 핸들러 클래스

    //각 장치의 접속과 데이터전송을 관리한 쓰레드
    private BTService.ConnectThread[] mConnectThread = new BTService.ConnectThread[MaxDeviceIndex];
    private BTService.ConnectedThread[] mConnectedThread = new BTService.ConnectedThread[MaxDeviceIndex];

    private int[] mState = new int[MaxDeviceIndex]; //현재의 연결상태를 가지는 변수

    private String[] mDevNum = new String[MaxDeviceIndex];  //연결된 장치의 주소값

    public static final int BT_SOCKET_RBUF_SIZE = 65536;    //수신버퍼의 크기

    private byte[] m_ReadBuf = new byte[BT_SOCKET_RBUF_SIZE];

    public BTService() {
    }

    public class BTBinder extends Binder {
        public BTService getService() { // 서비스 객체를 리턴
            return BTService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    //안드로이드 기본 블루투스관리자 할당

        for(int i=0;i<MaxDeviceIndex;i++)
        {
            mState[i]=STATE_NONE;  //현재 상태를 연결이 끓어짐으로 설정

            mDevNum[i] = "";

            btFwk[i] = CreateBTSocketFwk(this,i);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //if (D) Log.d(TAG,"onDestroy");
        for(int i=0;i<MaxDeviceIndex;i++)
        {
            ReleaseBTSocketFwk(btFwk[i]);
            Close(i);
        }
    }

    public void SetMainHandler(Handler h,int index)
    {
        if (index>=MaxDeviceIndex) return;
        mHandler_main[index] = h;
        SetJNIMainHandler(btFwk[index],h);
    }

    public void SetMonitorHandler(Handler h, int index) {
        if (index>=MaxDeviceIndex) return;
        mHandler_main[index] = h;
        SetJNIMonitorHandler(btFwk[index], h);
    }

    private void SendMessage(int msg,int arg1,int arg2,int index)
    {
        if (index>=MaxDeviceIndex) return;
        try
        {
            if (mHandler_main!=null) mHandler_main[index].obtainMessage(msg, arg1, arg2).sendToTarget();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //현재 연결상태를 반환하는 함수
    public int getState(int index)
    {
        if (index>=MaxDeviceIndex) return STATE_NONE;
        return mState[index];
    }

    //현재 연결된 장치의 주소값 반환
    public String getDeviceNum(int index)
    {
        if (index>=MaxDeviceIndex) return "";
        return mDevNum[index];
    }

    //현재의 연결상태를 설정함
    private void setState(int state,int index) {

        mState[index] = state;

        // Give the new state to the Handler so the UI Activity can update
        SendMessage(MESSAGE_STATE_CHANGE, state, -1,index);
    }

    //장치가 블루투스를 지원하는지 여부를 반환함
    public boolean getDeviceState()
    {

        if(bluetoothAdapter == null)
        {

            return false;
        }
        else
        {

            return true;
        }
    }

    //사용자가 블루투스를 사용하도록 요청한다.
    public boolean enableBlueTooth(Activity ac)
    {
        if (bluetoothAdapter==null) //장치가 블루투스를 지원하지 않는 경우
        {
            return false;
        }


        if(!bluetoothAdapter.isEnabled())
        {
            // 사용자에게 블루투스의 사용허가를 요청한다.
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ac.startActivityForResult(i, REQUEST_ENABLE_BT); //상위 액티비티에서 결과를 받아볼수 있음
            return false;
        }
        else
        {
            return true;
        }
    }

    //페어링된 장치들중에서 원하는 이름을 가진 장치목록을 반환한다.
    public Set<BluetoothDevice> GetPairedDeviceList()
    {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<BluetoothDevice> motiveDevices = new HashSet<BluetoothDevice>();

        for(BluetoothDevice device : pairedDevices)
        {
            if (device.getName().equals(DeviceName)) motiveDevices.add(device);
        }
        return motiveDevices;
    }

    public void Init()
    {

        for(int i=0;i<MaxDeviceIndex;i++) {

            // Cancel the thread that completed the connection
            if (mConnectThread[i] != null) {
                mConnectThread[i].cancel();
                mConnectThread[i] = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread[i] != null) {
                mConnectedThread[i].cancel();
                mConnectedThread[i] = null;
            }

            setState(STATE_NONE, i);
        }
    }

    //주소값을 이용해서 해당번호에 대해서 쓰레드를 생성해서 연결을 시도함
    public void Connect(String deviceNumber,int index)
    {
        if (index>=MaxDeviceIndex) return;

        if (mConnectThread[index] !=null) {mConnectThread[index].cancel(); mConnectThread[index] = null;}
        if (mConnectedThread[index] != null) {mConnectedThread[index].cancel(); mConnectedThread[index] = null;}

        // Get the BluetoothDevice object
        bluetoothDevice[index] = bluetoothAdapter.getRemoteDevice(deviceNumber);
        mDevNum[index] = deviceNumber;

        // Start the thread to connect with the given device
        mConnectThread[index] = new BTService.ConnectThread(bluetoothDevice[index],index);
        mConnectThread[index].start();

        setState(STATE_CONNECTING,index);
    }

    //이전에 연결되었던 장치에 대해서 연결을 시도함
    private void Connect(int index)
    {
        if (index>=MaxDeviceIndex) return;

        if (mConnectThread[index] !=null) {mConnectThread[index].cancel(); mConnectThread[index] = null;}
        if (mConnectedThread[index] != null) {mConnectedThread[index].cancel(); mConnectedThread[index] = null;}

        // Start the thread to connect with the given device
        mConnectThread[index] = new BTService.ConnectThread(bluetoothDevice[index],index);
        mConnectThread[index].start();

        setState(STATE_CONNECTING,index);
    }

    //장치가 연결되었을시 초기화 작업을 처리
    private synchronized void Connected(BluetoothSocket socket,int index) {

        // Cancel the thread that completed the connection
        if (mConnectThread[index] != null) {mConnectThread[index].cancel(); mConnectThread[index] = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread[index] != null) {mConnectedThread[index].cancel(); mConnectedThread[index] = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread[index] = new BTService.ConnectedThread(socket,index);
        mConnectedThread[index].start();

        DeviceOpend(btFwk[index]);

        setState(STATE_CONNECTED,index);
    }

    //데이터획득 명령을 보냄
    public void Start(int index)
    {
        if (index>=MaxDeviceIndex) return;

        AddCmd(btFwk[index], CMD_BLU_D2P_DATA_REALTIME_START, 0);
    }

    //데이터획득 정지명령을 보냄
    public void Stop(int index)  {
        if (index>=MaxDeviceIndex) return;

        AddCmd(btFwk[index],CMD_BLU_D2P_DATA_STOP, 0);
    }

    //요구하는 장치(index)에서 데이터를 가져옴
    public byte GetData(ST_DATA_PROC data, int index)
    {
        if (index>=MaxDeviceIndex) return -1;

        float[] arr = new float[PACKET_SAMPLE_NUM*5+(PACKET_SAMPLE_NUM/10)*6];
        byte ret = GetJNIData(btFwk[index],arr);
        data.SetData(arr);
        return (byte)ret;
    }

    //요구하는 장치(index)를 통해서 데이터를 전송함.
    //jni 라이브러리에서 호출됨.
    public boolean Write(byte[] buf,int len,int index)
    {
        if (index>=MaxDeviceIndex) return false;

        // Create temporary object
        BTService.ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState[index] != STATE_CONNECTED || mConnectedThread[index]==null) return false;
            r = mConnectedThread[index];
        }
        // Perform the write unsynchronized
        return r.write(buf,len);
    }

    //요구하는 장치(index)의 연결을 끊음.
    public void Close(int index)
    {
        if (index>=MaxDeviceIndex) return;


        // Cancel the thread that completed the connection
        if (mConnectThread[index] != null) {mConnectThread[index].cancel(); mConnectThread[index] = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread[index] != null) {mConnectedThread[index].cancel(); mConnectedThread[index] = null;}

        setState(STATE_NONE,index);
    }

    ///////////////////////////////////////////////
    //notch : 0-사용안함, 1-사용
    //hpf : 0-사용안함, 1-3Hz, 2-20Hz
    //lpf : 0-사용안함, 1-250Hz, 2-500Hz
    ///////////////////////////////////////////////
    public void SetEMGFilter(int notch,int hpf,int lpf,int index)
    {
        if (index>=MaxDeviceIndex) return;

        SetEMGFilterJNI(btFwk[index],notch,hpf,lpf);
    }

    ///////////////////////////////////////////////
    //hpf : 0-사용안함, 1-0.5Hz, 2-1Hz
    //lpf : 0-사용안함, 1-10Hz, 2-20Hz
    ///////////////////////////////////////////////
    public void SetAccFilter(int hpf,int lpf,int index)
    {
        if (index>=MaxDeviceIndex) return;

        SetAccFilterJNI(btFwk[index],hpf,lpf);
    }

    ///////////////////////////////////////////////
    //hpf : 0-사용안함, 1-0.5Hz, 2-1Hz
    //lpf : 0-사용안함, 1-10Hz, 2-20Hz
    ///////////////////////////////////////////////
    public void SetGyroFilter(int hpf,int lpf,int index)
    {
        if (index>=MaxDeviceIndex) return;

        SetGyroFilterJNI(btFwk[index],hpf,lpf);
    }

    //블루투스장치에 연결을 위한 쓰레드
    //연결에 성공시 초기화 함수 호출
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        //jni호출을 위한 index번호
        private int deviceIndex=-1;

        private boolean isLive=false;

        public ConnectThread(BluetoothDevice device,int index) {
            mmDevice = device;
            deviceIndex=index;
        }

        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            isLive=true;

            while(isLive)
            {

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    //if (D) Log.i(TAG, "Call connect socket");
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket.connect();
                } catch (IOException e) {
                    //connectionFailed();
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                    }
                }

                if (mmSocket.isConnected()) break;
                else
                {
                    try {
                        this.sleep(100);
                    } catch (InterruptedException e3) {
                        e3.printStackTrace();
                    }
                }
            }

            //if (D) Log.i(TAG, "Connected Device");
            // Reset the ConnectThread because we're done
            synchronized (BTService.this) {
                mConnectThread[deviceIndex] = null;
            }

            // Start the connected thread
            if (mmSocket.isConnected()) Connected(mmSocket,deviceIndex);
        }

        public void cancel() {
            isLive=false;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //연결된 블루투스 장치에 대해서 데이터송수신과 명령전송을 담당하는 쓰레드
    //안드로이드 코드상에서는 데이터를 바이트스트림으로 라이브러리에 전달하고
    //이외의 기능(packet 해석 및 데이터처리)은 라이브러리상에서 이루어진다.
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BufferedInputStream mmInStream;
        private final BufferedOutputStream mmOutStream;

        private int deviceIndex=-1;

        private boolean isLive=false;

        public ConnectedThread(BluetoothSocket socket,int index) {
            //if (D) Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                //if (D) Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = new BufferedInputStream(tmpIn,16);
            mmOutStream = new BufferedOutputStream(tmpOut,16);

            deviceIndex=index;
        }

        public void run() {
            // if (D) Log.i(TAG, "BEGIN mConnectedThread");

            long startTime = System.currentTimeMillis();
            long endTime = 0;
            long interval=0;

            isLive=true;

            // Keep listening to the InputStream while connected
            while (isLive) {
                endTime = System.currentTimeMillis();
                interval = endTime-startTime;

                if (interval>50)
                {
                    if (!TimeProgress50ms(btFwk[deviceIndex])) //명령어 큐처리
                    {
                        try {
                            mmSocket.close();
                        } catch (IOException e) {
                        }
                        isLive=false;
                        break;
                    }

                    startTime = endTime;
                }

                if (!ReceiveData())
                {
                    try {
                        mmSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isLive=false;
                    break;
                }
            }
            Close(deviceIndex);
            DeviceClosed(btFwk[deviceIndex]);
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public boolean write(byte[] buffer,int len) {
            try {
                if (isLive)
                {
                    mmOutStream.write(buffer,0, len);
                }
                return true;
            }
            catch (IOException e) {
                //if (D) Log.e(TAG, "Exception during write", e);
                return false;
            }
        }

        public void cancel() {
            isLive=false;
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private synchronized boolean ReceiveData()
        {
            int BytesReceived=0;
            int BytesAvailable=0;
            try
            {
                BytesAvailable = mmInStream.available();
                if (BytesAvailable>BT_SOCKET_RBUF_SIZE) BytesAvailable=BT_SOCKET_RBUF_SIZE;
                BytesReceived = mmInStream.read(m_ReadBuf,0,BytesAvailable);

            }
            catch (IOException e) {
                //if (D) Log.e(TAG, "disconnected", e);
                return false;
            }

            // 데이터 읽기 오류가 난 경우
            if (BytesReceived == SOCKET_ERROR)
            {
                return false;
            }

            if (BytesReceived>0)
            {
                //m_RcvConnectCheckCnt = COMM_BT_RCV_CONNECT_CHECK_CNT;	// 수신 시간 체크 카운터 초기화
                DeviceDataIn(btFwk[deviceIndex],m_ReadBuf,BytesReceived);
            }

            return true;
        }
    }
}
