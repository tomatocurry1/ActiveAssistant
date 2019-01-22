package com.tomatocurry1.active_assistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateInterpolator;

public class FloatingViewService extends Service {
    private final Handler handler = new Handler();
    private final Runnable gravityRunner = new GravityController();
    private WindowManager mWindowManager;
    public WindowManager.LayoutParams params;
    private View mFloatingView;
    public boolean isHeld = true;
    public double gravityTimeElapsed;
    public long gravityLastTime;
    public DisplayMetrics displayMetrics;
    public int squareHeightPixel;


    class GravityController  implements Runnable{



        GravityController(){
        }

        @Override
        public void run() {
            FloatingViewService.this.drawGravity();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        displayMetrics  = getResources().getDisplayMetrics();
        squareHeightPixel =  (int) (100 * getResources().getDisplayMetrics().density + 0.5f);

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null);



        //TYPE_APPLICATION_OVERLAY is for api ver 26 (oreo) and up
        if(VERSION.SDK_INT >= VERSION_CODES.O)
            params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        else
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        this.handler.post(this.gravityRunner);

        //dragging the object around
        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener(){
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private int height = getResources().getDisplayMetrics().heightPixels;




            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isHeld = true;
                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        isHeld = false;
                        gravityTimeElapsed = 0;
                        gravityLastTime = -1;
                        Log.println(Log.INFO, "movement coords", "init: " + initialX + ", " + initialTouchY + "; init_touch: " + initialTouchX + ", " + initialTouchY);

                        if (params.x < 0){
                            params.x = 0;
                        }else if(params.x > displayMetrics.widthPixels - squareHeightPixel){
                            params.x = displayMetrics.widthPixels - squareHeightPixel;
                        }
                        if (params.y < 0){
                            params.y = 0;
                        }else if(params.y > displayMetrics.heightPixels - squareHeightPixel){
                            params.y = displayMetrics.heightPixels - squareHeightPixel;
                        }

//
//                        int Xdiff = (int) (event.getRawX() - initialTouchX);
//                        int Ydiff = (int) (event.getRawY() - initialTouchY);
//
//                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
//                        //So that is click event.
//                        if (Xdiff < 10 && Ydiff < 10) {
//                            if (isViewCollapsed()) {
//                                //When user clicks on the image view of the collapsed layout,
//                                //visibility of the collapsed layout will be changed to "View.GONE"
//                                //and expanded view will become visible.
//                                collapsedView.setVisibility(View.GONE);
//                                expandedView.setVisibility(View.VISIBLE);
//                            }
//                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:

                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Log.println(Log.INFO,"drag logs",  params.y + "y pos" + displayMetrics.ydpi + "?" + Display.class.getMethod("getRawHeight").invoke(mWindowManager.getDefaultDisplay()));
                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }

        });

        handler.post(gravityRunner);

    }

    public void drawGravity(){
        if(!isHeld && params.y < displayMetrics.heightPixels - squareHeightPixel){


            if(gravityLastTime != -1){
                gravityTimeElapsed = (System.currentTimeMillis() - gravityLastTime)/1000.0;
                params.y += (int)((386.0885827*displayMetrics.ydpi)*gravityTimeElapsed/1000.0);


            }
            else
                gravityLastTime = System.currentTimeMillis();


            if (params.y > displayMetrics.heightPixels - squareHeightPixel)
                params.y = displayMetrics.heightPixels - squareHeightPixel;
            mWindowManager.updateViewLayout(mFloatingView, params);
            Log.println(Log.INFO,"gravity logs",  (int)((386.0885827*displayMetrics.ydpi)*gravityTimeElapsed/1000.0) + "speed " + gravityTimeElapsed);

        }
        handler.removeCallbacks(gravityRunner);
        handler.postDelayed(gravityRunner, 16);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}
