package com.physiolab.sante.santemulti;

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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.opencsv.CSVWriter;
import com.physiolab.sante.BlueToothService.BTService;
import com.physiolab.sante.SanteApp;
import com.physiolab.sante.UserInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
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
    /** 우선 MIN, MAX EMG와 동일하게*/

    private float EMGYMax = 1000.0f;
    private float EMGYMin = -1000.0f;
    private float AccYMax = 6.0f;
    private float AccYMin = -6.0f;
    private float GyroYMax = 800.0f;
    private float GyroYMin = -400.0f;
    private float TimeStart = 0.0f;
    private float TimeRange = 30.0f;

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



    public MeasureView(Context context) {
        super(context);
        mContext = context;

        SurfaceHolder mHolder;
        mHolder = getHolder();
        mHolder.addCallback(this);

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

    public void SetTimeRange(float value) {
        TimeRange = value;
        if (TimeStart + TimeRange < (float) EMGCount / (float) BTService.SAMPLE_RATE) {
            TimeStart = (float) EMGCount / (float) BTService.SAMPLE_RATE - TimeRange;
        }

        if (TimeStart + TimeRange > 300.0f) {
            TimeStart = 300.0f - TimeRange;
        }

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

                tempCanvas.drawColor(ContextCompat.getColor(getContext(), android.R.color.transparent), PorterDuff.Mode.SRC);
                //tempCanvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.SRC);
                tempCanvas.drawBitmap(bufferGraph, 0, 0, null);


                bufferCanvas.drawColor(ContextCompat.getColor(getContext(), android.R.color.transparent), PorterDuff.Mode.SRC);
                //bufferCanvas.drawColor(getResources().getColor(android.R.color.transparent), PorterDuff.Mode.SRC);
                bufferCanvas.drawBitmap(tempGraph, (int) Math.ceil(diff) * -1, 0, null);


                diff = diff - Math.ceil(diff);
            }



            if (EMGRMSEnable) {
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
                    yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }

                bufferCanvas.drawPath(path, RMSPnt);


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
                    yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }

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
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) dataCount / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);
                    } else {
                        yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][dataCount - refresh]) / (AccYMax - AccYMin));
                        xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - refresh) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
                        path.moveTo(xPos, yPos);

                        for (int i = refresh - 1; i > 0; i--) {
                            yPos = bufferGraph.getHeight() * ((AccYMax - AccData[axis][dataCount - i]) / (AccYMax - AccYMin));
                            xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) (dataCount - i) / (float) (BTService.SAMPLE_RATE / 10)) - TimeStart) / (TimeRange));
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

            if (EMGRMSEnable) {
                Log.wtf("EMGRMSEnable", "true");
                xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                xMaxCount = (int) Math.min(Math.ceil((double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE), (double) RMSCount);


                path.reset();

                yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[xMinCount]) / (RMSMax - RMSMin)));
                xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) xMinCount / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));


                path.moveTo(xPos, yPos);

                for (int i = xMinCount + 1; i < xMaxCount; i++) {
                    yPos = (float) (bufferGraph.getHeight() * ((RMSMax - RMSData[i]) / (RMSMax - RMSMin)));
                    xPos = 2 + (bufferGraph.getWidth() - 4) * ((((float) i / (float) BTService.SAMPLE_RATE) - TimeStart) / (TimeRange));

                    path.lineTo(xPos, yPos);
                }
                bufferCanvas.drawPath(path, RMSPnt);
            }else {
                Log.wtf("EMGRMSEnable", "false");
            }

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

                    yPos = bufferGraph.getHeight() * ((AccYMax - AccData[j][xMinCount]) / (AccYMax - AccYMin));
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

        }

        Log.d(TAG, "Redraw Stop");
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
            Log.d(TAG, "thread constructor");
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

   /* private void WriteString(Writer output, String str)
    {
        byte[] bytes;
        int length=0;

        try {
            bytes = str.getBytes("ANSI");
            length = bytes.length;

            while (length>=0x80)
            {
                output.write((length & 0x7F) | 0x80);
                length = length>>7;
            }

            output.write((length & 0x7F));
            output.write(bytes.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }*/


    private void WriteString(List<String[]> list, String str) {
        byte[] bytes;
        int length = 0;

        try {
            //bytes = str.getBytes("utf-8");
            bytes = str.getBytes("UTF-8");

            length = bytes.length;

            ArrayList<String> arrayList = new ArrayList();
            while (length >= 0x80) {

                int one =  (length & 0x7F) | 0x80;
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
    }

    //폴더 만들기
    private void CreateFolder() {
        File f = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/");
        //File f = new File(getContext().getFilesDir(), "/Sante/");
        //File f = new File(getContext().getExternalFilesDir(null) + "/Sante/");

        if (f.exists()) {
            if (!f.isDirectory()) {
                return;
            }
        } else {
            try {
                boolean ret = f.mkdirs();
                if (ret) Log.d(TAG, "Folder Create");
                else Log.d(TAG, "Folder Not Create");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

/*    private void testLog(MeasureInfo info){
        File file = new File(getContext().getExternalFilesDir("Sante"), "Sante_TUG.log");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

    }*/
/*
    //Log 파일 저장1
    @SuppressLint("DefaultLocale")
    private void Log(MeasureInfo info) {
        String outputStr = "";
        String str = "";

        File logFile = new File(getContext().getExternalFilesDir(null), "/Sante/" + "/Sante_TUG.log");


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

        outputStr = DateFormat.format("yyyyMMdd", info.measureTime).toString() + ", ";
        outputStr += DateFormat.format("HH", info.measureTime).toString() + ", ";
        outputStr += DateFormat.format("mm", info.measureTime).toString() + ", ";
        outputStr += info.name.replace(",", " ") + ", ";
        outputStr += info.age + ", ";
        if (info.gender) outputStr += "남" + ", ";
        else outputStr += "여" + ", ";
//        outputStr += info.location.replace(","," ") + ", ";

        str = info.memo.replace(",", " ");
        str = str.replace("\r", " ");
        str = str.replace("\n", " ");
        outputStr += str + ", ";

        str = "";
        if (info.watchCnt <= 0) {
            str = "00:00.00";
        } else {
            int minute = 0;
            int second = 0;
            int milliSecond = 0;

            milliSecond = (int) Math.floor((double) info.watchCnt / (double) BTService.SAMPLE_RATE * 100.0);
            second = (int) Math.floor((double) milliSecond / 100.0);
            milliSecond = milliSecond % 100;
            minute = (int) Math.floor((double) second / 60.0);
            second = second % 60;
            minute = minute % 60;

            str = String.format("%02d:%02d:%02d", minute, second, milliSecond);
        }
        outputStr += str + ", ";


        if (info.alarm) outputStr += "On" + ", ";
        else outputStr += "Off" + ", ";
        if (info.leadoff) outputStr += "Yes" + "\r\n";
        else outputStr += "No" + "\r\n";

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

    //Log 파일 저장2
    private void CreateLogFile() {
        String outputStr = "";
        CreateFolder();


        File logFile = new File(getContext().getExternalFilesDir(null) + "/Sante/" + "/Sante_TUG.log");

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
        outputStr += "수행시간, ";
        outputStr += "3초알림, ";
        outputStr += "Lead off\r\n";

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
    }*/


    //CSV 파일 저장
    public void SaveData(String wearingPart, Activity activity,
                         ArrayList<String> timeLab, String firstTime,
                         SanteApp santeApp) {
        Log.wtf("SaveData", "4444444444444444" + wearingPart);
        listener = (SaveFileListener) activity;

        //this.progressDialog = progressDialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity.getApplicationContext(), "저장소 접근 권한이 없어서\n데이터가 저장되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Log(info);

        saveLog();

        boolean isExportExist = false;
        export = "";
        /*if (info.name.compareTo("") != 0) {

            export = info.name;
        }*/

        Date nowDate = new Date(System.currentTimeMillis());
        export += UserInfo.getInstance().name;
        export += "_" + DateFormat.format("yyyyMMdd_HHmmss", nowDate).toString();
        export += "_";
        export += UserInfo.getInstance().spacial;
        export += "_";
        //export += UserInfo.getInstance().direction_of_wear;
        export += wearingPart;
        export += ".csv";

        //CreateFolder();


        saveCSV(wearingPart, timeLab, firstTime, santeApp);


        /*try {

            File exportFile = new File(export);
            exportFile.mkdirs();

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, export);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            //values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            Uri extVolumeUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            *//*Uri extVolumeUri =

                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "";*//*
            //Uri fileUri = context.getContentResolver().insert(extVolumeUri, values);
            Uri fileUri = activity.getApplicationContext().getContentResolver().insert(
                    extVolumeUri
                    , values);


            FileOutputStream fos = new FileOutputStream(activity.getApplicationContext().getContentResolver().openFileDescriptor(fileUri,
                    "w", null).getFileDescriptor());




            //FileOutputStream fos = new FileOutputStream(fileUri.toString());
            Writer out = new OutputStreamWriter(fos, "UTF-8");
            CSVWriter writer = new CSVWriter(out);

            List<String[]> data = new ArrayList<>();

            data.add(new String[]{String.valueOf(BTService.Time_Offset + nowDate.getTime() * 10000L)
                    , String.valueOf(EMGCount),
                    "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", ""
            });

            if (timeLab != null && timeLab.size() > 0){
                timeLab.add(0, String.valueOf(timeLab.size()));
                String[] realRaay = new String[timeLab.size()];
                realRaay = timeLab.toArray(realRaay);

                data.add(realRaay);

            }


           *//* String LINE_SEPERATOR = System.getProperty("line.separator");
            WriteString(data, info.name.replace(LINE_SEPERATOR, ""));
            WriteString(data, info.memo.replace(LINE_SEPERATOR, "\r\n"));*//*


            float time = 0F;
            float result;

            for (int i = 0; i < EMGCount; i++){

                int tmp = (int) Math.floor((double) i / 10.0);
                result = (float) (Math.floor(time * 10000)/10000);


                *//*bufOutput.write(
                        String.format("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f\r\n",
                                data.Filted[i],data.RMS[i],data.Acc[0][index],data.Acc[1][index],
                                data.Acc[2][index],data.Gyro[0][index],data.Gyro[1][index],data.Gyro[2][index])
                                .getBytes());*//*


                double rmsData = RMSData[tmp];

                float fdata0 = AccData[0][tmp];
                float fdata1 = AccData[1][tmp];
                float fdata2 = AccData[2][tmp];

                float fdata3 = GyroData[0][tmp];
                float fdata4 = GyroData[1][tmp];
                float fdata5 = GyroData[2][tmp];

                float fdata6 = EMGData[i];
                float fdata7 = LeadOffData[i];
                String putData = Float.toString(result);
                if (putData.equals("5.0E-4")){
                    putData = "0.0005";
                }else if (putData.equals("0.0")){
                    putData = "0";
                }

                data.add(new String[]{ putData, Float.toString(fdata0), Float.toString(fdata1),
                        Float.toString(fdata2), Float.toString(fdata3), Float.toString(fdata4),
                        Float.toString(fdata5),  Float.toString(fdata6), Float.toString(fdata7)*//*,
                        Double.toString(rmsData)*//*
                });
                //Log.d("time?????", String.valueOf(time));
                time += 0.0005F;
            }

            writer.writeAll(data);
            writer.close();
            int device = wearingPart.equals("right")? 0 : 1;
            listener.onSuccess(device);




        } catch (Exception e) {
            e.printStackTrace();
            listener.onFail();
            return;
        }*/


    }

    private void saveCSV(String wearingPart, ArrayList<String> timeLab, String firstTime, SanteApp santeApp){
        try {
            File exportFile = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/" + export);
            FileOutputStream fos = new FileOutputStream(exportFile);
            Writer out = new OutputStreamWriter(fos, "EUC-KR");

            //BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile)));
            /*bw.write((BTService.Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L) +
                    "," + UserInfo.getInstance().gender + "," + UserInfo.getInstance().birth + ","
                    + UserInfo.getInstance().height + ", " + UserInfo.getInstance().weight + "," + UserInfo.getInstance().alarm
                    + EMGCount + ", " + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," +
                    "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "" + "," +
            );

             bw.write("/");*/

            CSVWriter writer = new CSVWriter(out);

            List<String[]> data = new ArrayList<>();

            data.add(new String[]{String.valueOf(BTService.Time_Offset + UserInfo.getInstance().measureTime.getTime() * 10000L)
                    , String.valueOf(UserInfo.getInstance().gender), UserInfo.getInstance().birth,
                    UserInfo.getInstance().height, UserInfo.getInstance().weight, String.valueOf(UserInfo.getInstance().alarm),
                    String.valueOf(EMGCount),firstTime,  "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", ""
            });

            if (timeLab != null && timeLab.size() > 0){
                timeLab.add(0, String.valueOf(timeLab.size()));
                String[] realRaay = new String[timeLab.size()];
                realRaay = timeLab.toArray(realRaay);

                data.add(realRaay);

            }else if (timeLab.size() == 0) {
                timeLab.add("");
                timeLab.add("");
                String[] realRaay = new String[timeLab.size()];
                realRaay = timeLab.toArray(realRaay);

                data.add(realRaay);
            }

            String LINE_SEPERATOR = System.getProperty("line.separator");
            if (LINE_SEPERATOR != null){
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
            String EMGRMS = "0";

            if (santeApp.GetAccHPF(deviceNum) == 0) {
                AccHPF = "None";
            }else if (santeApp.GetAccHPF(deviceNum) == 1) {
                AccHPF = "0.5Hz";
            }else if (santeApp.GetAccHPF(deviceNum) == 2) {
                AccHPF = "1Hz";
            }

            if (santeApp.GetAccLPF(deviceNum) == 0) {
                AccLPF = "None";
            }else if (santeApp.GetAccLPF(deviceNum) == 1) {
                AccLPF = "10Hz";
            }else if (santeApp.GetAccLPF(deviceNum) == 2) {
                AccLPF = "20Hz";
            }

            if (santeApp.GetGyroHPF(deviceNum) == 0) {
                GyroHPF = "None";
            }else if (santeApp.GetGyroHPF(deviceNum) == 1){
                GyroHPF = "0.5Hz";
            }else if (santeApp.GetGyroHPF(deviceNum) == 2) {
                GyroHPF = "1Hz";
            }


            if (santeApp.GetGyroLPF(deviceNum) == 0) {
                GyroLPF = "None";
            }else if (santeApp.GetGyroLPF(deviceNum) == 1) {
                GyroLPF = "10Hz";
            }else if (santeApp.GetGyroLPF(deviceNum) == 2) {
                GyroLPF = "20Hz";
            }


            if (santeApp.GetEMGNotch(deviceNum) == 0) {
                EMGNotch = "Notch Off";
            }else {
                EMGNotch = "Notch On";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGHPF = "None";
            }else if (santeApp.GetEMGHPF(deviceNum) == 1){
                EMGHPF = "3Hz";
            }else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGHPF = "20Hz";
            }

            if (santeApp.GetEMGHPF(deviceNum) == 0) {
                EMGLPF = "None";
            }else if (santeApp.GetEMGHPF(deviceNum) == 1) {
                EMGLPF = "250Hz";
            }else if (santeApp.GetEMGHPF(deviceNum) == 2) {
                EMGLPF = "500Hz";
            }


            String[] defaltSettingData = new String[]{
                    "",
                    "Acc HPF " + AccHPF,
                    "Acc LPF " + AccLPF,
                    "Gyro HPF " + GyroHPF,
                    "Gyro LPF " + GyroLPF,
                    "EMG Notch " + EMGNotch,
                    "EMG HPF " + EMGHPF,
                    "EMG LPF " + EMGLPF,
                    String.valueOf(santeApp.GetEMGRMS(deviceNum))
            };
            data.add(defaltSettingData);

            float time = 0F;
            float result;




            for (int i = 0; i < EMGCount; i++){

                int tmp = (int) Math.floor((double) i / 10.0);
                result = (float) (Math.floor(time * 10000)/10000);

                double rmsData = RMSData[i];


                double sampleRMSData = SampleRMSData[i];
                /*int xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
                int xMaxCount = (int) Math.min(
                        Math.ceil(
                                (double) (TimeStart + TimeRange) * (double) BTService.SAMPLE_RATE
                        ), (double) EMGCount);

                for (int j = xMinCount + 1; j < xMaxCount; j++) {
                    //Log.wtf("EMGEnable!!!", "여기옴?11111111");
                    sampleRMSData = bufferGraph.getHeight() * ((EMGYMax - RMS(EMGData,600,j)) / (EMGYMax - EMGYMin));

                }*/



                /*xMinCount = (int) Math.max(Math.floor((double) TimeStart * (double) BTService.SAMPLE_RATE), 0.0);
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
                bufferCanvas.drawPath(path, EMGPnt);*/

                sampleRMSData = SampleRMS(EMGData, i);

                float fdata0 = AccData[0][tmp];
                float fdata1 = AccData[1][tmp];
                float fdata2 = AccData[2][tmp];

                float fdata3 = GyroData[0][tmp];
                float fdata4 = GyroData[1][tmp];
                float fdata5 = GyroData[2][tmp];

                float fdata6 = EMGData[i];
                float fdata7 = LeadOffData[i];
                String putData = Float.toString(result);
                if (putData.equals("5.0E-4")){
                    putData = "0.0005";
                }else if (putData.equals("0.0")){
                    putData = "0";
                }

                data.add(new String[]{ putData, Float.toString(fdata0), Float.toString(fdata1),
                        Float.toString(fdata2), Float.toString(fdata3), Float.toString(fdata4),
                        Float.toString(fdata5),  Float.toString(fdata6), Float.toString(fdata7),
                        Double.toString(sampleRMSData)
                });

                /*data.add(new String[]{ putData, Float.toString(fdata0), Float.toString(fdata1),
                        Float.toString(fdata2), Float.toString(fdata3), Float.toString(fdata4),
                        Float.toString(fdata5),  Float.toString(fdata6), Float.toString(fdata7),
                        Double.toString(rmsData), Double.toString(sampleRMSData)
                });*/
                //Log.d("time?????", String.valueOf(time));
                time += 0.0005F;
            }

            writer.writeAll(data);
            writer.close();
            int device = wearingPart.equals("ch1")? 0 : 1;
            listener.onSuccess(device);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @SuppressLint("DefaultLocale")
    private void saveLog() {
        String outputStr = "";
        String str = "";

        File logFile = new File(getContext().getExternalFilesDir(null), "/I-Motion Lab/" + "/Sante_TUG.log");


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
//        outputStr += info.location.replace(","," ") + ", ";

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

        if (UserInfo.getInstance().leadoff) outputStr += "Yes" + "\r\n";
        else outputStr += "No" + "\r\n";


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
        }

    }


    private void CreateLogFile() {
        String outputStr = "";
        CreateFolder();


        File logFile = new File(getContext().getExternalFilesDir(null) + "/I-Motion Lab/" + "/Sante_TUG.log");

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
        int cut_N = Math.round(ct/dt);
        for(int i = 0;i<MN;i++){
            if( i < cut_N/2){
//                rms_mvc[i] =

            }

        }

        return rms_mvc;
    }


    public float RMS(float[] EMGData, int m,int count) {
        float result = 0;
        //Log.wtf("EMGEnable!!!", "여기옴?222222222");
        if (count > 0) {
            /*Log.wtf("EMGEnable!!!", "여기옴?333333333");
            Log.wtf("RMS1 MM", String.valueOf(m));*/
            for (int i = 0; i < m; i++) {
                if(count < m)
                    break;
                result += Math.pow(EMGData[count - (m - i)], 2);
            }
            result = (float) Math.sqrt(result/m);
            return result;

        }
        else return 0;

    }


    public double SampleRMS(float[] EMGData, int few) {
        double result = 0;
        if (few < 5){
            //int sub =
            for (int i = 0; i < 10; i++){
                result += Math.pow(EMGData[i], 2);
            }
            result = (double) Math.sqrt(result/11);
            return result;
        }else if (EMGData.length-5 < few){
            for (int i = EMGData.length-10; i < EMGData.length; i++){
                result += Math.pow(EMGData[i], 2);
            }
            result = (double) Math.sqrt(result/11);
            return result;
        } else {
            for (int i = few-5; i < few+6; i++){
                result += Math.pow(EMGData[i], 2);
            }

            result = (double) Math.sqrt(result/11);
            return result;
        }

    }
}
