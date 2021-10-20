package com.physiolab.sante.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.physiolab.sante.santemulti.R;
import com.physiolab.sante.santemulti.databinding.DialogDefaultBinding;

public class DefaultDialog extends BaseDialog{

    private String title;
    private String body;
    private boolean isFullScreen = false;
    private View.OnClickListener mCloseButtonListener;
    private Context mContext;
    private DialogDefaultBinding binding;


    public DefaultDialog(Context context, View.OnClickListener closeButtonListener, String title, String body) {
        super(context, R.style.FullScreenDialogStyle);
        mCloseButtonListener = closeButtonListener;
        mContext = context;
        this.title = title;
        this.body = body;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DialogDefaultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        binding.btnConfirm.setOnClickListener(mCloseButtonListener);
        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        binding.title.setText(title);
        binding.body.setText(body);

        if (title.equals("신호감지")) {
            binding.btnConfirm.setVisibility(View.GONE);
        } else if (body.equals("측정결과를 저장하시겠습니까?")) {

            binding.btnCancel.setVisibility(View.VISIBLE);

        }
        if (body.equals("신호감지")) {
            binding.btnConfirm.setVisibility(View.GONE);
        } else if (body.equals("정보등록 없이 테스트를 진행하겠습니까?")) {

            binding.btnCancel.setVisibility(View.VISIBLE);

        }

    }   //  onCreate

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Point pt = new Point();
        getWindow().getWindowManager().getDefaultDisplay().getSize(pt);


        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(pt);
    }
}
