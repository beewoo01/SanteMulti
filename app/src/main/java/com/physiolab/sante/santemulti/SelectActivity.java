package com.physiolab.sante.santemulti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.physiolab.sante.dialog.SanteGuideDialog;
import com.physiolab.sante.santemulti.databinding.ActivitySelectBinding;

public class SelectActivity extends AppCompatActivity {

    private ActivitySelectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();

    }

    private void initView(){

        binding.btnOneMeasure.setOnClickListener( v -> {
            startActivity(new Intent(SelectActivity.this, Connect_1chActivity.class));
        });

        binding.btnTwoMeasure.setOnClickListener( v -> {
            startActivity(new Intent(SelectActivity.this, MainTestActivity.class));
        });

        binding.santeIntroduce.setOnClickListener( v-> {
            SanteGuideDialog santeGuideDialog = new SanteGuideDialog(this);
            santeGuideDialog.show();
        });

    }

}