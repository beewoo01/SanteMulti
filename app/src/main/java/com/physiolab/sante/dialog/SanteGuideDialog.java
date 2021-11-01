package com.physiolab.sante.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.physiolab.sante.santemulti.R;
import com.physiolab.sante.santemulti.databinding.DialogDefaultBinding;
import com.physiolab.sante.santemulti.databinding.DialogSanteinfoBinding;


public class SanteGuideDialog extends BaseDialog {

    private Context mContext;
    private DialogSanteinfoBinding binding;


    public SanteGuideDialog(Context context) {
        super(context, R.style.FullScreenDialogStyle);
        mContext = context;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogSanteinfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getAttributes().windowAnimations = R.style.AnimationPopupStyle;


        Glide.with(mContext).load(R.drawable.sante_info)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.img01);

        binding.btnClose.setOnClickListener(v -> dismiss());

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
