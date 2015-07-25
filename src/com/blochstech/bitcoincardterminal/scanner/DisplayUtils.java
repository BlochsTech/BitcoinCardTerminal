package com.blochstech.bitcoincardterminal.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

@SuppressLint("NewApi")
public class DisplayUtils {
    @SuppressWarnings("deprecation") //Used correctly
	public static Point getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenResolution = new Point();
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(screenResolution);
        } else {
            screenResolution.set(display.getWidth(), display.getHeight());
        }

        return screenResolution;
    }

    @SuppressWarnings("deprecation")
	public static int getScreenOrientation(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int orientation = Configuration.ORIENTATION_UNDEFINED;
        int width, height;
        if (android.os.Build.VERSION.SDK_INT >= 13) {
	        Point point = new Point();
	        display.getSize(point);
	        width = point != null ? point.x : 0;
	        height = point != null ? point.y : 0;
        }else{
        	width = display.getWidth();
        	height = display.getHeight();
        }
        if(width==height && android.os.Build.VERSION.SDK_INT < 16){
            orientation = Configuration.ORIENTATION_SQUARE;
        } else{
            if(width <= height){
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

}
