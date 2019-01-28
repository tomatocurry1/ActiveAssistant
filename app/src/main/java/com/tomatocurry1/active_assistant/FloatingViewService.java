package com.tomatocurry1.active_assistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class FloatingViewService extends Service {
    private final Handler handler = new Handler();
    private final Runnable gravityRunner = new GravityController();
    private WindowManager mWindowManager;
    public WindowManager.LayoutParams params;
    private View mFloatingView;
    private WindowManager.LayoutParams textBubbleParams;
    private View mTextBubbleView;

    public boolean isHeld = true;
    public double gravityTimeElapsed;
    public long gravityLastTime;
    public DisplayMetrics displayMetrics;
    public int squareHeightPixel;
    private Intent speechIntent;
    private SpeechRecognizer recognizer;
    private RecognitionListener listener;
    private CallManager callManager = new CallManager();
    private TextView textBubble;
    private View textBubbleWrapper;
    private CancellationSignal callCancellationSignal;
    private ProgressBar progressBar;


    class GravityController  implements Runnable{

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

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.assistant_layout, null);

        mTextBubbleView = LayoutInflater.from(this).inflate(R.layout.textbubble_layout, null);


        //TYPE_APPLICATION_OVERLAY is for api ver 26 (oreo) and up
        if(VERSION.SDK_INT >= VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            textBubbleParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
        else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            textBubbleParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        params.gravity = Gravity.TOP | Gravity.LEFT;
        textBubbleParams.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);
        mWindowManager.addView(mTextBubbleView, textBubbleParams);

        progressBar = mTextBubbleView.findViewById(R.id.text_progress_cancel);


        //this.handler.post(this.gravityRunner);

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

                        int Xdiff = Math.abs((int) (event.getRawX() - initialTouchX));
                        int Ydiff = Math.abs((int) (event.getRawY() - initialTouchY));

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So this is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            v.performClick();
                            Toast.makeText(FloatingViewService.this,"Listening for Commands " + Xdiff + "," + Ydiff, Toast.LENGTH_SHORT).show();
                            startListening();
                        }

                        return true;
                    case MotionEvent.ACTION_MOVE:


                        mTextBubbleView.setVisibility(View.INVISIBLE);
                        if(callCancellationSignal!=null)
                            callCancellationSignal.cancel();

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

        textBubble = mTextBubbleView.findViewById(R.id.assistant_text);
        textBubbleWrapper = mTextBubbleView.findViewById(R.id.textbubble_wrapper);

        initSpeechToText();

        //handler.post(gravityRunner);

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

    private void initSpeechToText(){
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.tomatocurry1.active_assistant");



        recognizer = SpeechRecognizer
                .createSpeechRecognizer(this.getApplicationContext());
        listener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                int i=0;
                ArrayList<String> voiceResults = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] confidenceResults = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                if (voiceResults == null) {
                    Log.e(TAG, "No voice results");
                } else {
                    Log.d(TAG, "Printing matches: ");
                    for (String match : voiceResults) {
                        Log.d(TAG, match + ", " + confidenceResults[i++]);
                    }
                }
                if(voiceResults.get(0).contains("call")) {
                    callCancellationSignal = new CancellationSignal();
                    if (callManager.readyCall(FloatingViewService.this, voiceResults.get(0).substring(5), callCancellationSignal)){
                        displaySpeechBubble("Starting a call to: " + voiceResults.get(0).substring(5));
                        assignSpeechBubbleCancel(callCancellationSignal);
                    }else{
                        displaySpeechBubble("No contact was found");
                    }
                }
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
                if(callCancellationSignal != null)
                    callCancellationSignal.cancel();
                Log.d(TAG, "Ready for speech");
                View box = mFloatingView.findViewById(R.id.root_container);

                float x = box.getTranslationX();
                float y = box.getTranslationY();

                ((TextView)mTextBubbleView.findViewById(R.id.assistant_text)).setText("What's up?");
                Log.println(Log.INFO, "nvm", x + ", " + y + " | " + box.getTranslationX() + ", " + box.getTranslationY());

            }

            @Override
            public void onError(int error) {
                Log.d(TAG,
                        "Error listening for speech: " + error);

            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech starting");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEndOfSpeech() {
                // TODO Auto-generated method stub
                //((TextView)mFloatingView.findViewById(R.id.assistant_text)).setText("");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // TODO Auto-generated method stub

            }
        };

        recognizer.setRecognitionListener(listener);

    }

    private void startListening(){
        recognizer.startListening(speechIntent);
    }

    private void displaySpeechBubble(String text){
        mTextBubbleView.setVisibility(View.VISIBLE);
        textBubble.setText(text + " - longer progressbar");
        textBubbleParams.y = params.y - mTextBubbleView.getHeight();
        mWindowManager.updateViewLayout(mTextBubbleView, textBubbleParams);
    }

    private void assignSpeechBubbleCancel(final CancellationSignal cancel){
        CountDownTimer countDownTimer = new CountDownTimer(5000, 500) {
            @Override
            public void onTick(long l) {
                if(callCancellationSignal.isCanceled()){
                    progressBar.setProgress(0);
                    cancel();

                }
                progressBar.setProgress((int)(100*l/5000));
            }

            @Override
            public void onFinish() {
                progressBar.setProgress(0);
            }
        };


        textBubbleWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel.cancel();
                displaySpeechBubble("call cancelled");
                Log.println(Log.INFO,"bubble click", "cancel");
            }
        });

        countDownTimer.start();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
        if (mTextBubbleView != null) mWindowManager.removeView(mTextBubbleView);
    }
}
