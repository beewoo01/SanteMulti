package com.physiolab.sante.santemulti;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.physiolab.sante.dialog.SanteGuideDialog;
import com.physiolab.sante.santemulti.databinding.ActivitySelectBinding;

import java.util.Map;
import java.util.Observer;

public class SelectActivity extends AppCompatActivity {

    private ActivitySelectBinding binding;
    private int choseActivity = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();



    }

    private void initView() {

        binding.btnOneMeasure.setOnClickListener(v -> {
            choseActivity = 1;
            if (isGetbluetoocePermission) {
                startActivity(new Intent(SelectActivity.this, Main1chActivity.class));
            } else {
                checkPermission();
            }


        });

        binding.btnTwoMeasure.setOnClickListener(v -> {
            choseActivity = 2;
            if (isGetbluetoocePermission) {
                startActivity(new Intent(SelectActivity.this, Main2chActivity.class));
            } else {
                checkPermission();
            }

        });

        binding.santeIntroduce.setOnClickListener(v -> {
            SanteGuideDialog santeGuideDialog = new SanteGuideDialog(this);
            santeGuideDialog.show();
        });

    }

    private boolean isGetbluetoocePermission = false;

    private void checkPermission() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }else {
                isGetbluetoocePermission = true;
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }else {
                isGetbluetoocePermission = true;
            }
        }*/

        requestPermissionsLauncher.launch(new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

    }

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {


                if (result.containsValue(false)) {
                    isGetbluetoocePermission = false;
                }else {
                    if (choseActivity == 1) {
                        startActivity(new Intent(SelectActivity.this, Main1chActivity.class));
                    }else if (choseActivity == 2) {
                        startActivity(new Intent(SelectActivity.this, Main2chActivity.class));
                    }
                    isGetbluetoocePermission = true;
                }
            });
}