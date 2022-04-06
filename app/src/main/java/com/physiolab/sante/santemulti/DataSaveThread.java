package com.physiolab.sante.santemulti;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ST_DATA_PROC;
import com.physiolab.sante.SanteApp;
import com.physiolab.sante.UserInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class DataSaveThread extends Thread {
    private boolean isLive = false;
    private final Date measureTime;
    private final int deviceIndex;
    private final Queue<ST_DATA_PROC> queue = new LinkedList<>();

    private final ArrayList<String> timeLab = new ArrayList<>();
    private final SanteApp santeApp;
    private int EMGCount = 0;
    private String firstDataTime = null;
    //private FileChannel fileChannel;

    public static ArrayList<String> psFileName;
    private Activity activity;


    public DataSaveThread(Date time, int index, Activity activity) {
        super();
        measureTime = time;
        deviceIndex = index;
        queue.clear();
        santeApp = (SanteApp) activity.getApplication();
        this.activity = activity;
        psFileName = new ArrayList<>();
    }

    public void setFirstDataTime(String firstDataTime) {
        this.firstDataTime = firstDataTime;
    }


    public void Add(ST_DATA_PROC d) {
        queue.add(d);
    }

    public void cancle() {
        /*try {

            fileChannel.position(0);
            fileChannel.write(ByteBuffer.wrap("현우1, 현우2 \r\n".getBytes("UTF-8")));
            fileOutput.getChannel().position(0).write(ByteBuffer.wrap("현우1, 현우2 \r\n".getBytes("UTF-8")));
            *//*fileOutput.getChannel().position(0).write(ByteBuffer.wrap("현우3, 현우4 \r\n".getBytes("UTF-8")));*//*

        } catch (IOException e) {
            e.printStackTrace();
        }*/
        /*if (fileOutput != null) {
            try {
                //fileOutput.getChannel().position((long) 0.1).write(ByteBuffer.wrap("fe2fjew".getBytes("UTF-8")));
                firstInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        isLive = false;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void run() {
        super.run();

        isLive = true;

        FileOutputStream fileOutput = null;
        BufferedOutputStream bufOutput = null;

        boolean isFileExist = false;
        boolean isDirExist = CreateFolder();

        File saveFile = null;


        isFileExist = false;

        if (isDirExist) {
            int saveDevice = deviceIndex + 1;
            String saveFileName = UserInfo.getInstance().name;
            saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", measureTime).toString();
            saveFileName += UserInfo.getInstance().spacial + "_";
            saveFileName += "ch" + saveDevice + ".csv";
            //Log.wtf("saveFileName", saveFileName);
            psFileName.add(saveFileName);

            saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);
            //saveFile = new File(activity.getExternalFilesDir(null).getAbsolutePath() + "/I-Motion Lab/" + saveFileName);

            try {
                fileOutput = new FileOutputStream(saveFile, false);
                bufOutput = new BufferedOutputStream(fileOutput);
                isFileExist = true;
            } catch (IOException e) {
                e.printStackTrace();
                isFileExist = false;
            }
        }



        /*try {
            if (saveFile.createNewFile()){
                isFileExist = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            isFileExist = false;
        }

        if (!isFileExist) {
            int saveDevice = deviceIndex + 1;
            String saveFileName = UserInfo.getInstance().name;
            saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", measureTime).toString();
            saveFileName += UserInfo.getInstance().spacial + "_";
            saveFileName += "ch" + saveDevice + ".csv";
            saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);
            return;
        }*/


        try {
            long Time_Offset = 621355968000000000L+9L*60L*60L*1000L*1000L*10L;
            long measureTime =
                    Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L;

            bufOutput.write(String.format(measureTime + "," +
                            UserInfo.getInstance().gender + "," +
                            UserInfo.getInstance().birth + "," +
                            UserInfo.getInstance().height + "," +
                            UserInfo.getInstance().weight + "," +
                            UserInfo.getInstance().alarm + "," +
                            firstDataTime + ", \r\n"
                    ).getBytes()
            );

            bufOutput.write(String.format(UserInfo.getInstance().name + "\r\n").getBytes("EUC-KR"));
            bufOutput.write(String.format(UserInfo.getInstance().memo + "\r\n").getBytes("EUC-KR"));

            writeDataInfo(bufOutput);

            //bufOutput.write(String.format("EMG, RMS, Acc-X, Acc-Y, Acc-Z, Gyro-X, Gyro-Y, Gyro-Z\r\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        float time = 0F;

        while (isLive) {
                while (queue.size() > 0) {
                    ST_DATA_PROC data = queue.poll();

                    if (data == null) continue;

                    int index = 0;
                    for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
                        index = (int) Math.floor((double) i / 10.0);
                        time += 0.0005F;
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
                                            SampleRMS2(data.Filted, data.Filted.length, santeApp.GetEMGRMS(deviceIndex))//RMS
                                            //data.RMS[i]

                                    ).getBytes());

                        /*bufOutput.write(
                                String.format("%.4f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f, %.8f\r\n",
                                        time, data.Filted[i], data.RMS[i], data.Acc[0][index], data.Acc[1][index],
                                        data.Acc[2][index], data.Gyro[0][index], data.Gyro[1][index], data.Gyro[2][index])
                                        .getBytes());*/
                        /*bufOutput.write(
                                String.format("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f\r\n",
                                        data.Filted[i],data.RMS[i],data.Acc[0][index],data.Acc[1][index],
                                        data.Acc[2][index],data.Gyro[0][index],data.Gyro[1][index],data.Gyro[2][index])
                                        .getBytes());*/
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        EMGCount++;
                    }
                }
            /*} else {
                Log.wtf("fileOutput.getChannel())", "11111111");
                try {
                    //Log.wtf("fileOutput.getChannel())", String.valueOf(fileOutput.getChannel().position(2)));
                    //firstInfo();
                    //fileOutput.getChannel().position(2).write(ByteBuffer.wrap("fjew".getBytes("UTF-8")));

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }*/


        }


        /*try {
            bufOutput.write(String.format("마지막 Index").getBytes());
            bufOutput.write(EMGCount);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        try {
            bufOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fileOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bufOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void firstInfo() {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + psFileName.get(0));

            File nFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + "1"+ psFileName.get(0));
            InputStream inputStream = new FileInputStream(file);
            FileOutputStream fileOutput = new FileOutputStream(nFile);
            BufferedOutputStream bufOutput = new BufferedOutputStream(fileOutput);
            try {

                bufOutput.write(String.format(measureTime + "1," +
                                UserInfo.getInstance().gender + "1," +
                                UserInfo.getInstance().birth + "1," +
                                UserInfo.getInstance().height + "1," +
                                UserInfo.getInstance().weight + "1," +
                                UserInfo.getInstance().alarm + "1," +
                                "이거 되어야해" + "1," +
                                firstDataTime + ", \r\n"
                        ).getBytes()
                );

                bufOutput.write(String.format(UserInfo.getInstance().name + "\r\n").getBytes("UTF-8"));
                bufOutput.write(String.format(UserInfo.getInstance().memo + "\r\n").getBytes("UTF-8"));

                writeDataInfo(bufOutput);


                byte[] buf = new byte[1024];

                int len;
                while ((len = inputStream.read()) > 0) {

                    fileOutput.write(buf, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
    }

    public double SampleRMS2(double[] EMGData, int size, int sec) {
        double result = 0;
        int conInt = 100;

        if (sec == 1) {
            conInt = 200;
        } else if (sec == 2) {
            conInt = 600;
        } else if (sec == 3) {
            conInt = 1000;
        } else if (sec == 4) {
            conInt = 2000;
        }


        if (EMGData.length < conInt) {
            for (int i = 0; i < EMGData.length; i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = Math.sqrt(result / EMGData.length);
        } else if (size < (conInt / 2)) {
            for (int i = 0; i < conInt; i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = Math.sqrt(result / conInt);
        } else if ((EMGData.length - size) < (conInt / 2)) {

            for (int i = EMGData.length - conInt; i < EMGData.length; i++) {
                result += Math.pow(EMGData[i], 2);
            }

            result = Math.sqrt(result / conInt);
        } else {
            for (int i = (size - conInt / 2); i <= (size + conInt / 2); i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = Math.sqrt(result / conInt);
        }

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
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/");
        //File f = new File(activity.getExternalFilesDir(null).getAbsolutePath(), "/I-Motion Lab/");

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
