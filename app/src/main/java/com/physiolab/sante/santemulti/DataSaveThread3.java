package com.physiolab.sante.santemulti;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ST_DATA_PROC;
import com.physiolab.sante.SanteApp;
import com.physiolab.sante.UserInfo;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class DataSaveThread3 extends Thread {
    private boolean isLive = false;
    //private final Date measureTime;
    private final int deviceIndex;
    //private final Queue<ST_DATA_PROC> queue = new LinkedList<>();
    private Queue<ST_DATA_PROC> queue = null;
    private final SanteApp santeApp;
    private final File file;
    private final String firstDataTime;
    private int conInt = 100;
    private final ArrayList<Double> EMGData = new ArrayList<>();
    private final SaveFileListener saveFileListener;

    public DataSaveThread3(File file, int index, SanteApp santeApp, String firstDataTime, Activity activity) {
        super();
        deviceIndex = index;
        this.file = file;
        queue = new LinkedList<>();
        this.santeApp = santeApp;
        this.firstDataTime = firstDataTime;
        setRmsFilte();
        saveFileListener = (SaveFileListener) activity;
    }

    public void Add(ST_DATA_PROC d) {
        queue.add(d);

    }

    public void cancle() {
        isLive = false;
    }

    private void setRmsFilte() {
        int sec = santeApp.GetEMGRMS(deviceIndex);
        conInt = 100;
        if (sec == 1) {
            conInt = 200;
        } else if (sec == 2) {
            conInt = 600;
        } else if (sec == 3) {
            conInt = 1000;
        } else if (sec == 4) {
            conInt = 2000;
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void run() {
        super.run();

        isLive = true;

        FileOutputStream fileOutput = null;
        BufferedOutputStream bufOutput = null;
        boolean isDirExist = CreateFolder();

        try {

            if (isDirExist) {
                fileOutput = new FileOutputStream(file.getAbsolutePath(), false);
                bufOutput = new BufferedOutputStream(fileOutput);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



    }

    public double SampleRMS2(int position) {
        double result = 0;
        int poi = position + 1;
        int sub = 40 - poi;
        if (EMGData.size() < (conInt + 40)) {
            poi = 0;
            sub = 0;
        }

        for (int i = poi; i < EMGData.size() - sub; i++) {
            double data = EMGData.get(i);
            result += Math.pow(data, 2);
        }

        result = Math.sqrt(result / (EMGData.size() - (poi + sub)));
        return result;

    }

    private void writeDataInfo(BufferedOutputStream bufOutput) {


        String AccHPF = "None";
        String AccLPF = "None";
        String GyroHPF = "None";
        String GyroLPF = "None";
        String EMGNotch = "Off";
        String EMGHPF = "None";
        String EMGLPF = "None";
        String EMGRMS = "0.3s";

        if (santeApp.GetAccHPF(deviceIndex) == 0) {
            AccHPF = "None";
        } else if (santeApp.GetAccHPF(deviceIndex) == 1) {
            AccHPF = "0.5Hz";
        } else if (santeApp.GetAccHPF(deviceIndex) == 2) {
            AccHPF = "1Hz";
        }

        if (santeApp.GetAccLPF(deviceIndex) == 0) {
            AccLPF = "None";
        } else if (santeApp.GetAccLPF(deviceIndex) == 1) {
            AccLPF = "10Hz";
        } else if (santeApp.GetAccLPF(deviceIndex) == 2) {
            AccLPF = "20Hz";
        }

        if (santeApp.GetGyroHPF(deviceIndex) == 0) {
            GyroHPF = "None";
        } else if (santeApp.GetGyroHPF(deviceIndex) == 1) {
            GyroHPF = "0.5Hz";
        } else if (santeApp.GetGyroHPF(deviceIndex) == 2) {
            GyroHPF = "1Hz";
        }


        if (santeApp.GetGyroLPF(deviceIndex) == 0) {
            GyroLPF = "None";
        } else if (santeApp.GetGyroLPF(deviceIndex) == 1) {
            GyroLPF = "10Hz";
        } else if (santeApp.GetGyroLPF(deviceIndex) == 2) {
            GyroLPF = "20Hz";
        }


        if (santeApp.GetEMGNotch(deviceIndex) == 0) {
            EMGNotch = "Notch Off";
        } else {
            EMGNotch = "Notch On";
        }

        if (santeApp.GetEMGHPF(deviceIndex) == 0) {
            EMGHPF = "None";
        } else if (santeApp.GetEMGHPF(deviceIndex) == 1) {
            EMGHPF = "3Hz";
        } else if (santeApp.GetEMGHPF(deviceIndex) == 2) {
            EMGHPF = "20Hz";
        }

        if (santeApp.GetEMGHPF(deviceIndex) == 0) {
            EMGLPF = "None";
        } else if (santeApp.GetEMGHPF(deviceIndex) == 1) {
            EMGLPF = "250Hz";
        } else if (santeApp.GetEMGHPF(deviceIndex) == 2) {
            EMGLPF = "500Hz";
        }

        if (santeApp.GetEMGRMS(deviceIndex) == 0) {
            EMGRMS = "0.05s";
        } else if (santeApp.GetEMGRMS(deviceIndex) == 1) {
            EMGRMS = "0.1s";
        } else if (santeApp.GetEMGRMS(deviceIndex) == 2) {
            EMGRMS = "0.3s";
        } else if (santeApp.GetEMGRMS(deviceIndex) == 3) {
            EMGRMS = "0.5s";
        } else if (santeApp.GetEMGRMS(deviceIndex) == 4) {
            EMGRMS = "1s";
        }

        try {
            bufOutput.write(
                    String.format(
                            " ,Acc HPF," + AccHPF + ",Acc LPF," + AccLPF +
                                    ",Gyro HPF," + GyroHPF + ",Gyro LPF," + GyroLPF +
                                    ",EMG Notch," + EMGNotch + ",EMG HPF," + EMGHPF +
                                    ",EMG LPF," + EMGLPF + ",EMG RMS," + EMGRMS + "\r\n").getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean CreateFolder() {
        boolean ret = false;
        //File f = new File(activity.getExternalFilesDir(null).getAbsolutePath(), "/I-Motion Lab/");
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
