package com.physiolab.sante;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

public class ScreenSize
{
    public int standardSize_X, standardSize_Y;
    private float density;

    private Point getScreenSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return  size;
    }

    public void getStandardSize(Activity activity) {
        Point ScreenSize = getScreenSize(activity);
        density  = activity.getResources().getDisplayMetrics().density;

        standardSize_X = (int) (ScreenSize.x / density);
        standardSize_Y = (int) (ScreenSize.y / density);
    }
}


