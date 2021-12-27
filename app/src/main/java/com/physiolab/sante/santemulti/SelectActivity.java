package com.physiolab.sante.santemulti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;

import com.physiolab.sante.dialog.SanteGuideDialog;
import com.physiolab.sante.santemulti.databinding.ActivitySelectBinding;

public class SelectActivity extends AppCompatActivity {

    private ActivitySelectBinding binding;
    private ToneGenerator tone;
    private SoundPool sPool;
    private int beepNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();

    }

    private void initView(){

        binding.btnOneMeasure.setOnClickListener( v -> {
            startActivity(new Intent(SelectActivity.this, Main1chActivity.class));
        });

        binding.btnTwoMeasure.setOnClickListener( v -> {
            startActivity(new Intent(SelectActivity.this, Main2chActivity.class));
        });

        binding.santeIntroduce.setOnClickListener( v-> {
            SanteGuideDialog santeGuideDialog = new SanteGuideDialog(this);
            santeGuideDialog.show();
        });

    }

    /*private void BeepInit2() {
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);

        //sPool = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);


        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(8).build();
        beepNum = sPool.load(getApplicationContext(), R.raw.beep, 1);


    }

    private void BeepInit() {
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MIN_VOLUME);

        //sPool = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);


        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(8).build();
        beepNum = sPool.load(getApplicationContext(), R.raw.beep, 1);


    }

    private void BeepPlay() {
        tone.startTone(ToneGenerator.TONE_DTMF_S, 100);

        //sPool.play(beepNum, 1f, 1f, 0, 0, 1f);
        sPool.play(beepNum, 1f, 1f, 0, 0, 1f);
    }*/

}