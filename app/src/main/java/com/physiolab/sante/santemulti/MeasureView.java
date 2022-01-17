package com.physiolab.sante.santemulti;

import static com.physiolab.sante.santemulti.DataSaveThread.psFileName;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.opencsv.CSVWriter;
import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.SanteApp;
import com.physiolab.sante.UserInfo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MeasureView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "SurfaceView-Graph";
    private static final boolean D = true;
    private Context mContext;
    String export;
    private GraphViewThread gvThread;

    private Bitmap bufferGraph = null;
    private Canvas bufferCanvas = null;
    private Bitmap tempGraph = null;
    private Canvas tempCanvas = null;
    private Bitmap gridGraph = null;
    private Canvas gridCanvas = null;

    private float[] EMGData = null;
    private float[] LeadOffData = null;
    private float[][] AccData = null;
    private float[][] GyroData = null;

    private double[] RMSData = null;
    private double[] SampleRMSData = null;


    private int EMGCount = 0;
    private int dataCount = 0;
    private int RMSCount = 0;
    private double diff = 0;

    private float RMSMax = 1000.0f;
    private float RMSMin = -1000.0f;
    /**
     * 우선 MIN, MAX EMG와 동일하게
     */

    private float EMGYMax = 1000.0f;
    private float EMGYMin = -1000.0f;
    private float AccYMax = 6.0f;
    private float AccYMin = -6.0f;
    private float GyroYMax = 800.0f;
    private float GyroYMin = -400.0f;
    private float TimeStart = 0.0f;
    private float TimeStart2 = 0.0f;
    private float TimeRange = 30.0f;
    private float TimeRange2 = 0.0f;

    private boolean EMGEnable = true;
    private boolean EMGRMSEnable = true;
    private boolean AccXEnable = true;
    private boolean AccYEnable = true;
    private boolean AccZEnable = true;
    private boolean GyroXEnable = true;
    private boolean GyroYEnable = true;
    private boolean GyroZEnable = true;

    private Paint borderPnt;
    private Paint gridPnt;
    private Paint gridCenterPnt;
    private Paint EMGPnt;
    private Paint[] AccPnt = new Paint[3];
    private Paint[] GyroPnt = new Paint[3];

    /* RMS Graph */
    private Paint RMSPnt;

    private boolean isRedraw = false;
    //private DefaultDialog defaultDialog;
    private ProgressDialog progressDialog;

    private SaveFileListener listener;
    private SanteApp santeApps;


    public MeasureView(Context context, Activity activity) {
        super(context);
        mContext = context;
        santeApps = (SanteApp) activity.getApplication();
        //Log.wtf("santeApps.GetEMGRMS(0);", String.valueOf(santeApps.GetEMGRMS(0)));
        SurfaceHolder mHolder;
        mHolder = getHolder();
        mHolder.addCallback(this);
        /*santeApps = new SanteApp[]{
                (SanteApp) this.mContext,
                (SanteApp) this.mContext
        };*/
        //Log.wtf("GetEMGRMS", String.valueOf(santeApps[0].GetEMGRMS(0)));

        borderPnt = new Paint();
        borderPnt.setColor(getResources().getColor(R.color.GraphBorder));
        borderPnt.setStrokeWidth(8.0f);
        borderPnt.setStyle(Paint.Style.STROKE);

        gridPnt = new Paint();
        gridPnt.setColor(getResources().getColor(R.color.GraphGrid));
        gridPnt.setStyle(Paint.Style.STROKE);

        gridCenterPnt = new Paint();
        gridCenterPnt.setColor(getResources().getColor(R.color.GraphBorder));
        gridCenterPnt.setStrokeWidth(2.0f);
        gridCenterPnt.setStyle(Paint.Style.STROKE);

        EMGPnt = new Paint();
        EMGPnt.setColor(getResources().getColor(R.color.GraphEMG));
        EMGPnt.setStyle(Paint.Style.STROKE);

        RMSPnt = new Paint();
        RMSPnt.setColor(getResources().getColor(R.color.GraphRMS));
        RMSPnt.setStrokeWidth(2.0f);
        RMSPnt.setStyle(Paint.Style.STROKE);

        AccPnt[0] = new Paint();
        AccPnt[0].setColor(getResources().getColor(R.color.GraphAccX));
        AccPnt[0].setStrokeWidth(2.0f);
        AccPnt[0].setStyle(Paint.Style.STROKE);

        AccPnt[1] = new Paint();
        AccPnt[1].setColor(getResources().getColor(R.color.GraphAccY));
        AccPnt[1].setStrokeWidth(2.0f);
        AccPnt[1].setStyle(Paint.Style.STROKE);

        AccPnt[2] = new Paint();
        AccPnt[2].setColor(getResources().getColor(R.color.GraphAccZ));
        AccPnt[2].setStrokeWidth(2.0f);
        AccPnt[2].setStyle(Paint.Style.STROKE);

        GyroPnt[0] = new Paint();
        GyroPnt[0].setColor(getResources().getColor(R.color.GraphGyroX));
        GyroPnt[0].setStrokeWidth(2.0f);
        GyroPnt[0].setStyle(Paint.Style.STROKE);

        GyroPnt[1] = new Paint();
        GyroPnt[1].setColor(getResources().getColor(R.color.GraphGyroY));
        GyroPnt[1].setStrokeWidth(2.0f);
        GyroPnt[1].setStyle(Paint.Style.STROKE);

        GyroPnt[2] = new Paint();
        GyroPnt[2].setColor(getResources().getColor(R.color.GraphGyroZ));
        GyroPnt[2].setStrokeWidth(2.0f);
        GyroPnt[2].setStyle(Paint.Style.STROKE);
    }

    public void Init() {
        EMGCount = 0;
        dataCount = 0;
        diff = 0.0;
        TimeStart = 0.0f;
        RMSCount = 0;
        TimeStart2 = 0.0f;
//         EMGData = null;
//        LeadOffData = null;
//         AccData = null;
//         GyroData = null;
        Refresh();
    }

    public void SetData(float[] emg, double[] rms, float[] leadoff, float[][] acc, float[][] gyro, double[] sampleRmsData) {
        EMGData = emg;
        RMSData = rms;
        SampleRMSData = sampleRmsData;
        LeadOffData = leadoff;
        AccData = acc;
        GyroData = gyro;
        EMGCount = 0;
        RMSCount = 0;
        dataCount = 0;
        diff = 0.0;
        TimeStart = 0.0f;
        TimeStart2 = 0.0f;

        Refresh();
    }

    public void PrevPage() {
        TimeStart = TimeStart - TimeRange;
        if (TimeStart < 0.0f) TimeStart = 0.0f;
        Refresh();
    }

    public void PrevSec() {
        TimeStart = TimeStart - 1.0f;
        if (TimeStart < 0.0f) TimeStart = 0.0f;
        Refresh();
    }

    public void NextSec() {
        TimeStart = TimeStart + 1.0f;
        if (TimeStart + TimeRange > 300.0f) TimeStart = 300.0f - TimeRange;
        Refresh();
    }

    public void NextPage() {
        TimeStart = TimeStart + TimeRange;
        if (TimeStart + TimeRange > 300.0f) TimeStart = 300.0f - TimeRange;
        Refresh();
    }

    public void SetAccRange(float max, float min) {
        AccYMax = max;
        AccYMin = min;

        Refresh();
    }

    public void SetGyroRange(float max, float min) {
        GyroYMax = max;
        GyroYMin = min;

        Refresh();
    }

    public void SetEMGRange(float max, float min) {
        EMGYMax = max;
        EMGYMin = min;

        Refresh();
    }

    public float GetAccMax() {
        return AccYMax;
    }

    public float GetAccMin() {
        return AccYMin;
    }

    public float GetGyroMax() {
        return GyroYMax;
    }

    public float GetGyroMin() {
        return GyroYMin;
    }

    public float GetEMGMax() {
        return EMGYMax;
    }

    public float GetEMGMin() {
        return EMGYMin;
    }

    public float GetTimeRange() {
        return TimeRange;
    }

    public float GetTimeStart() {
        return TimeStart;
    }

    public float SetTimeStart2() {
        return TimeStart2;
    }

    public float GetTimeStart2() {
        return TimeStart2;
    }

    public void SetTimeRange(float value) {
        TimeRange = value;
        //TimeStart2 = value ;
        //TimeStart2 = (float) EMGCount / (float) BTService.SAMPLE_RATE;
        if (TimeStart + TimeRange < (float) EMGCount / (float) BTService.SAMPLE_RATE) {
            TimeStart = (float) EMGCount / (float) BTService.SAMPLE_RATE - TimeRange;
        }

        if (TimeStart + TimeRange > 300.0f) {
            TimeStart = 300.0f - TimeRange;
        }

        //TimeStart2 = EMGCount / (float) BTService.SAMPLE_RATE;

        Refresh();
    }

    public void SetCount(int emg, int data, int rms) {
        int refresh = 4;

        synchronized (bufferCanvas) {

            float yPos;
            float xPos;

            Path path = new Path();
            path.reset();

            if ((float) emg / (float) BTService.SAMPLE_RATE > TimeStart + TimeRange) {
                diff += ((double) (bufferGraph.getWidth() - 4) * (((double) (emg - EMGCount) / (double) BTService.SAMPLE_RATE) / (double) (TimeRange)));

                TimeStart = TimeStart + (float) ((double) Math.ceil(diff) / (double) (bufferGraph.getWidth() - 4) * (double) (TimeRange));
                //TimeStart2 += 0.01f;
                tempCanvas.drawColor(ContextCompat.getColor(getContext(), android.R.color.transparent), PorterDuff.Mode.SRC);
                //tempCanvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.SRC);
                tempCanvas.drawBitmap(bufferGraph, 0, 0, null);


                bufferCanvas.drawColor(ContextCompat.getColor(getContext(), android.R.color.transparent), PorterDuff.Mode.SRC);
                //bufferCanvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.SRC);
                bufferCanvas.drawBitmap(tempGraph, (int) Math.ceil(diff) * -1, 0, null);


                diff = diff - Math.ceil(diff);
            } else {
                /*diff += ((double) (bufferGraph.getWidth() - 4) * (((double) (emg - EMGCount) / (double) BTService.SAMPLE_RATE) / (double) (TimeRange)));
                TimeStart2 += 0.01f;
                diff = diff - Math.ceil(diff);*/
            }


            if (EMGEnable) {
                //Log.wtf("EMGEnable!!!", "1111111111");
                if (EMGCount == 0) {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[EMGCount]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) EMGCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.moveTo(xPos, yPos);
                } else {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[EMGCount - refresh]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (EMGCount - refresh) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                    path.moveTo(xPos, yPos);

                    for (int i = refresh - 1; i > 0; i--) {
                        yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[EMGCount - i]) / (EMGYMax - EMGYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (EMGCount - i) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                        path.lineTo(xPos, yPos);
                    }
                }

                for (int i = EMGCount; i < emg; i++) {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[i]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, EMGPnt);
            }


            /*if (EMGRMSEnable) {

                int conInt = santeApps.GetEMGRMS(0);

                switch (conInt) {
                    case 0: {
                        conInt = 10;
                        break;
                    }

                    case 1: {
                        conInt = 20;
                        break;
                    }

                    case 2: {
                        conInt = 60;
                        break;
                    }

                    case 3: {
                        conInt = 100;
                        break;
                    }

                    case 4: {
                        conInt = 200;
                        break;
                    }
                }

                path.reset();
                if (EMGCount == 0) {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[EMGCount]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) EMGCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                    path.moveTo(xPos, yPos);

                } else {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[EMGCount - refresh]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (EMGCount - refresh) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                    path.moveTo(xPos, yPos);

                    for (int i = refresh - 1; i > 0; i--) {
                        yPos = bufferGraph.getHeight() * ((EMGYMax - RMS(EMGData, conInt, i)) / (EMGYMax - EMGYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (EMGCount - i) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                        path.lineTo(xPos, yPos);
                    }
                }

                *//*for (int i = EMGCount; i < emg; i++) {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - RMS(EMGData, conInt, i)) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }*//*
                bufferCanvas.drawPath(path, RMSPnt);
            }*/


            //if (GyroEnable)
            {

                for (int axis = 2; axis >= 0; axis--) {
                    if (axis == 2 && !GyroZEnable) continue;
                    if (axis == 1 && !GyroYEnable) continue;
                    if (axis == 0 && !GyroXEnable) continue;

                    path.reset();

                    if (dataCount == 0) {
                        yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[axis][dataCount]) / (GyroYMax - GyroYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) dataCount / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);
                    } else {
                        yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[axis][dataCount - refresh]) / (GyroYMax - GyroYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - refresh) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);

                        for (int i = refresh - 1; i > 0; i--) {
                            yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[axis][dataCount - i]) / (GyroYMax - GyroYMin));
                            xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - i) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                            path.lineTo(xPos, yPos);
                        }
                    }

                    for (int i = dataCount; i < data; i++) {
                        yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[axis][i]) / (GyroYMax - GyroYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }
                    bufferCanvas.drawPath(path, GyroPnt[axis]);
                }
            }

            //if (AccEnable)
            {
                for (int axis = 2; axis >= 0; axis--) {
                    if (axis == 2 && !AccZEnable) continue;
                    if (axis == 1 && !AccYEnable) continue;
                    if (axis == 0 && !AccXEnable) continue;

                    path.reset();

                    if (dataCount == 0) {
                        yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][dataCount]) / (AccYMax - AccYMin));
                        //yPos = bufferGraph.getHeight() * ((AccYMax - AccData[j][xMinCount]) / (AccYMax - AccYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) dataCount / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        //xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);
                    } else {
                        yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][dataCount - refresh]) / (AccYMax - AccYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - refresh) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);

                        for (int i = refresh - 1; i > 0; i--) {
                            yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][dataCount - i]) / (AccYMax - AccYMin));
                            //yPos = bufferGraph.getHeight() * ((AccYMax - AccData[j][i]) / (AccYMax - AccYMin));
                            xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - i) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                            //xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));
                            path.lineTo(xPos, yPos);
                        }
                    }

                    for (int i = dataCount; i < data; i++) {
                        yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][i]) / (AccYMax - AccYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }
                    bufferCanvas.drawPath(path, AccPnt[axis]);
                }
            }


            // 이전 RMS 그래프
           /* {
                if (EMGRMSEnable) {
                    path.reset();
                    if (RMSCount == 0) {
                        yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[RMSCount]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) RMSCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                        path.moveTo(xPos, yPos);
                    } else {

                        yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[RMSCount - refresh]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (RMSCount - refresh) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);


                        for (int i = refresh - 1; i > 0; i--) {
                            yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[RMSCount - i]) / (RMSMax - RMSMin)));
                            xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (RMSCount - i) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                            path.lineTo(xPos, yPos);
                        }

                    }

                    for (int i = RMSCount; i < emg; i++) {

                        int sec = santeApps.GetEMGRMS(0);

                        int conInt;
                        if (sec == 1) {
                            conInt = 10;
                        } else if (sec == 2) {
                            conInt = 20;
                        } else if (sec == 3) {
                            conInt = 60;
                        } else if (sec == 4) {
                            conInt = 100;
                        } else {
                            conInt = 200;
                        }
                        if (conInt != 0 && i != conInt) {
                            if (i > conInt) {
                                yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i - conInt]) / (RMSMax - RMSMin)));
                            }
                        } else {
                            yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                        }


                        //yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }

                    bufferCanvas.drawPath(path, RMSPnt);


                }
            }*/


            {
                if (EMGRMSEnable) {
                    path.reset();
                    if (RMSCount == 0) {
                        yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[RMSCount]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) RMSCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                        path.moveTo(xPos, yPos);
                    } else {

                        yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[RMSCount - refresh]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (RMSCount - refresh) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);


                        for (int i = refresh - 1; i > 0; i--) {
                            yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[RMSCount - i]) / (RMSMax - RMSMin)));
                            xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (RMSCount - i) / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));
                            path.lineTo(xPos, yPos);
                        }

                    }

                    for (int i = RMSCount; i < emg; i++) {

                        yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[i]) / (RMSMax - RMSMin)));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }

                    bufferCanvas.drawPath(path, RMSPnt);


                }
            }


        }


        EMGCount = emg;
        dataCount = data;
        RMSCount = rms;

    }

    public void Refresh() {
        isRedraw = true;
    }

    private void DrawGrid() {
        synchronized (gridCanvas) {

            gridCanvas.drawColor(getResources().getColor(R.color.GraphBackground));
            for (int i = 1; i < 10; i++) {
                if (i == 5) {
                    gridCanvas.drawLine(gridGraph.getWidth() / 10 * i, 0, gridGraph.getWidth() / 10 * i, gridGraph.getHeight(), gridCenterPnt);
                    gridCanvas.drawLine(0, gridGraph.getHeight() / 10 * i, gridGraph.getWidth(), gridGraph.getHeight() / 10 * i, gridCenterPnt);
                } else {
                    gridCanvas.drawLine(gridGraph.getWidth() / 10 * i, 0, gridGraph.getWidth() / 10 * i, gridGraph.getHeight(), gridPnt);
                    gridCanvas.drawLine(0, gridGraph.getHeight() / 10 * i, gridGraph.getWidth(), gridGraph.getHeight() / 10 * i, gridPnt);
                }
            }
        }
    }

    private void Redraw() {

        synchronized (bufferCanvas) {
            diff = 0.0;

            Path path = new Path();
            int xMinCount;
            int xMaxCount;
            float yPos;
            float xPos;
            //Log.wtf("EMGEnable!!!", String.valueOf(EMGEnable));

            bufferCanvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.SRC);


            if (EMGEnable) {
                xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE), (double) EMGCount);

                path.reset();

                yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[xMinCount]) / (EMGYMax - EMGYMin));
                xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                path.moveTo(xPos, yPos);

                for (int i = xMinCount + 1; i < xMaxCount; i++) {
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[i]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, EMGPnt);
            } /*else if (EMGEnable && EMGRMSEnable) {
                xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE), (double) EMGCount);

                path.reset();

                yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[xMinCount]) / (EMGYMax - EMGYMin));
                xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                path.moveTo(xPos, yPos);

                for (int i = xMinCount + 1; i < xMaxCount; i++) {
                    //Log.wtf("EMGEnable!!!", "여기옴?11111111");
                    yPos = bufferGraph.getHeight() * ((EMGYMax - RMS(EMGData,600,i)) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, EMGPnt);
            }*/


            //if(GyroEnable)
            {
                for (int j = 2; j >= 0; j--) {
                    if (j == 2 && !GyroZEnable) continue;
                    if (j == 1 && !GyroYEnable) continue;
                    if (j == 0 && !GyroXEnable) continue;

                    xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE / 10.0), 0.0);
                    xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * ((double) BTService.SAMPLE_RATE / 10.0)), (double) dataCount);

                    path.reset();

                    yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[j][xMinCount]) / (GyroYMax - GyroYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));

                    path.moveTo(xPos, yPos);

                    for (int i = xMinCount + 1; i < xMaxCount; i++) {
                        yPos = bufferGraph.getHeight() * ((GyroYMax - GyroData[j][i]) / (GyroYMax - GyroYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }
                    bufferCanvas.drawPath(path, GyroPnt[j]);
                }

            }

            //if(AccEnable)
            {
                for (int j = 2; j >= 0; j--) {
                    if (j == 2 && !AccZEnable) continue;
                    if (j == 1 && !AccYEnable) continue;
                    if (j == 0 && !AccXEnable) continue;

                    xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE / 10.0), 0.0);
                    xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * ((double) BTService.SAMPLE_RATE / 10.0)), (double) dataCount);

                    path.reset();

                    //yPos = bufferGraph.getHeight() * ((AccYMax - AccData[j][xMinCount]) / (AccYMax - AccYMin));
                    yPos = bufferGraph.getHeight() * ((EMGYMax - EMGData[xMinCount]) / (EMGYMax - EMGYMin));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));

                    path.moveTo(xPos, yPos);

                    for (int i = xMinCount + 1; i < xMaxCount; i++) {
                        yPos = bufferGraph.getHeight() * ((AccYMax - AccData[j][i]) / (AccYMax - AccYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / ((float) BTService.SAMPLE_RATE / 10.0f)) - TimeStart) / (TimeRange));

                        path.lineTo(xPos, yPos);
                    }
                    bufferCanvas.drawPath(path, AccPnt[j]);
                }

            }


            if (EMGRMSEnable) {
                xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE), (double) RMSCount);

                path.reset();

                yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[xMinCount]) / (RMSMax - RMSMin)));
                xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                path.moveTo(xPos, yPos);

                for (int i = xMinCount + 1; i < xMaxCount; i++) {
                    //Log.wtf("EMGEnable!!!", "여기옴?11111111");
                    int conInt = santeApps.GetEMGRMS(0);

                    switch (conInt) {
                        case 0: {
                            conInt = 10;
                            break;
                        }

                        case 1: {
                            conInt = 20;
                            break;
                        }

                        case 2: {
                            conInt = 60;
                            break;
                        }

                        case 3: {
                            conInt = 100;
                            break;
                        }

                        case 4: {
                            conInt = 200;
                            break;
                        }
                    }
                    //yPos = bufferGraph.getHeight() * ((RMSMin - RMS(EMGData, conInt, i)) / (RMSMax - RMSMin));
                    yPos = (float) (bufferGraph.getHeight() * ((RMSMax - SampleRMSData[i]) / (RMSMax - RMSMin)));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, RMSPnt);
            }

            /*if (EMGRMSEnable) {
                xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE), (double) RMSCount);

                path.reset();

                yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[xMinCount]) / (RMSMax - RMSMin)));
                xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                path.moveTo(xPos, yPos);

                for (int i = xMinCount + 1; i < xMaxCount; i++) {
                    //Log.wtf("EMGEnable!!!", "여기옴?11111111");
                    int conInt = santeApps.GetEMGRMS(0);

                    switch (conInt) {
                        case 0: {
                            conInt = 10;
                            break;
                        }

                        case 1: {
                            conInt = 20;
                            break;
                        }

                        case 2: {
                            conInt = 60;
                            break;
                        }

                        case 3: {
                            conInt = 100;
                            break;
                        }

                        case 4: {
                            conInt = 200;
                            break;
                        }
                    }
                    //yPos = bufferGraph.getHeight() * ((RMSMin - RMS(EMGData, conInt, i)) / (RMSMax - RMSMin));
                    yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, RMSPnt);
            }*/


        }

        //Log.d(TAG, "Redraw Stop");
    }

    public boolean GetEnable(int i) {
        switch (i) {
            case 0:
                return (AccXEnable || AccYEnable || AccZEnable);
            case 1:
                return (GyroXEnable || GyroYEnable || GyroZEnable);
            case 2:
                return EMGEnable;
            default:
                return false;
        }
    }

    public void GetEnable(int i, boolean[] axis) {
        switch (i) {
            case 0:
                axis[0] = AccXEnable;
                axis[1] = AccYEnable;
                axis[2] = AccZEnable;
                break;
            case 1:
                axis[0] = GyroXEnable;
                axis[1] = GyroYEnable;
                axis[2] = GyroZEnable;
                break;
            case 2:
                axis[0] = EMGEnable;
                //axis[1] = EMGEnable;
                axis[1] = EMGRMSEnable;
                axis[2] = EMGEnable;
                break;
            default:
                axis[0] = false;
                axis[1] = false;
                axis[2] = false;
                break;
        }
    }

    public boolean ToggleEnable(int i) {
        switch (i) {
            case 0:
                if (AccXEnable || AccYEnable || AccZEnable) {
                    AccXEnable = false;
                    AccYEnable = false;
                    AccZEnable = false;
                } else {
                    AccXEnable = true;
                    AccYEnable = true;
                    AccZEnable = true;
                }
                Refresh();
                return (AccXEnable || AccYEnable || AccZEnable);
            case 1:
                if (GyroXEnable || GyroYEnable || GyroZEnable) {
                    GyroXEnable = false;
                    GyroYEnable = false;
                    GyroZEnable = false;
                } else {
                    GyroXEnable = true;
                    GyroYEnable = true;
                    GyroZEnable = true;
                }
                Refresh();
                return (GyroXEnable || GyroYEnable || GyroZEnable);
            case 2:
                EMGEnable = !EMGEnable;
                Refresh();
                return EMGEnable;
            default:
                return false;
        }
    }

    public boolean SetEnable(int i, boolean x, boolean y, boolean z) {
        switch (i) {
            case 0:
                AccXEnable = x;
                AccYEnable = y;
                AccZEnable = z;

                Refresh();
                return (AccXEnable || AccYEnable || AccZEnable);
            case 1:
                GyroXEnable = x;
                GyroYEnable = y;
                GyroZEnable = z;

                Refresh();
                return (GyroXEnable || GyroYEnable || GyroZEnable);
            case 2:
                EMGEnable = x;
                EMGRMSEnable = y;
                Refresh();
                return EMGEnable;
            default:
                return false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        gvThread = new GraphViewThread(getHolder(), this);
        gvThread.setRunning(true);
        gvThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //RGBA_F16
        if (bufferGraph == null || tempGraph == null || gridGraph == null) {
            bufferGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(bufferGraph);
            tempGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            tempCanvas = new Canvas(tempGraph);
            gridGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            gridCanvas = new Canvas(gridGraph);
            DrawGrid();
            Refresh();
        } else if (bufferGraph.getWidth() != width || bufferGraph.getHeight() != height) {
            try {
                bufferGraph.recycle();
                tempGraph.recycle();
                gridGraph.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }

            bufferGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(bufferGraph);
            tempGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            tempCanvas = new Canvas(tempGraph);
            gridGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            gridCanvas = new Canvas(gridGraph);
            DrawGrid();
            Refresh();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        gvThread.setRunning(false);
        while (retry) {
            try {
                gvThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class GraphViewThread extends Thread {
        private final SurfaceHolder sHolder;
        private boolean running = false;

        public GraphViewThread(SurfaceHolder h, MeasureView v) {
            sHolder = h;
        }

        public void setRunning(boolean run) {
            running = run;
        }

        @Override
        public void run() {
            Canvas c;
            long startTime = System.currentTimeMillis();
            long checkTime = startTime;

            while (running) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                c = null;
                try {
                    c = sHolder.lockCanvas(null);
                    synchronized (sHolder) {
                        if (c == null) continue;

                        if (bufferGraph != null) {
                            synchronized (bufferCanvas) {
                                c.drawBitmap(gridGraph, 0, 0, null);
                                c.drawBitmap(bufferGraph, 0, 0, null);
                                c.drawRect(0, 0, bufferGraph.getWidth(), bufferGraph.getHeight(), borderPnt);
                            }
                        }

                        if (isRedraw) {
                            Redraw();
                            isRedraw = false;
                        }
                    }
                } finally {
                    if (c != null) {
                        sHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }


    /*private void WriteString(DataOutputStream output, String str) {
        byte[] bytes;
        int length = 0;

        try {
            bytes = str.getBytes(StandardCharsets.UTF_8);
            length = bytes.length;

            while (length >= 0x80) {
                output.writeByte((length & 0x7F) | 0x80);
                length = length >> 7;
            }

            output.writeByte((length & 0x7F));
            output.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*private void WriteString(List<String[]> list, String str) {
        byte[] bytes;
        int length = 0;

        try {
            //bytes = str.getBytes("utf-8");
            bytes = str.getBytes("UTF-8");

            length = bytes.length;

            ArrayList<String> arrayList = new ArrayList();
            while (length >= 0x80) {

                int one = (length & 0x7F) | 0x80;
                arrayList.add(String.valueOf(one));
                length = length >> 7;
            }
            arrayList.add(String.valueOf((length & 0x7F)));
            String data = new String(bytes);
            arrayList.add(data);

            String[] strings = new String[arrayList.size()];
            strings = arrayList.toArray(strings);

            list.add(strings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    //폴더 만들기
    private void CreateFolder() {
        //File f = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/");
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/");

        if (f.exists()) {
            if (!f.isDirectory()) {
                return;
            }
        } else {
            try {
                boolean ret = f.mkdirs();
                /*if (ret) Log.d(TAG, "Folder Create");
                else Log.d(TAG, "Folder Not Create");*/
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }


    //CSV 파일 저장
    public void SaveData(String wearingPart, Activity activity,
                         ArrayList<String> timeLab, String firstTime,
                         SanteApp santeApp) {


        listener = (SaveFileListener) activity;

        //this.progressDialog = progressDialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity.getApplicationContext(), "저장소 접근 권한이 없어서\n데이터가 저장되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Log(info);

        //deleteFile();

        int deviceNum = 0;
        if (!wearingPart.equals("ch1")) {
            deviceNum = 1;
        }
        saveLog(deviceNum, timeLab);
        //readCSVFile();

        export = "";

        Date nowDate = new Date(System.currentTimeMillis());
        export += UserInfo.getInstance().name;
        export += "_" + DateFormat.format("yyyyMMdd_HHmmss", nowDate).toString();
        export += "_";
        export += UserInfo.getInstance().spacial;
        export += "_";
        //export += UserInfo.getInstance().direction_of_wear;
        export += wearingPart;
        //export += ".snt";
        export += ".csv";

        /*SaveTxtThread saveTxtThread = new SaveTxtThread(wearingPart, timeLab, firstTime, santeApp);
        saveTxtThread.start();*/

        //CreateFolder();

        //saveSNT(wearingPart, timeLab, firstTime, santeApp);
        //saveCSV(wearingPart, timeLab, firstTime, santeApp);
        //saveTxt(wearingPart, timeLab, firstTime, santeApp);

        /*SaveTxtThread saveTxtThread = new SaveTxtThread(wearingPart, timeLab, firstTime, santeApp);
        saveTxtThread.start();*/

    }

    public void deleteFile() {

        /*for (String fileName : psFileName) {
            //File file = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/" + fileName);
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/I-Motion Lab/" + fileName);
            try {
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/


    }

    class SaveTxtThread extends Thread {

        private final String wearingPart;
        private final ArrayList<String> timeLab;
        private final String firstTime;
        private final SanteApp santeApp;

        public SaveTxtThread(String wearingPart, ArrayList<String> timeLab,
                             String firstTime, SanteApp santeApp) {

            this.wearingPart = wearingPart;
            this.timeLab = timeLab;
            this.firstTime = firstTime;
            this.santeApp = santeApp;

        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            super.run();


            try {

                long Time_Offset = 621355968000000000L + 9L * 60L * 60L * 1000L * 1000L * 10L;
                String saveFileName = UserInfo.getInstance().name;
                saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", new Date()).toString();
                saveFileName += UserInfo.getInstance().spacial;
                saveFileName += wearingPart + "gg.csv";

                File saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);

                FileOutputStream fileOutput = new FileOutputStream(saveFile, false);
                BufferedOutputStream bufOutput = new BufferedOutputStream(fileOutput);
                long measureTime =
                        Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L;
                bufOutput.write(String.format(measureTime + "," +
                                UserInfo.getInstance().gender + "," +
                                UserInfo.getInstance().birth + "," +
                                UserInfo.getInstance().height + "," +
                                UserInfo.getInstance().weight + "," +
                                UserInfo.getInstance().alarm + "," +
                                EMGCount + "," +
                                firstTime + ", \r\n"
                        ).getBytes()
                );

                bufOutput.write(timeLab.size());
                if (timeLab.size() > 0) {
                    for (int i = 0; i < timeLab.size(); i++) {
                        bufOutput.write(String.format(timeLab.get(i) + ",").getBytes());
                    }

                } else {
                    bufOutput.write(String.format("").getBytes());
                }
                bufOutput.write(String.format("\r\n").getBytes());

                bufOutput.write(String.format(UserInfo.getInstance().name + "\r\n").getBytes(StandardCharsets.UTF_8));
                bufOutput.write(String.format(UserInfo.getInstance().memo + "\r\n").getBytes(StandardCharsets.UTF_8));

                int deviceNum = 0;
                if (!wearingPart.equals("ch1")) {
                    deviceNum = 1;
                }

                writeDataInfo(santeApp, deviceNum, bufOutput);
                //bufOutput.write(String.format("EMG, RMS, Acc-X, Acc-Y, Acc-Z, Gyro-X, Gyro-Y, Gyro-Z\r\n").getBytes());

                float time = 0.0000F;
                float result;

                for (int i = 0; i < EMGCount; i++) {
                    int tmp = (int) Math.floor((double) i / 10.0);
                    result = (float) (Math.floor(time * 10000) / 10000);

                    //double rmsData = RMSData[i];


                    double sampleRMSData =
                            SampleRMS2(EMGData, i, santeApp.GetEMGRMS(deviceNum));

                    float fdata0 = AccData[0][tmp];
                    float fdata1 = AccData[1][tmp];
                    float fdata2 = AccData[2][tmp];

                    float fdata3 = GyroData[0][tmp];
                    float fdata4 = GyroData[1][tmp];
                    float fdata5 = GyroData[2][tmp];

                    float fdata6 = EMGData[i];
                    float fdata7 = LeadOffData[i];

                    String putData = Float.toString(time);

                    if (putData.equals("5.0E-4")) {
                        putData = "0.0005";
                    } else if (putData.equals("0.0")) {
                        putData = "0";
                    }


                    //bufOutput.write(String.format("%.4"result).getBytes());
                    bufOutput.write(
                            String.format(
                                    "%.4f, " +
                                            "%.8f, " + // 0
                                            "%.8f, " + // 1
                                            "%.8f, " + // 2
                                            "%.8f, " + // 3
                                            "%.8f, " + // 4
                                            "%.8f, " + // 5
                                            "%.8f, " + // 6
                                            "%.8f, " + // 7
                                            "%.8f\r\n", //8
                                    result, fdata0, fdata1, fdata2, fdata3,
                                    fdata4, fdata5, fdata6, fdata7,
                                    sampleRMSData
                            ).getBytes()
                    );

                    time += 0.0005F;
                }
                Log.wtf("SaveTxtThread", "saveTextFile");
                listener.onSuccess(deviceNum);

                bufOutput.flush();
                fileOutput.flush();
                bufOutput.close();
                fileOutput.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*@SuppressLint("DefaultLocale")
    private void saveTxt(String wearingPart, ArrayList<String> timeLab,
                         String firstTime, SanteApp santeApp) {


        try {

            String saveFileName = UserInfo.getInstance().name;
            saveFileName += DateFormat.format("yyyyMMdd_HHmmss_", new Date()).toString();
            saveFileName += UserInfo.getInstance().spacial;
            saveFileName += wearingPart + ".csv";

            File saveFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + saveFileName);

            FileOutputStream fileOutput = new FileOutputStream(saveFile, false);
            BufferedOutputStream bufOutput = new BufferedOutputStream(fileOutput);
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
            );

            bufOutput.write(timeLab.size());
            if (timeLab.size() > 0) {
                for (int i = 0; i < timeLab.size(); i++) {
                    bufOutput.write(String.format(timeLab.get(i) + ",").getBytes());
                }

            } else {
                bufOutput.write(String.format("").getBytes());
            }
            bufOutput.write(String.format("\r\n").getBytes());

            bufOutput.write(String.format(UserInfo.getInstance().name + "\r\n").getBytes(StandardCharsets.UTF_8));
            bufOutput.write(String.format(UserInfo.getInstance().memo + "\r\n").getBytes(StandardCharsets.UTF_8));

            int deviceNum = 0;
            if (!wearingPart.equals("ch1")) {
                deviceNum = 1;
            }

            writeDataInfo(santeApp, deviceNum, bufOutput);
            //bufOutput.write(String.format("EMG, RMS, Acc-X, Acc-Y, Acc-Z, Gyro-X, Gyro-Y, Gyro-Z\r\n").getBytes());

            float time = 0F;
            float result;

            for (int i = 0; i < EMGCount; i++) {
                int tmp = (int) Math.floor((double) i / 10.0);
                result = (float) (Math.floor(time * 10000) / 10000);

                //double rmsData = RMSData[i];


                double sampleRMSData =
                        SampleRMS2(EMGData, i, santeApp.GetEMGRMS(deviceNum));

                float fdata0 = AccData[0][tmp];
                float fdata1 = AccData[1][tmp];
                float fdata2 = AccData[2][tmp];

                float fdata3 = GyroData[0][tmp];
                float fdata4 = GyroData[1][tmp];
                float fdata5 = GyroData[2][tmp];

                float fdata6 = EMGData[i];
                float fdata7 = LeadOffData[i];

                String putData = Float.toString(time);

                if (putData.equals("5.0E-4")) {
                    putData = "0.0005";
                } else if (putData.equals("0.0")) {
                    putData = "0";
                }


                /*bufOutput.write(String.format(putData).getBytes());
                dataOutputStream.writeDouble(fdata0);
                //bufOutput.write(String.format(",").getBytes());
                //dataOutputStream.write(",".getBytes());

                dataOutputStream.writeDouble(fdata1);
                //bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata2);
                //bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata3);
                //bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata4);
                //bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata5);
                bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata6);
                bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(fdata7);
                bufOutput.write(String.format(",").getBytes());

                dataOutputStream.writeDouble(sampleRMSData);
                bufOutput.write(String.format(",").getBytes());*//*

     *//*String f0 = String.valueOf(fdata0);
                String f1 = String.valueOf(fdata1);
                String f2 = String.valueOf(fdata2);
                String f3 = String.valueOf(fdata3);
                String f4 = String.valueOf(fdata4);
                String f5 = String.valueOf(fdata5);
                String f6 = String.valueOf(fdata6);
                String f7 = String.valueOf(fdata7);
                String s1 = String.valueOf(sampleRMSData);

                bufOutput.write(
                        String.format(f0 + ",",
                                f1 + ",",
                                f2 + ",",
                                f3 + ",",
                                f4 + ",",
                                f5 + ",",
                                f6 + ",",
                                f7 + ",",
                                s1 + ",",
                                "\r\n"
                        ).getBytes()
                );*//*

                bufOutput.write(String.format(putData).getBytes());
                bufOutput.write(
                        String.format(
                                "%.8f, " + // 0
                                        "%.8f, " + // 1
                                        "%.8f, " + // 2
                                        "%.8f, " + // 3
                                        "%.8f, " + // 4
                                        "%.8f, " + // 5
                                        "%.8f, " + // 6
                                        "%.8f, " + // 7
                                        "%.8f\r\n", //8
                                fdata0, fdata1, fdata2, fdata3,
                                fdata4, fdata5, fdata6, fdata7,
                                sampleRMSData
                        ).getBytes()
                );

                time += 0.0005F;
            }

            listener.onSuccess(deviceNum);

            bufOutput.flush();
            fileOutput.flush();
            bufOutput.close();
            fileOutput.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }*/

    private void writeDataInfo(SanteApp santeApp, int deviceNum, BufferedOutputStream bufOutput) {

        String AccHPF = "None";
        String AccLPF = "None";
        String GyroHPF = "None";
        String GyroLPF = "None";
        String EMGNotch = "Off";
        String EMGHPF = "None";
        String EMGLPF = "None";
        String EMGRMS = "0.3s";

        if (santeApp.GetAccHPF(deviceNum) == 0) {
            AccHPF = "None";
        } else if (santeApp.GetAccHPF(deviceNum) == 1) {
            AccHPF = "0.5Hz";
        } else if (santeApp.GetAccHPF(deviceNum) == 2) {
            AccHPF = "1Hz";
        }

        if (santeApp.GetAccLPF(deviceNum) == 0) {
            AccLPF = "None";
        } else if (santeApp.GetAccLPF(deviceNum) == 1) {
            AccLPF = "10Hz";
        } else if (santeApp.GetAccLPF(deviceNum) == 2) {
            AccLPF = "20Hz";
        }

        if (santeApp.GetGyroHPF(deviceNum) == 0) {
            GyroHPF = "None";
        } else if (santeApp.GetGyroHPF(deviceNum) == 1) {
            GyroHPF = "0.5Hz";
        } else if (santeApp.GetGyroHPF(deviceNum) == 2) {
            GyroHPF = "1Hz";
        }


        if (santeApp.GetGyroLPF(deviceNum) == 0) {
            GyroLPF = "None";
        } else if (santeApp.GetGyroLPF(deviceNum) == 1) {
            GyroLPF = "10Hz";
        } else if (santeApp.GetGyroLPF(deviceNum) == 2) {
            GyroLPF = "20Hz";
        }


        if (santeApp.GetEMGNotch(deviceNum) == 0) {
            EMGNotch = "Notch Off";
        } else {
            EMGNotch = "Notch On";
        }

        if (santeApp.GetEMGHPF(deviceNum) == 0) {
            EMGHPF = "None";
        } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
            EMGHPF = "3Hz";
        } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
            EMGHPF = "20Hz";
        }

        if (santeApp.GetEMGHPF(deviceNum) == 0) {
            EMGLPF = "None";
        } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
            EMGLPF = "250Hz";
        } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
            EMGLPF = "500Hz";
        }

        if (santeApp.GetEMGRMS(deviceNum) == 0) {
            EMGRMS = "0.05s";
        } else if (santeApp.GetEMGRMS(deviceNum) == 1) {
            EMGRMS = "0.1s";
        } else if (santeApp.GetEMGRMS(deviceNum) == 2) {
            EMGRMS = "0.3s";
        } else if (santeApp.GetEMGRMS(deviceNum) == 3) {
            EMGRMS = "0.5s";
        } else if (santeApp.GetEMGRMS(deviceNum) == 4) {
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


    /*private void saveSNT(String wearingPart, ArrayList<String> timeLab,
                         String firstTime, SanteApp santeApp) {

        CreateFolder();

        FileOutputStream fileOutput = null;
        BufferedOutputStream bufOutput = null;
        DataOutputStream output = null;

        ByteBuffer buf = ByteBuffer.allocate(Float.BYTES);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.clear();

        boolean isExportExist = false;
        *//*
        export = "";
        if (info.name.compareTo("") != 0) {

            export = info.name;
        }
        export += "_" + DateFormat.format("yyyyMMdd_HHmmss", info.measureTime).toString();

        export += ".snt";

        CreateFolder();*//*

        File exportFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + export);

        try {
            fileOutput = new FileOutputStream(exportFile, false);
            bufOutput = new BufferedOutputStream(fileOutput, 4096);
            output = new DataOutputStream(bufOutput);
            isExportExist = true;
        } catch (IOException e) {
            e.printStackTrace();
            isExportExist = false;
        }

        if (!isExportExist) {
            try {
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                bufOutput.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                fileOutput.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            listener.onFail();
            return;
        }


        try {
            //파일구분을 위한 표시 : 6 byte
            output.writeShort((short) 0xFF12);
            output.writeShort((short) 0xFF50);
            output.writeShort((short) 0x0001);
            output.flush();

            //측정시간
            output.writeLong(Long.reverseBytes(BTService.Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L));

            output.writeBoolean(UserInfo.getInstance().gender);

            byte[] strData = UserInfo.getInstance().birth.getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            output.writeInt(Integer.parseInt(UserInfo.getInstance().height));
            output.writeInt(Integer.parseInt(UserInfo.getInstance().weight));

            output.writeBoolean(UserInfo.getInstance().alarm);
            output.writeInt(Integer.reverseBytes(EMGCount));

            strData = firstTime.getBytes(StandardCharsets.UTF_8);
            output.write(strData);


            if (timeLab != null && timeLab.size() > 0) {
                output.writeInt(timeLab.size());
                for (int i = 0; i < timeLab.size(); i++) {
                    strData = timeLab.get(i).getBytes(StandardCharsets.UTF_8);
                    output.write(strData);
                }
            }else {
                strData = "".getBytes(StandardCharsets.UTF_8);
                output.write(strData);
                output.write(strData);
            }


            *//*if (info.watchCnt < 0) {
                output.writeInt(Integer.reverseBytes(0));

            } else {
                output.writeInt(Integer.reverseBytes(info.watchCnt));
            }*//*
            output.flush();

            String LINE_SEPERATOR = System.getProperty("line.separator");

            WriteString(output, UserInfo.getInstance().name.replace(LINE_SEPERATOR, ""));
            WriteString(output, UserInfo.getInstance().memo.replace(LINE_SEPERATOR, "\r\n"));
            output.flush();

            fileOutput.getChannel().position(2048);

            output.flush();

            int deviceNum = 0;
            if (!wearingPart.equals("ch1")) {
                deviceNum = 1;
            }

            String AccHPF = "None";
            String AccLPF = "None";
            String GyroHPF = "None";
            String GyroLPF = "None";
            String EMGNotch = "Off";
            String EMGHPF = "None";
            String EMGLPF = "None";
            String EMGRMS = "0.3s";

            if (santeApp.GetAccHPF(deviceNum) == 0) {
                AccHPF = "None";
            } else if (santeApp.GetAccHPF(deviceNum) == 1) {
                AccHPF = "0.5Hz";
            } else if (santeApp.GetAccHPF(deviceNum) == 2) {
                AccHPF = "1Hz";
            }

            if (santeApp.GetAccLPF(deviceNum) == 0) {
                AccLPF = "None";
            } else if (santeApp.GetAccLPF(deviceNum) == 1) {
                AccLPF = "10Hz";
            } else if (santeApp.GetAccLPF(deviceNum) == 2) {
                AccLPF = "20Hz";
            }

            if (santeApp.GetGyroHPF(deviceNum) == 0) {
                GyroHPF = "None";
            } else if (santeApp.GetGyroHPF(deviceNum) == 1) {
                GyroHPF = "0.5Hz";
            } else if (santeApp.GetGyroHPF(deviceNum) == 2) {
                GyroHPF = "1Hz";
            }


            if (santeApp.GetGyroLPF(deviceNum) == 0) {
                GyroLPF = "None";
            } else if (santeApp.GetGyroLPF(deviceNum) == 1) {
                GyroLPF = "10Hz";
            } else if (santeApp.GetGyroLPF(deviceNum) == 2) {
                GyroLPF = "20Hz";
            }


            if (santeApp.GetEMGNotch(deviceNum) == 0) {
                EMGNotch = "Notch Off";
            } else {
                EMGNotch = "Notch On";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGHPF = "None";
            } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
                EMGHPF = "3Hz";
            } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGHPF = "20Hz";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGLPF = "None";
            } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
                EMGLPF = "250Hz";
            } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGLPF = "500Hz";
            }

            if (santeApp.GetEMGRMS(deviceNum) == 0) {
                EMGRMS = "0.05s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 1) {
                EMGRMS = "0.1s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 2) {
                EMGRMS = "0.3s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 3) {
                EMGRMS = "0.5s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 4) {
                EMGRMS = "1s";
            }

            strData = "".getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "Acc HPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = AccHPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "Acc LPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = AccLPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "Gyro HPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = GyroHPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "Gyro LPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = GyroLPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "EMG Notch".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = EMGNotch.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "EMG HPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = EMGHPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "EMG LPF".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = EMGLPF.getBytes(StandardCharsets.UTF_8);
            output.write(strData);

            strData = "EMG RMS".getBytes(StandardCharsets.UTF_8);
            output.write(strData);
            strData = EMGRMS.getBytes(StandardCharsets.UTF_8);
            output.write(strData);



            for (int i = 0; i < EMGCount; i++) {
                int tmp = (int) Math.floor((double) i / 10.0);

                buf.clear();

                buf.putFloat(AccData[0][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);
                buf.clear();

                buf.putFloat(AccData[1][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);
                buf.clear();

                buf.putFloat(AccData[2][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                buf.clear();
                buf.putFloat(GyroData[0][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                buf.clear();
                buf.putFloat(GyroData[1][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                buf.clear();
                buf.putFloat(GyroData[2][tmp]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                buf.clear();
                buf.putFloat(EMGData[i]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                buf.clear();
                buf.putFloat(LeadOffData[i]);
                bufOutput.write(buf.array(), 0, Float.BYTES);

                float sampleRMSData = (float) SampleRMS2(EMGData, i, santeApp.GetEMGRMS(deviceNum));
                buf.clear();
                buf.putFloat(sampleRMSData);
                bufOutput.write(buf.array(), 0, Float.BYTES);


            }
            listener.onSuccess(deviceNum);


        } catch (Exception e) {
            e.printStackTrace();
            listener.onFail();
            return;
        }

        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            bufOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            fileOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        *//*Toast toast = Toast.makeText(context, "데이터 저장이 완료되었습니다.", Toast.LENGTH_SHORT);

        toast.setGravity(Gravity.CENTER, 0, 0);


        toast.show();*//*


    }*/

    /*private void saveCSV(String wearingPart, ArrayList<String> timeLab,
                         String firstTime, SanteApp santeApp) {
        try {
            CreateFolder();
            //File exportFile = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/" + export);
            File exportFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + export);

            FileOutputStream fos = new FileOutputStream(exportFile);
            Writer out = new OutputStreamWriter(fos, "EUC-KR");

            CSVWriter writer = new CSVWriter(out);

            List<String[]> data = new ArrayList<>();
            long Time_Offset = 621355968000000000L+9L*60L*60L*1000L*1000L*10L;
            data.add(new String[]{
                    String.valueOf(Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L)
                    , String.valueOf(UserInfo.getInstance().gender),
                    UserInfo.getInstance().birth,
                    UserInfo.getInstance().height,
                    UserInfo.getInstance().weight,
                    String.valueOf(UserInfo.getInstance().alarm),
                    String.valueOf(EMGCount), firstTime, "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", ""
            });

            if (timeLab != null && timeLab.size() > 0) {
                timeLab.add(0, String.valueOf(timeLab.size()));
                String[] realRaay = new String[timeLab.size()];
                realRaay = timeLab.toArray(realRaay);

                data.add(realRaay);

            } else {
                data.add(new String[]{"", ""});
            }

            String LINE_SEPERATOR = System.getProperty("line.separator");
            if (LINE_SEPERATOR != null) {
                WriteString(data, UserInfo.getInstance().name.replace(LINE_SEPERATOR, ""));
                WriteString(data, UserInfo.getInstance().memo.replace(LINE_SEPERATOR, "\r\n"));
            }

            int deviceNum = 0;
            if (!wearingPart.equals("ch1")) {
                deviceNum = 1;
            }

            String AccHPF = "None";
            String AccLPF = "None";
            String GyroHPF = "None";
            String GyroLPF = "None";
            String EMGNotch = "Off";
            String EMGHPF = "None";
            String EMGLPF = "None";
            String EMGRMS = "0.3s";

            if (santeApp.GetAccHPF(deviceNum) == 0) {
                AccHPF = "None";
            } else if (santeApp.GetAccHPF(deviceNum) == 1) {
                AccHPF = "0.5Hz";
            } else if (santeApp.GetAccHPF(deviceNum) == 2) {
                AccHPF = "1Hz";
            }

            if (santeApp.GetAccLPF(deviceNum) == 0) {
                AccLPF = "None";
            } else if (santeApp.GetAccLPF(deviceNum) == 1) {
                AccLPF = "10Hz";
            } else if (santeApp.GetAccLPF(deviceNum) == 2) {
                AccLPF = "20Hz";
            }

            if (santeApp.GetGyroHPF(deviceNum) == 0) {
                GyroHPF = "None";
            } else if (santeApp.GetGyroHPF(deviceNum) == 1) {
                GyroHPF = "0.5Hz";
            } else if (santeApp.GetGyroHPF(deviceNum) == 2) {
                GyroHPF = "1Hz";
            }


            if (santeApp.GetGyroLPF(deviceNum) == 0) {
                GyroLPF = "None";
            } else if (santeApp.GetGyroLPF(deviceNum) == 1) {
                GyroLPF = "10Hz";
            } else if (santeApp.GetGyroLPF(deviceNum) == 2) {
                GyroLPF = "20Hz";
            }


            if (santeApp.GetEMGNotch(deviceNum) == 0) {
                EMGNotch = "Notch Off";
            } else {
                EMGNotch = "Notch On";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGHPF = "None";
            } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
                EMGHPF = "3Hz";
            } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGHPF = "20Hz";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGLPF = "None";
            } else if (santeApp.GetEMGHPF(deviceNum) == 1) {
                EMGLPF = "250Hz";
            } else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGLPF = "500Hz";
            }

            if (santeApp.GetEMGRMS(deviceNum) == 0) {
                EMGRMS = "0.05s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 1) {
                EMGRMS = "0.1s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 2) {
                EMGRMS = "0.3s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 3) {
                EMGRMS = "0.5s";
            } else if (santeApp.GetEMGRMS(deviceNum) == 4) {
                EMGRMS = "1s";
            }

            String[] defaltSettingData = new String[]{
                    "",
                    "Acc HPF", AccHPF,
                    "Acc LPF", AccLPF,
                    "Gyro HPF", GyroHPF,
                    "Gyro LPF", GyroLPF,
                    "EMG Notch", EMGNotch,
                    "EMG HPF", EMGHPF,
                    "EMG LPF", EMGLPF,
                    "EMG RMS", EMGRMS
                    //, String.valueOf(santeApp.GetEMGRMS(deviceNum))
            };
            data.add(defaltSettingData);

            float time = 0F;
            float result;


            *//*Log.wtf("EMGCount", String.valueOf(EMGCount));
            Log.wtf("EMGData.length", String.valueOf(EMGData.length));*//*
            for (int i = 0; i < EMGCount; i++) {

                int tmp = (int) Math.floor((double) i / 10.0);
                result = (float) (Math.floor(time * 10000) / 10000);

                //double rmsData = RMSData[i];


                double sampleRMSData = SampleRMSData[i];

                //sampleRMSData = SampleRMS(EMGData, i);
                *//*Log.wtf("santeApp.GetEMGRMS(deviceNum)",
                        String.valueOf(santeApp.GetEMGRMS(deviceNum)));*//*
                sampleRMSData = SampleRMS2(EMGData, i, santeApp.GetEMGRMS(deviceNum));

                float fdata0 = AccData[0][tmp];
                float fdata1 = AccData[1][tmp];
                float fdata2 = AccData[2][tmp];

                float fdata3 = GyroData[0][tmp];
                float fdata4 = GyroData[1][tmp];
                float fdata5 = GyroData[2][tmp];

                float fdata6 = EMGData[i];
                float fdata7 = LeadOffData[i];
                String putData = Float.toString(result);
                if (putData.equals("5.0E-4")) {
                    putData = "0.0005";
                } else if (putData.equals("0.0")) {
                    putData = "0";
                }

                data.add(new String[]{putData, Float.toString(fdata0), Float.toString(fdata1),
                        Float.toString(fdata2), Float.toString(fdata3), Float.toString(fdata4),
                        Float.toString(fdata5), Float.toString(fdata6), Float.toString(fdata7),
                        Double.toString(sampleRMSData)
                });

                time += 0.0005F;
            }

            writer.writeAll(data);
            writer.close();
            //int device = wearingPart.equals("ch1") ? 0 : 1;
            listener.onSuccess(deviceNum);


        } catch (Exception e) {
            e.printStackTrace();
            listener.onFail();
        }

    }*/


    @SuppressLint("DefaultLocale")
    private void saveLog(int deviceNum, ArrayList<String> timeLab) {

        String outputStr = "";
        String str = "";

        //File logFile = new File(getContext().getExternalFilesDir(null), "/I-Motion Lab/" + "/I_Motion_TUG.log");
        File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + "/I_Motion_TUG.log");


        if (!logFile.exists()) {
            CreateLogFile();
        }

        FileOutputStream fileOutput = null;
        BufferedWriter bufWriter = null;

        try {
            fileOutput = new FileOutputStream(logFile, true);
            bufWriter = new BufferedWriter(new OutputStreamWriter(fileOutput));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        outputStr = DateFormat.format("yyyyMMdd", UserInfo.getInstance().measureTime).toString() + ", ";
        outputStr += DateFormat.format("HH", UserInfo.getInstance().measureTime).toString() + ", ";
        outputStr += DateFormat.format("mm", UserInfo.getInstance().measureTime).toString() + ", ";
        outputStr += UserInfo.getInstance().name.replace(",", " ") + ", ";
        outputStr += UserInfo.getInstance().birth + ", ";
        if (UserInfo.getInstance().gender) outputStr += "남" + ", ";
        else outputStr += "여" + ", ";

        outputStr += "tug1" + ", ";

        str = UserInfo.getInstance().memo.replace(",", " ");
        str = str.replace("\r", " ");
        str = str.replace("\n", " ");
        outputStr += str + ", ";

        str = "";
        outputStr += UserInfo.getInstance().spacial + ", ";
        if (UserInfo.getInstance().watchCnt <= 0) {
            str = "00:00.00";
        } else {
            int minute = 0;
            int second = 0;
            int milliSecond = 0;

            milliSecond = (int) Math.floor((double) UserInfo.getInstance().watchCnt / (double) BTService.SAMPLE_RATE * 100.0);
            second = (int) Math.floor((double) milliSecond / 100.0);
            milliSecond = milliSecond % 100;
            minute = (int) Math.floor((double) second / 60.0);
            second = second % 60;
            minute = minute % 60;

            str = String.format("%02d:%02d:%02d", minute, second, milliSecond);
        }
        outputStr += str + ", ";


        if (UserInfo.getInstance().alarm) outputStr += "On" + ", ";
        else outputStr += "Off" + ", ";

        /*if (UserInfo.getInstance().leadoff) outputStr += "Yes" + "\r\n";
        else outputStr += "No" + "\r\n";*/

        if (UserInfo.getInstance().leadoff) outputStr += "Yes";
        else outputStr += "No";


        if (timeLab.size() > 0) {
            for (int i = 0; i < timeLab.size(); i++) {
                if (i == timeLab.size() - 1) {
                    outputStr += ", " + timeLab.get(i) + "\r\n";
                } else {
                    outputStr += ", " + timeLab.get(i);
                }

            }
        } else {
            outputStr += "\r\n";
        }


        try {
            bufWriter.write(outputStr);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFail();
        }

        try {
            bufWriter.close();
            fileOutput.close();
            //여기 데이터 저장
            //listener.onSuccess(deviceNum);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFail();
        }


    }


    private void CreateLogFile() {
        String outputStr = "";
        CreateFolder();


        //File logFile = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/" + "/I_Motion_TUG.log");
        File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/I-Motion Lab/" + "/I_Motion_TUG.log");

        FileOutputStream fileOutput = null;
        BufferedWriter bufWriter = null;

        try {
            fileOutput = new FileOutputStream(logFile, false);
            bufWriter = new BufferedWriter(new OutputStreamWriter(fileOutput));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        outputStr = "시험날짜, ";
        outputStr += "시간, ";
        outputStr += "분, ";
        outputStr += "이름, ";
        outputStr += "나이, ";
        outputStr += "성별, ";
        outputStr += "측정지, ";
        outputStr += "특이사항, ";
        outputStr += "테스트명, ";
        outputStr += "수행시간, ";
        outputStr += "3초알림, ";
        outputStr += "Lead off\r\n ";


        try {
            bufWriter.write(outputStr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            bufWriter.close();
            fileOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private float[] sEMG_MVC_RMS(float[] MVC2, float ct, int dt) {
        int MN = MVC2.length;
        float[] rms_mvc = new float[MN];
        float[] sqr_mvc = new float[MN];
        for (int i = 0; i < MN; i++) {
            sqr_mvc[i] = MVC2[i] * MVC2[i];
        }
        int cut_N = Math.round(ct / dt);
        for (int i = 0; i < MN; i++) {
            if (i < cut_N / 2) {
//                rms_mvc[i] =

            }

        }

        return rms_mvc;
    }

    private double prevResult = 0D;

    /*public float SampleRMS3(float[] EMGData, int size) {
        int rmsSetting = santeApps.GetEMGRMS(0);
        int conInt;
        if (rmsSetting == 1) {
            conInt = 2;
        } else if (rmsSetting == 2) {
            conInt = 5;
        } else if (rmsSetting == 3) {
            conInt = 9;
        } else if (rmsSetting == 4) {
            conInt = 19;
        } else {
            conInt = 0;
        }

        float result = 0;
        if (size > 0) {
            for (int i = 0; i < m; i++) {
                if ((i & conInt) != 0) {

                }else {

                }
                result += Math.pow(EMGData[i], 2);
                //result += Math.pow(EMGData[count - (m - i)], 2);
            }
            result = (float) Math.sqrt(result / 600);
            return result;

        } else return 0;



    }*/

    public double SampleRMS2(float[] EMGData, int size, int sec) {
        double result = 0;
        int conInt = 100;
        /*if (sec == 0) {
            conInt = 10;
        } else */

        if (sec == 1) {
            conInt = 200;
        } else if (sec == 2) {
            conInt = 600;
        } else if (sec == 3) {
            conInt = 1000;
        } else if (sec == 4) {
            conInt = 2000;
        }


        /*if (sec == 1) {
            conInt = 2;
        } else if (sec == 2) {
            conInt = 5;
        } else if (sec == 3) {
            conInt = 9;
        } else if (sec == 4) {
            conInt = 19;
        } else {
            conInt = 0;
        }*/

        if (EMGData.length < conInt) {
            for (int i = 0; i < EMGData.length; i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = Math.sqrt(result / EMGData.length);
        } else if (size < (conInt / 2)) {
            /** 0번째 부터 시작
             * EMGData 의 i - conInt 가 0 보다 작음*/
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

        /*if (size < 5) {
            // 0 번째 데이터 ~ 4번 까지
            if (conInt == 0 || size % conInt == 0) {
                for (int i = 0; i < 10; i++) {
                    result += Math.pow(EMGData[i], 2);
                }
            } else {
                result = prevResult;
            }
            prevResult = result;

        } else if (EMGData.length - 5 < size) {
            // 마지막 -5 번째 데이터 ~ 마지막 까지
            for (int i = EMGData.length - 10; i < EMGData.length; i++) {
                if (conInt != 0 && size % conInt != 0) {
                    result = prevResult;
                } else {
                    result += Math.pow(EMGData[i], 2);
                }
                prevResult = result;

            }

        } else {
            // 중간 데이터
            for (int i = size - 5; i < size + 6; i++) {
                if (conInt != 0 && size % conInt != 0) {
                    result = prevResult;
                } else {
                    result += Math.pow(EMGData[i], 2);
                }
                prevResult = result;
            }

        }
        result = (double) Math.sqrt(result / 11);*/
        return result;
    }

    /*RMS(EMGData,600,i)*/
    public float RMS(float[] EMGData, int m, int count) {
        float result = 0;
        if (count > 0) {
            for (int i = 0; i < m; i++) {
                if (count < m)
                    break;
                result += Math.pow(EMGData[count - (m - i)], 2);
            }
            result = (float) Math.sqrt(result / m);
            return result;

        } else return 0;

    }


    public double SampleRMS(float[] EMGData, int few) {
        double result = 0;
        if (few < 5) {
            for (int i = 0; i < 10; i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = (double) Math.sqrt(result / 11);
            return result;
        } else if (EMGData.length - 5 < few) {
            for (int i = EMGData.length - 10; i < EMGData.length; i++) {
                result += Math.pow(EMGData[i], 2);
            }
            result = (double) Math.sqrt(result / 11);
            return result;
        } else {
            for (int i = few - 5; i < few + 6; i++) {
                result += Math.pow(EMGData[i], 2);
            }

            result = (double) Math.sqrt(result / 11);
            return result;
        }

    }
}
