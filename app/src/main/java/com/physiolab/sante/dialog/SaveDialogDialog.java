package com.physiolab.sante.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.physiolab.sante.listener.SaveFileDialogListener;
import com.physiolab.sante.santemulti.R;

public class SaveDialogDialog extends Dialog implements SaveFileDialogListener {

    private TextView percent_txv;
    private int percent;

    public SaveDialogDialog(@NonNull Context context, int percent) {
        super(context, R.style.FullScreenDialogStyle);
        this.percent = percent;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_dialog);
        percent_txv = findViewById(R.id.percent_txv);
        percent_txv.setText(percent + " %");
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getAttributes().windowAnimations = R.style.AnimationPopupStyle;
    }

    @Override
    public void onBackPressed() {
        Log.wtf("SaveDialogDialog", "onBackPressed");
        if (percent >= 100) super.onBackPressed();
    }

    /*@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Point pt = new Point();
        getWindow().getWindowManager().getDefaultDisplay().getSize(pt);

        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(pt);
    }*/

    @SuppressLint("SetTextI18n")
    @Override
    public void onPercent(int percent) {
        this.percent = percent;
        percent_txv.setText(this.percent + " %");
        if (percent >= 100) {
            dismiss();
        }
    }
}
