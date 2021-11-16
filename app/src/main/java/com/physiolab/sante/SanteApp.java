package com.physiolab.sante;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SanteApp extends Application {

    private DefaultValue defaultValue = new DefaultValue();

    SharedPreferences sharedPreferences;

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_CORES);
    public Handler maintThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    class DefaultValue
    {
        public final int AccHPF = 0;
        public final int AccLPF = 0;

        public final int GyroHPF = 0;
        public final int GyroLPF = 0;

        public final int EMGNotch = 0;


        public final int EMGHPF = 1;
        public final int EMGLPF = 0;

        public final int EMGRMS = 2;

        public final float EMGMax = 1000.0f;
        public final float EMGMin = -1000.0f;

        public final float AccMax = 6.0f;
        public final float AccMin = -6.0f;

        public final float GyroMax = 800.0f;
        public final float GyroMin = -800.0f;

        public final float EMGFullMax = 200000.0f;
        public final float EMGFullMin = -200000.0f;

        public final float AccFullMax = 16.0f;
        public final float AccFullMin = -16.0f;

        public final float GyroFullMax = 2000.0f;
        public final float GyroFullMin = -2000.0f;

        public final float EMGMinimumRange = 20.0f;

        public final float AccMinimumRange = 0.2f;

        public final float GyroMinimumRange = 2.0f;

        public final float TimeRange = 30.0f;

        public final float TimeMinimumRange = 2.0f;

        public final int LeadOff = 2;
    }



    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("Setup", MODE_PRIVATE);

        overrideFont(getApplicationContext(),"SERIF","Roboto-Light.ttf");
    }

    public void overrideFont(Context context, String defaultFontNameToOverride, String customFontFileNameInAssets)
    {
        try
        {
            final Typeface customFontTypeFace = Typeface.createFromAsset(context.getAssets(),customFontFileNameInAssets);
            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);

            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null,customFontTypeFace);
        }
        catch(Exception e)
        {

        }
    }

    public int GetAccHPF(int device) { return sharedPreferences.getInt("AccHPF"+device,defaultValue.AccHPF);}
    public int GetAccLPF(int device) { return sharedPreferences.getInt("AccLPF"+device,defaultValue.AccLPF);}

    public int GetGyroHPF(int device) { return sharedPreferences.getInt("GyroHPF"+device,defaultValue.GyroHPF);}
    public int GetGyroLPF(int device) { return sharedPreferences.getInt("GyroLPF"+device,defaultValue.GyroLPF);}

    public int GetEMGNotch(int device) { return sharedPreferences.getInt("EMGNotch"+device,defaultValue.EMGNotch);}

    public int GetEMGHPF(int device) { return sharedPreferences.getInt("EMGHPF"+device,defaultValue.EMGHPF);}
    public int GetEMGLPF(int device) { return sharedPreferences.getInt("EMGLPF"+device,defaultValue.EMGLPF);}

    public int GetEMGRMS(int device) { return sharedPreferences.getInt("EMGRMS"+device, defaultValue.EMGRMS);}
    //editor.putInt("EMGRMS"+device,rms);

    public float GetAccMax(int device) { return sharedPreferences.getFloat("AccMax"+device,defaultValue.AccMax); }
    public float GetAccMin(int device) { return sharedPreferences.getFloat("AccMin"+device,defaultValue.AccMin); }

    public float GetGyroMax(int device) { return sharedPreferences.getFloat("GyroMax"+device,defaultValue.GyroMax); }
    public float GetGyroMin(int device) { return sharedPreferences.getFloat("GyroMin"+device,defaultValue.GyroMin); }

    public float GetEMGMax(int device) { return sharedPreferences.getFloat("EMGMax"+device,defaultValue.EMGMax); }
    public float GetEMGMin(int device) { return sharedPreferences.getFloat("EMGMin"+device,defaultValue.EMGMin); }

    public float GetTimeRange(int device) { return sharedPreferences.getFloat("TimeRange"+device,defaultValue.TimeRange); }

    public int GetLeadOff(int device) { return sharedPreferences.getInt("LeadOff"+device,defaultValue.LeadOff); }

    public boolean GetAlarm(int device) { return sharedPreferences.getBoolean("Alarm"+device,true); }
    public boolean GetPreview(int device) { return sharedPreferences.getBoolean("Preview"+device,true); }

    public float GetDefaultAccMax() { return defaultValue.AccMax; }
    public float GetDefaultAccMin() { return defaultValue.AccMin; }
    public float GetDefaultGyroMax() { return defaultValue.GyroMax; }
    public float GetDefaultGyroMin() { return defaultValue.GyroMin; }
    public float GetDefaultEMGMax() { return defaultValue.EMGMax; }
    public float GetDefaultEMGMin() { return defaultValue.EMGMin; }

    public float GetFullAccMax() { return defaultValue.AccFullMax; }
    public float GetFullAccMin() { return defaultValue.AccFullMin; }
    public float GetFullGyroMax() { return defaultValue.GyroFullMax; }
    public float GetFullGyroMin() { return defaultValue.GyroFullMin; }
    public float GetFullEMGMax() { return defaultValue.EMGFullMax; }
    public float GetFullEMGMin() { return defaultValue.EMGFullMin; }

    public float GetAccMinimunRange() { return defaultValue.AccMinimumRange; }
    public float GetGyroMinimunRange() { return defaultValue.GyroMinimumRange; }
    public float GetEMGMinimunRange() { return defaultValue.EMGMinimumRange; }
    public float GetTimeMinimunRange() { return defaultValue.TimeMinimumRange; }

    public void SetAccHPF(int hpf, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("AccHPF"+device ,hpf);
        editor.commit();
    }
    public void SetAccLPF(int lpf, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("AccLPF"+device,lpf);
        editor.commit();
    }

    public void SetGyroHPF(int hpf, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("GyroHPF"+device,hpf);
        editor.commit();
    }
    public void SetGyroLPF(int lpf, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("GyroLPF"+device,lpf);
        editor.commit();
    }

    public void SetEMGNotch(int notch, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("EMGNotch"+device, notch);
        editor.commit();
    }
    public void SetEMGHPF(int hpf, int device) {

        //Log.wtf("여기는 SetEmgHPF", hpf +"");
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("EMGHPF"+device, hpf);
        editor.commit();
    }
    public void SetEMGLPF(int lpf, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("EMGLPF"+device,lpf);
        editor.commit();
    }


    public void SetEMGRMS(int rms ,int device){
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("EMGRMS"+device,rms);
        editor.commit();
    }



    public void SetAccMax(float max, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("AccMax"+device, max);
        editor.commit();
    }
    public void SetAccMin(float min, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("AccMin"+device, min);
        editor.commit();
    }

    public void SetGyroMax(float max, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("GyroMax"+device, max);
        editor.commit();
    }
    public void SetGyroMin(float min, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("GyroMin"+device, min);
        editor.commit();
    }

    public void SetEMGMax(float max, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("EMGMax"+device, max);
        editor.commit();
    }
    public void SetEMGMin(float min, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("EMGMin"+device, min);
        editor.commit();
    }

    public void SetTimeRange(float range, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putFloat("TimeRange"+device, range);
        editor.commit();
    }

    public void SetLeadOff(int leadoff, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putInt("LeadOff"+device, leadoff);
        editor.commit();
    }

    public void SetAlarm(boolean alarm, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putBoolean("Alarm"+device, alarm);
        editor.commit();
    }

    public void SetPreview(boolean preview, int device) {
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putBoolean("Preview"+device, preview);
        editor.commit();
    }

}
