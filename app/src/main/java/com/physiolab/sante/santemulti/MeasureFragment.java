package com.physiolab.sante.santemulti;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ST_DATA_PROC;
import com.physiolab.sante.SanteApp;

import org.apache.commons.lang3.ArrayUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MeasureFragment extends Fragment {
    /*private static final String TAG = "Fragment-Monitor";
    private static final boolean D = true;*/

    private MeasureView mView = null;

    private float[] EMGData = new float[BTService.SAMPLE_RATE * 60 * BTService.MaxMinute];
    //private double[] RMSData = new double[BTService.SAMPLE_RATE * 60 * 40];
    private double[] SampleRMSData = new double[BTService.SAMPLE_RATE * 60 * BTService.MaxMinute];
    private float[] LeadOffData = new float[BTService.SAMPLE_RATE * 60 * BTService.MaxMinute];
    private float[][] AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * BTService.MaxMinute];
    private float[][] GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * BTService.MaxMinute];

    // * 60 * 40    여기서 * 40 은 분(Minute)


    private int EMGCount = 0;
    private int RMSCount = 0;
    private int dataCount = 0;
    private String firstDataTime = null;
    private SanteApp santeApps;

    private int conInt = 100;
    //private ArrayList<Double> emgArrList = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mView = new MeasureView(getActivity(), getActivity());
        santeApps = (SanteApp) getActivity().getApplication();
        //mView.SetData(EMGData, RMSData, LeadOffData, AccData, GyroData, SampleRMSData);
        mView.SetData(EMGData, LeadOffData, AccData, GyroData, SampleRMSData);

        return mView;
    }

    public void Init() {
        EMGCount = 0;
        dataCount = 0;
        RMSCount = 0;
//        EMGData = new float[BTService.SAMPLE_RATE * 60 * 5];
//        LeadOffData = new float[BTService.SAMPLE_RATE * 60 * 5];
//        AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
//      GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
        mView.Init();
    }

    @SuppressLint("SimpleDateFormat")
    public boolean Add(ST_DATA_PROC data) {

        for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
            if (EMGCount >= BTService.SAMPLE_RATE * 60 * BTService.MaxMinute) return false;

            EMGData[EMGCount] = (float) data.Filted[i];
            LeadOffData[EMGCount] = (float) data.BPF_DC[i];


            //RMSData[EMGCount] = data.RMS[i];

            SampleRMSData[EMGCount] = RMS(EMGData, conInt, RMSCount);
            //SampleRMSData[EMGCount] = SampleRMS2(i);

            EMGCount++;
            RMSCount++;

        }


        for (int i = 0; i < BTService.PACKET_SAMPLE_NUM / 10; i++) {
            if (dataCount >= (BTService.SAMPLE_RATE / 10) * 60 * BTService.MaxMinute) return false;

            for (int j = 0; j < 3; j++) {
                AccData[j][dataCount] = (float) data.Acc[j][i];
                GyroData[j][dataCount] = (float) data.Gyro[j][i];
            }
            dataCount++;
        }

        //mView.SetCount(EMGCount, dataCount);
        mView.SetCount(EMGCount, dataCount, RMSCount);
        return true;
    }

    public void setRMSFilte(int deviceIndex) {
        conInt = 100;
        switch (santeApps.GetEMGRMS(deviceIndex)) {
            case 1:
                conInt = 200;
                break;

            case 2:
                conInt = 600;
                break;

            case 3:
                conInt = 1000;
                break;

            case 4:
                conInt = 2000;
                break;

        }
    }

    /*private void refrashData() {
        EMGCount = 0;
        RMSCount = 0;
        dataCount = 0;
        EMGData = new float[BTService.SAMPLE_RATE * 60 * 40];
        //RMSData = new double[BTService.SAMPLE_RATE * 60 * 40];
        SampleRMSData = new double[BTService.SAMPLE_RATE * 60 * 40];
        LeadOffData = new float[BTService.SAMPLE_RATE * 60 * 40];
        AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 40];
        GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 40];
        mView.Init();
        mView.SetData(EMGData, LeadOffData, AccData, GyroData, SampleRMSData);
        //mView.SetData(EMGData, RMSData, LeadOffData, AccData, GyroData, SampleRMSData);
    }*/

    /*private int setRMSFilter() {
        int result = 10;

        switch (santeApps.GetEMGRMS(0)) {
            case 0: {
                result = 10;
                break;
            }

            case 1: {
                result = 20;
                break;
            }

            case 2: {
                result = 60;
                break;
            }

            case 3: {
                result = 100;
                break;
            }

            case 4: {
                result = 200;
                break;
            }
        }
        return result;
    }*/

    public float RMS(float[] EMGData, int m, int count) {
        float result = 0;
        int start = 0;
        int end = count;

        if (count >= m) {
            start = count - m ;
        }

        for (int i = start; i < end; i++) {
            result += Math.pow(EMGData[i], 2);
        }
        result = (float) Math.sqrt(result / (count - start));

        return result;


        /*if (count > 0) {
            for (int i = 0; i < m; i++) {
                if (count < m)
                    break;
                result += Math.pow(EMGData[count - (m - i)], 2);
            }
            result = (float) Math.sqrt(result / m);
            return result;

        } else return 0;*/

    }


    public void SetFirstDataTime(String firstDataTime) {
        this.firstDataTime = firstDataTime;
    }

    public String GetFirstDataTime() {
        return firstDataTime;
    }


    public void SetAccRange(float max, float min) {
        mView.SetAccRange(max, min);
    }

    public void SetGyroRange(float max, float min) {
        mView.SetGyroRange(max, min);
    }

    public void SetEMGRange(float max, float min) {
        mView.SetEMGRange(max, min);
    }

    public float GetAccMax() {
        return mView.GetAccMax();
    }

    public float GetAccMin() {
        return mView.GetAccMin();
    }

    public float GetGyroMax() {
        return mView.GetGyroMax();
    }

    public float GetGyroMin() {
        return mView.GetGyroMin();
    }

    public float GetEMGMax() {
        return mView.GetEMGMax();
    }

    public float GetEMGMin() {
        return mView.GetEMGMin();
    }

    public float GetTimeRange() {
        return mView.GetTimeRange();
    }

    public float GetTimeStart() {
        return mView.GetTimeStart();
    }

    public float GetTimeStart2() {
        return mView.GetTimeStart2();
    }


    public boolean GetEnable(int i) {
        return mView.GetEnable(i);
    }

    public void GetEnable(int i, boolean[] axis) {
        mView.GetEnable(i, axis);
    }

    public boolean ToggleEnable(int i) {
        return mView.ToggleEnable(i);
    }

    public boolean SetEnable(int i, boolean x, boolean y, boolean z) {
        return mView.SetEnable(i, x, y, z);
    }


    public int getConunt() {
        return EMGCount;
    }

    public void SetTimeRange(float value) {
        mView.SetTimeRange(value);
    }

    public void PrevPage() {
        mView.PrevPage();
    }

    public void PrevSec() {
        mView.PrevSec();
    }

    public void NextSec() {
        mView.NextSec();
    }

    public void NextPage() {
        mView.NextPage();
    }

    public void SaveData(String wearingPart, Activity activity
            , ArrayList<String> timLab, SanteApp santeApp) {
        //mView.SaveData(activity, info, context, timLab, progressDialog);
        mView.SaveData(wearingPart, activity, timLab, firstDataTime, santeApp);

    }

    public void SaveData2(int device, Activity activity
            , ArrayList<String> timLab, SanteApp santeApp) {
        //mView.SaveData(activity, info, context, timLab, progressDialog);
        mView.SaveData2(device+1, activity, timLab, firstDataTime, santeApp);

    }

    public float[][] getGyroData() {

        return GyroData;

    }

    public void setGyroData(int count) {
        for (int i = 0; i < count; i++) {
            GyroData[0][i] = 0;
            GyroData[1][i] = 0;
            GyroData[2][i] = 0;
        }
        return;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mView = null;
        EMGData = null;
        //RMSData = null;
        SampleRMSData = null;
        LeadOffData = null;
        AccData = null;
        GyroData = null;
        EMGCount = 0;
        RMSCount = 0;
        dataCount = 0;
        firstDataTime = null;
        santeApps = null;
        System.gc();
    }


    /*public double SampleRMS2(int position) {
        double result = 0;
        int poi = position + 1;
        int sub = 40 - poi;

        if (emgArrList.size() < (conInt + 40)) {
            poi = 0;
            sub = 0;
        }

        for (int i = poi; i < emgArrList.size() - sub; i++) {
            double data = emgArrList.get(i);
            result += Math.pow(data, 2);
        }

        result = Math.sqrt(result / (emgArrList.size() - (poi + sub)));
        return result;

    }*/
}
