package com.tomatocurry1.active_assistant;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

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




    public AssistantAnimationController(FloatingViewService context, ImageView v, LayoutParams params){
        imageView = v;
        this.context = context;
        this.params = params;
        v.setImageDrawable(context.getResources().getDrawable(R.drawable.shime1));
        animationQueue = new LinkedList<>();
        animationRunner = new AnimationRunner();
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
        animationQueue.add(context.getResources().getDrawable(R.drawable.shime1));
        animationQueue.add(context.getResources().getDrawable(R.drawable.shime2));
        animationQueue.add(context.getResources().getDrawable(R.drawable.shime1));
        animationQueue.add(context.getResources().getDrawable(R.drawable.shime3));
        animationQueue.add(context.getResources().getDrawable(R.drawable.shime1));
        handler.post(animationRunner);
    }

    public void iterateQueue(){
        handler.removeCallbacks(animationRunner);
        handler.postDelayed(animationRunner, 40*6);
    }
}
