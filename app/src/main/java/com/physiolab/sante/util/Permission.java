package com.physiolab.sante.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permission {

    public static final String[] REQUIRED_PERMISSIONS1 = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public static final String[] REQUIRED_PERMISSIONS2 = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private final Activity activity;
    private final String[] PERMISSIONS;

    public Permission(Activity activity, String... permissions) {
        this.activity = activity;
        this.PERMISSIONS = permissions;
    }

    public boolean isAllPermissionGranted(Context context) {
        if (context != null && PERMISSIONS != null) {
            for (String permission : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    public void requestDangerousPermissions(Activity activity) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    activity,
                    REQUIRED_PERMISSIONS1,
                    REQUEST_CODE_PERMISSIONS
            );
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    REQUIRED_PERMISSIONS2,
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

   /* public boolean permissionState() {

        if (ContextCompat.checkSelfPermission(activity, permissions)
                == PackageManager.PERMISSION_GRANTED) {
            //Permission is granted
            return true;

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
        )) {
            //사용자가 권한 요청을 명시적으로 거부한 경우 true를 반환한다.
            //사용자가 권한 요청을 처음 보거나, 다시 묻지 않음 선택한 경우, 권한을 허용한 경우 false를 반환한다.
            return false;
        }else {
            //Permission has not been asked yet
            return false;
        }
    }*/

}
