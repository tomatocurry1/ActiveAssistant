package com.tomatocurry1.active_assistant.animations;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.tomatocurry1.active_assistant.FloatingViewService;
import com.tomatocurry1.active_assistant.R;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class AssistantAnimationController {
    private final Handler handler = new Handler();
    public ImageView imageView;
    public Queue<Drawable> animationQueue;
    public FloatingViewService context;
    AnimationRunner animationRunner;
    LayoutParams params;
    Drawable[] assistantSprites = new Drawable[46];




    public AssistantAnimationController(FloatingViewService context, ImageView v, LayoutParams params){
        imageView = v;
        this.context = context;
        this.params = params;
        v.setImageDrawable(context.getResources().getDrawable(R.drawable.shime1));
        animationQueue = new LinkedList<>();
        animationRunner = new AnimationRunner();
        for(int i = 0; i < assistantSprites.length; i++) {
            try {
                assistantSprites[i] = context.getResources().getDrawable(R.drawable.class.getField("shime" + (i + 1)).getInt(null));
            }
            catch (NoSuchFieldException e){

            }catch(IllegalAccessException e){

            }
        }

    }


    class AnimationRunner  implements Runnable{



        AnimationRunner(){
        }

        @Override
        public void run() {
            if(animationQueue.size() > 0){
                imageView.setImageDrawable(animationQueue.poll());
                params.x -= 10;

                context.mWindowManager.updateViewLayout(context.mFloatingView, params);

            }
            else
                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.shime1));
            iterateQueue();
        }


    }

    public void walk(){
        animationQueue.add(assistantSprites[0]);
        animationQueue.add(assistantSprites[1]);
        animationQueue.add(assistantSprites[0]);
        animationQueue.add(assistantSprites[2]);
        animationQueue.add(assistantSprites[0]);
        handler.post(animationRunner);
    }

    public void iterateQueue(){
        handler.removeCallbacks(animationRunner);
        handler.postDelayed(animationRunner, 40*6);
    }
}
