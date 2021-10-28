package com.physiolab.sante.santemulti;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.ST_DATA_PROC;

import java.util.ArrayList;

public class MeasureFragment extends Fragment {
    private static final String TAG = "Fragment-Monitor";
    private static final boolean D = true;

    private MeasureView mView = null;

    private final float[] EMGData = new float[BTService.SAMPLE_RATE * 60 * 5];
    private final float[] LeadOffData = new float[BTService.SAMPLE_RATE * 60 * 5];
    private final float[][] AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
    private final float[][] GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
    private int EMGCount = 0;
    private int dataCount = 0;

    public static MeasureFragment newInstance() {
        return new MeasureFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        mView = new MeasureView(getActivity());
        mView.SetData(EMGData, LeadOffData, AccData, GyroData);

        return mView;
    }

    public void Init() {
        EMGCount = 0;
        dataCount = 0;
//        EMGData = new float[BTService.SAMPLE_RATE * 60 * 5];
//        LeadOffData = new float[BTService.SAMPLE_RATE * 60 * 5];
//        AccData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
//      GyroData = new float[3][(BTService.SAMPLE_RATE / 10) * 60 * 5];
        mView.Init();
    }

    public boolean Add(ST_DATA_PROC data) {
        for (int i = 0; i < BTService.PACKET_SAMPLE_NUM; i++) {
            if (EMGCount >= BTService.SAMPLE_RATE * 60 * 5) return false;

            EMGData[EMGCount] = (float) data.Filted[i];
            LeadOffData[EMGCount] = (float) data.BPF_DC[i];
            EMGCount++;
        }

        for (int i = 0; i < BTService.PACKET_SAMPLE_NUM / 10; i++) {
            if (dataCount >= (BTService.SAMPLE_RATE / 10) * 60 * 5) return false;
            for (int j = 0; j < 3; j++) {
                AccData[j][dataCount] = (float) data.Acc[j][i];
                GyroData[j][dataCount] = (float) data.Gyro[j][i];
            }
            dataCount++;
        }

        mView.SetCount(EMGCount, dataCount);
        return true;
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

    public void SaveData(String wearingPart, Activity activity, ArrayList<String> timLab) {
        Log.wtf("SaveData", "333333333");
        //mView.SaveData(activity, info, context, timLab, progressDialog);
        mView.SaveData(wearingPart, activity, timLab);


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
