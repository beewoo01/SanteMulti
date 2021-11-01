package com.physiolab.sante.santemulti;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ScanActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button pairBtn;
    private TextView findDevice;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothScanAdapter searchAdapter;
    private BluetoothDevice bluetoothDevice = null;
    private ArrayList<String> pairedList = null;
    private boolean isShowToast = true;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Intent intent = getIntent();
        pairedList = new ArrayList<>(Arrays.asList(intent.getStringArrayExtra("pairedArray")));

        pairBtn = findViewById(R.id.btn_pair);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        recyclerView = findViewById(R.id.recyclerview_search);
        findDevice = findViewById(R.id.findDevice);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());



        findDevice.setOnClickListener(v -> {
            searchAdapter.getList().clear();
            bluetoothDevice = null;
            if (checkCoarseLocationPermassion()) {
                Boolean bool = bluetoothAdapter.startDiscovery();
                Log.d("Discovery : ", String.valueOf(bool));
            }
        });

        pairBtn.setOnClickListener( v -> {
            if (bluetoothDevice != null){

                bluetoothAdapter.cancelDiscovery();

                /*디바이스 페어링 요청*/
                try {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    progressDialog.setContentView(R.layout.progress);
                    isShowToast = false;
                    bluetoothDevice.createBond();
                    /*Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(bluetoothDevice, (Object[]) null);*/
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "페어링에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showDiscoveredDevices();
    }


    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);

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

    private void showDiscoveredDevices() {
        searchAdapter = new BluetoothScanAdapter( bluetoothDevice -> this.bluetoothDevice = bluetoothDevice );
        recyclerView.setAdapter(searchAdapter);
    }


    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth를 지원하지 않은 단말기 입니다.", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                if (bluetoothAdapter.isDiscovering()) {
                    Toast.makeText(this, "장치 검색중입니다...", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(this, "Bluetooth is enable", Toast.LENGTH_SHORT).show();

                }
            } else {
                Intent enabvleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enabvleIntent, 11);
            }
        }
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //t1.setText("Searching...");
                isShowToast = true;
                findDevice.setEnabled(false);
                findDevice.setTextColor(Color.GRAY);
                Toast.makeText(getApplicationContext(), "디바이스를 찾는 중입니다.", Toast.LENGTH_SHORT).show();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("ACTION_FOUND", "이리오네");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().equalsIgnoreCase("TUG")) {
                    if (!pairedList.contains(device.getAddress())){
                        searchAdapter.addItem(device);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //t1.setText("Finished");

                findDevice.setEnabled(true);
                findDevice.setTextColor(ContextCompat.getColor(ScanActivity.this, R.color.mainColor));
                if (isShowToast)
                    Toast.makeText(getApplicationContext(), "디바이스 찾기가 종료되었습니다.", Toast.LENGTH_SHORT).show();

            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                Log.d("ACTIONBONDSTATECHANGED", "이리오네");
            }

        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (progressDialog != null){
            progressDialog.dismiss();
        }

        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("생명주기", "onStop");

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 11) {
            checkBluetoothState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 11:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Access coarse location allowed. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Access coarse location forbidden. You can't scan Bluetooth devices", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    interface ItemClick {
        void onClick(BluetoothDevice bluetoothDevice);
    }


    class BluetoothScanAdapter extends RecyclerView.Adapter<BluetoothScanAdapter.ViewHolder> {


        private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();

        private ItemClick click;
        private int clickPosition = -1;

        public void addItem(BluetoothDevice bluetoothDevice) {
            HashSet<BluetoothDevice> hashSet = new HashSet<>(bluetoothDevices);
            hashSet.add(bluetoothDevice);
            bluetoothDevices = new ArrayList<>(hashSet);
            notifyDataSetChanged();
        }

        public BluetoothScanAdapter(ItemClick click) {
            this.click = click;
        }

        public ArrayList<BluetoothDevice> getList(){
            return bluetoothDevices;
        }

        @NonNull
        @Override
        public BluetoothScanAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_device_item, parent, false);
            return new BluetoothScanAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BluetoothScanAdapter.ViewHolder holder, int position) {
            holder.txvName.setText(bluetoothDevices.get(position).getName());
            holder.txvAddress.setText(bluetoothDevices.get(position).getAddress());

            holder.itemView.setOnClickListener(v ->{
                click.onClick(bluetoothDevices.get(position));
                clickPosition = position;
                notifyDataSetChanged();

            });

            if (clickPosition == position){
                holder.constraintLayout.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.button_05));
                holder.txvName.setTextColor(ContextCompat.getColor(holder.txvAddress.getContext(), R.color.white));
                holder.txvAddress.setTextColor(ContextCompat.getColor(holder.txvAddress.getContext(), R.color.white));

            }else {
                holder.constraintLayout.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.button_04));
                holder.txvName.setTextColor(ContextCompat.getColor(holder.txvAddress.getContext(), R.color.black));
                holder.txvAddress.setTextColor(ContextCompat.getColor(holder.txvAddress.getContext(), R.color.mainColor));
            }
        }

        @Override
        public int getItemCount() {
            return bluetoothDevices.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView txvName, txvAddress;
            ConstraintLayout constraintLayout;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                constraintLayout = itemView.findViewById(R.id.device_item_parent);
                txvName = itemView.findViewById(R.id.txvName);
                txvAddress = itemView.findViewById(R.id.txvAddress);

            }
        }
    }
}