package com.physiolab.sante.santemulti;

import android.app.Activity;
import android.os.Environment;
import android.text.format.DateFormat;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.UserInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

public class SaveThread  extends Thread{

    private final Date measureTime;
    private final int deviceIndex;
    private final int EMGCount;

    public SaveThread(Date time, int index, Activity activity, String wearingPart, int EMGCount) {
        measureTime = time;
        deviceIndex = index;
        this.EMGCount = EMGCount;
    }

    @Override
    public void run() {
        super.run();

        /*boolean isFileExist = false;
        boolean isDirExist = CreateFolder();

        FileOutputStream fileOutput = null;
        BufferedOutputStream bufOutput = null;


        String saveFileName = UserInfo.getInstance().name;
        saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", new Date()).toString();
        saveFileName += UserInfo.getInstance().spacial;
        saveFileName += wearingPart + ".csv";

        File saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);

        try {
            fileOutput = new FileOutputStream(saveFile, false);
            bufOutput = new BufferedOutputStream(fileOutput);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        long measureTime =
                BTService.Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L;

        bufOutput.write(String.format(measureTime + "," +
                        UserInfo.getInstance().gender + "," +
                        UserInfo.getInstance().birth + "," +
                        UserInfo.getInstance().height + "," +
                        UserInfo.getInstance().weight + "," +
                        UserInfo.getInstance().alarm + "," +
                        EMGCount + "," +
                        firstTime + ", \r\n"
                ).getBytes()
        );*/

    }

    private boolean CreateFolder() {
        boolean ret = false;
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/");

        //Log.d("SaveTest","Create Folder 1");

        if (f.exists()) {
            ret = f.isDirectory() != false;
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
}
