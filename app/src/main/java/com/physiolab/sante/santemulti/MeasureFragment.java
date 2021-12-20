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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MeasureFragment extends Fragment {
    private static final String TAG = "Fragment-Monitor";
    private static final boolean D = true;

    private MeasureView mView = null;

    private final float[] EMGData = new float[BTService.SAMPLE_RATE * 60 * 5];
    private final double[] RMSData = new double[BTService.SAMPLE_RATE * 60 * 5];
    private final double[] SampleRMSData = new double[BTService.SAMPLE_RATE * 60 * 5];
    private final float[] LeadOffData = new float[BTService.SAMPLE_RATE * 60 * 5];
    private final float[][] AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
    private final float[][] GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
    private int EMGCount = 0;
    private int RMSCount = 0;
    private int dataCount = 0;
    private String firstDataTime = null;

    public static MeasureFragment newInstance() {
        return new MeasureFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        mView = new MeasureView(getActivity());
        mView.SetData(EMGData, RMSData, LeadOffData, AccData, GyroData, SampleRMSData);

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
            if (EMGCount >= BTService.SAMPLE_RATE * 60 * 5) return false;

            EMGData[EMGCount] = (float) data.Filted[i];
            LeadOffData[EMGCount] = (float) data.BPF_DC[i];


            RMSData[EMGCount] = data.RMS[i];


            EMGCount++;
            RMSCount++;

        }
        //RMSCount = (int) Math.sqrt(EMGCount);


        for (int i = 0; i < BTService.PACKET_SAMPLE_NUM / 10; i++) {
            if (dataCount >= (BTService.SAMPLE_RATE / 10) * 60 * 5) return false;
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



    public void SetFirstDataTime(String firstDataTime){
        this.firstDataTime = firstDataTime;
        Log.wtf("SetFirstDataTime", String.valueOf(firstDataTime));
    };

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
        Log.wtf("SaveData", "333333333");
        //mView.SaveData(activity, info, context, timLab, progressDialog);
        mView.SaveData(wearingPart, activity, timLab, firstDataTime, santeApp);


    }

    public float[][] getGyroData(){

        return GyroData;

    }
    public void setGyroData(int count){
        for(int i = 0 ; i<count;i++){
            GyroData[0][i]=0;
            GyroData[1][i]=0;
            GyroData[2][i]=0;
        }
        return ;

    }



}
