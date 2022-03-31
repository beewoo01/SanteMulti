package com.physiolab.sante.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.physiolab.sante.UserInfo;
import com.physiolab.sante.santemulti.DialogOnClick;
import com.physiolab.sante.santemulti.Measure1chActivity;
import com.physiolab.sante.santemulti.R;
import com.physiolab.sante.santemulti.databinding.DialogDefaultBinding;

public class DefaultDialog extends BaseDialog{

    private final String title;
    private final String body;
    private DialogDefaultBinding binding;

    private final DialogOnClick listener;



    public DefaultDialog(Context context, DialogOnClick listener, String title, String body) {
        super(context, R.style.FullScreenDialogStyle);
        //mCloseButtonListener = closeButtonListener;
        this.listener = listener;
        mContext = context;
        this.title = title;
        this.body = body;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.wtf("DefaultDialog", "onBackPressed");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DialogDefaultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        binding.editMemo.setText(UserInfo.getInstance().memo);
        binding.btnConfirm.setOnClickListener(v -> {
            UserInfo.getInstance().memo = binding.editMemo.getText().toString();
            listener.confirm(true);
            dismiss();
        });
        //binding.btnConfirm.setOnClickListener(mCloseButtonListener);
        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.confirm(false);
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
