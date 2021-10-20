package com.physiolab.sante;

import com.physiolab.sante.BlueToothService.BTService;

public class ST_DATA_PROC {
    public double[] Org = new double[BTService.PACKET_SAMPLE_NUM];

    // 필터링된 데이터
    public double[] Filted = new double[BTService.PACKET_SAMPLE_NUM];

    // RMS 값
    // (참고) RMS 값 관련 계산은 PACKET_SAMPLE_NUM이 지난 후 계산 바람
    public double[] RMS = new double[BTService.PACKET_SAMPLE_NUM];

    // BPF된 데이터(전극 접촉 임피던스)
    public double[] BPF = new double[BTService.PACKET_SAMPLE_NUM];

    // BPF된 데이터의 abs의 LPF(전극 접촉 임피던스의 값)
    public double[] BPF_DC = new double[BTService.PACKET_SAMPLE_NUM];

    // 가속도 X,Y, Z Filtered
    public float[][] Acc = new float[3][BTService.PACKET_SAMPLE_NUM / 10];

    // Gyro X, Y, Z Filtered
    public float[][] Gyro = new float[3][BTService.PACKET_SAMPLE_NUM / 10];

    public ST_DATA_PROC()
    {
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM;i++)
        {
            Org[i]=0.0;
            Filted[i]=0.0;
            RMS[i]=0.0;
            BPF[i]=0.0;
            BPF_DC[i]=0.0;
        }
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM/10;i++)
        {
            for(int j=0;j<3;j++)
            {
                Acc[j][i]=0.0f;
                Gyro[j][i]=0.0f;
            }
        }
    }

    public ST_DATA_PROC(ST_DATA_PROC d)
    {
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM;i++)
        {
            Org[i]=0.0;
            Filted[i]=0.0;
            RMS[i]=0.0;
            BPF[i]=0.0;
            BPF_DC[i]=0.0;
        }
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM/10;i++)
        {
            for(int j=0;j<3;j++)
            {
                Acc[j][i]=0.0f;
                Gyro[j][i]=0.0f;
            }
        }
    }

    public void SetData(ST_DATA_PROC d)
    {
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM;i++)
        {
            Org[i]=d.Org[i];
            Filted[i]=d.Filted[i];
            RMS[i]=d.RMS[i];
            BPF[i]=d.BPF[i];
            BPF_DC[i]=d.BPF_DC[i];
        }
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM/10;i++)
        {
            for(int j=0;j<3;j++)
            {
                Acc[j][i]=d.Acc[j][i];
                Gyro[j][i]=d.Gyro[j][i];
            }
        }
    }

    public void SetData(float[] arr)
    {
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM;i++)
        {
            Org[i]=arr[BTService.PACKET_SAMPLE_NUM*0+i];
            Filted[i]=arr[BTService.PACKET_SAMPLE_NUM*1+i];
            RMS[i]=arr[BTService.PACKET_SAMPLE_NUM*2+i];
            BPF[i]=arr[BTService.PACKET_SAMPLE_NUM*3+i];
            BPF_DC[i]=arr[BTService.PACKET_SAMPLE_NUM*4+i];
        }
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM/10;i++)
        {
            for(int j=0;j<3;j++)
            {
                Acc[j][i]=arr[BTService.PACKET_SAMPLE_NUM*5+(BTService.PACKET_SAMPLE_NUM/10)*j+i];
            }
        }
        for (int i=0;i<BTService.PACKET_SAMPLE_NUM/10;i++)
        {
            for(int j=0;j<3;j++)
            {
                Gyro[j][i]=arr[BTService.PACKET_SAMPLE_NUM*5+(BTService.PACKET_SAMPLE_NUM/10)*(j+3)+i];
            }
        }
    }
}
