package com.physiolab.sante.santemulti;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import java.util.List;
import java.util.Queue;

public class DataSaveThread2 extends Thread {
    private boolean isLive = false;
    //private final Date measureTime;
    private final int deviceIndex;
    //private final Queue<ST_DATA_PROC> queue = new LinkedList<>();
    private Queue<ST_DATA_PROC> queue = null;
    private final SanteApp santeApp;
    private File file;
    private String firstDataTime;
    private int conInt = 100;
    private final ArrayList<Double> EMGData = new ArrayList<>();
    private boolean isSucess = false;
    private SaveFileListener saveFileListener;
    //private FileChannel fileChannel;


    public DataSaveThread2(File file, int index, SanteApp santeApp, String firstDataTime, Activity activity) {
        super();
        //measureTime = time;
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
        Log.wtf("isLive", String.valueOf(isLive));
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
        /*boolean isFileExist = false;
        boolean isDirExist = CreateFolder();*/
        try {

            if (isDirExist) {
                fileOutput = new FileOutputStream(file.getAbsolutePath(), false);
                bufOutput = new BufferedOutputStream(fileOutput);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (isDirExist) {
            try {
                long Time_Offset = 621355968000000000L + 9L * 60L * 60L * 1000L * 1000L * 10L;
                long measureTime =
                        Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L;

                bufOutput.write(String.format(measureTime + "," +
                                UserInfo.getInstance().gender + "," +
                                UserInfo.getInstance().birth + "," +
                                UserInfo.getInstance().height + "," +
                                UserInfo.getInstance().weight + "," +
                                UserInfo.getInstance().alarm + "," +
                                firstDataTime + "\r\n"
                        ).getBytes()
                );

                bufOutput.write(String.format(UserInfo.getInstance().name + "\r\n").getBytes("EUC-KR"));
                bufOutput.write(String.format(UserInfo.getInstance().memo + "\r\n").getBytes("EUC-KR"));

                writeDataInfo(bufOutput);

                //bufOutput.write(String.format("EMG, RMS, Acc-X, Acc-Y, Acc-Z, Gyro-X, Gyro-Y, Gyro-Z\r\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            float time = 0.0000F;

            while (isLive) {
                while (queue.size() > 0) {

                    ST_DATA_PROC data = queue.poll();

                    if (data == null) continue;

                    //Log.wtf("Add", String.valueOf(EMGData.size()));

                    //EMGData.addAll(new ArrayList(Arrays.asList(data.Filted)));
                    //ArrayList<Double> sublist = new ArrayList<Double>(Arrays.asList(data.Filted));

                    EMGData.addAll(Arrays.asList(ArrayUtils.toObject(data.Filted)));
                    if (EMGData.size() > (conInt + BTService.PACKET_SAMPLE_NUM)) {
                        //Log.wtf("EMGDatasize11", String.valueOf(EMGData.size()));
                        EMGData.subList(0, EMGData.size() - (conInt + BTService.PACKET_SAMPLE_NUM)).clear();
                    }

                    //Log.wtf("queue", String.valueOf(EMGData.size()));

                    int index = 0;
                    for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
                        //BTService.PACKET_SAMPLE_NUM = 40
                        index = (int) Math.floor((double) i / 10.0);

                        //EMGData.add(data.Filted[i]);

                        try {

                            bufOutput.write(
                                    String.format("%.4f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f ,%.8f\r\n",
                                            time,
                                            data.Acc[0][index],
                                            data.Acc[1][index],
                                            data.Acc[2][index],
                                            data.Gyro[0][index],
                                            data.Gyro[1][index],
                                            data.Gyro[2][index],
                                            data.Filted[i], //EMG Data
                                            data.BPF_DC[i], //Lead Off
                                            //SampleRMS2(data.Filted, data.Filted.length, santeApp.GetEMGRMS(deviceIndex))//RMS
                                            SampleRMS2(i)//RMS
                                            //data.RMS[i]

                                    ).getBytes());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        time += 0.0005F;
                        //EMGCount++;
                    }
                }

            }


            isSucess = true;
            saveFileListener.onSuccess(deviceIndex);

            Log.wtf("FileSave", "FileSave END22222");

            try {
                bufOutput.flush();
                Log.wtf("FileSave", "FileSave END1111");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                fileOutput.flush();
                Log.wtf("FileSave", "FileSave END2222");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                bufOutput.close();
                Log.wtf("FileSave", "FileSave END3333");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                fileOutput.close();
                Log.wtf("FileSave", "FileSave END4444");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /*private boolean test = false;
    private boolean test2 = true;


    public double SampleRMS2(int position) {
        double result = 0;
        int poi = position;

        int subSize;
        int length = EMGData.size();
        if (length < conInt + BTService.PACKET_SAMPLE_NUM) {
            poi = 0;
            subSize = 0;
        } else {
            //POSITION = 0
            //poi = 0
            //subSize = 40
            subSize = BTService.PACKET_SAMPLE_NUM - position;

        }
        if (test2) {
            if (EMGData.size() == 640) {
                test = true;
                test2 = false;
            }
        }

        if (test) {
            Log.wtf("poi?", String.valueOf(poi));
            Log.wtf("subSize?", String.valueOf(subSize));
        }

        for (int i = poi; i < EMGData.size() - subSize; i++) {
            double data = EMGData.get(i);
            if (test) {
                if (i == 0) {
                    Log.wtf("data00", String.valueOf(data));
                } else if (i == (EMGData.size() - subSize) - 1) {
                    Log.wtf("dataendend", String.valueOf(data));
                }

                if (i == (EMGData.size() - subSize)) {
                    Log.wtf("data????", String.valueOf(data));
                }
            }

            result += Math.pow(data, 2);
        }
        if (test) {
            Log.wtf("pow????", String.valueOf(result));
        }

        result = Math.sqrt(result / EMGData.size() - (poi + subSize));
        if (test) {
            Log.wtf("result sqrt", String.valueOf(result));
            Log.wtf("result size", String.valueOf(EMGData.size() - (poi + subSize)));
            test = false;
        }
        return result;
    }*/

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
