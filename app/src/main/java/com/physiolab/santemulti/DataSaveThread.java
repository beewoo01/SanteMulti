package com.physiolab.santemulti;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.BlueToothService.ST_DATA_PROC;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class DataSaveThread extends Thread {
    private boolean isLive=false;
    private Date measureTime;
    private int deviceIndex;
    private Queue<ST_DATA_PROC> queue = new LinkedList<>();

    public DataSaveThread(Date time,int index)
    {
        super();
        measureTime=time;
        deviceIndex=index;
        queue.clear();
    }

    public void Add(ST_DATA_PROC d)
    {
        queue.add(d);
    }

    public void cancle()
    {
        isLive=false;
    }

    @Override
    public void run() {
        super.run();

        isLive=true;

        FileOutputStream fileOutput=null;
        BufferedOutputStream bufOutput=null;

        boolean isFileExist=false;
        boolean isDirExist = CreateFolder();

        File saveFile;

        Log.d("SaveTest","Save Thread 1");

        isFileExist = false;
        if (isDirExist)
        {
            Log.d("SaveTest","Save Thread 1-1");

            String saveFileName = DateFormat.format("yyyyMMdd_HHmmss_", measureTime).toString();
            saveFileName+=Integer.toString(deviceIndex)+".txt";

            saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"/SanteMulti/"+saveFileName);

            try {
                fileOutput = new FileOutputStream(saveFile,false);
                bufOutput = new BufferedOutputStream(fileOutput) ;
                isFileExist = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                isFileExist = false;
            }
        }

        if (!isFileExist) return;

        Log.d("SaveTest","Save Thread 2");

        try {
            bufOutput.write(String.format("EMG, RMS, Acc-X, Acc-Y, Acc-Z, Gyro-X, Gyro-Y, Gyro-Z\r\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(isLive)
        {
            while(queue.size()>0)
            {
                ST_DATA_PROC data = queue.poll();

                if (data==null) continue;

                int index=0;
                for(int i = 0; i< BTService.PACKET_SAMPLE_NUM; i++)
                {
                    index = (int)Math.floor((double)i/10.0);
                    try {
                        bufOutput.write(String.format("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f\r\n",data.Filted[i],data.RMS[i],data.Acc[0][index],data.Acc[1][index],data.Acc[2][index],data.Gyro[0][index],data.Gyro[1][index],data.Gyro[2][index]).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.d("SaveTest","Save Thread 3");
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

    private boolean CreateFolder()
    {
        boolean ret=false;
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"/SanteMulti/");

        Log.d("SaveTest","Create Folder 1");

        if (f.exists())
        {
            if (f.isDirectory()==false)
            {
                ret = false;
            }
            else
            {
                ret = true;
            }
        }
        else
        {
            try {
                ret = f.mkdir();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                ret = false;
            }
        }
        return ret;
    }
}
