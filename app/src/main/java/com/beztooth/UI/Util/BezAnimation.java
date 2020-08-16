package com.beztooth.UI.Util;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import com.beztooth.R;

public class BezAnimation
{
    public static void StartButtonDownAnimation(Context context, View view)
    {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.button_down);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }

    public static void StartButtonUpAnimation(Context context, View view)
    {
        StartButtonUpAnimation(context, view, null);
    }

    public static void StartButtonUpAnimation(Context context, View view, Animation.AnimationListener animationListener)
    {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.button_up);
        animation.setInterpolator(new OvershootInterpolator(2));
        animation.setFillAfter(true);
        if (animationListener != null)
        {
            animation.setAnimationListener(animationListener);
        }

        view.startAnimation(animation);
    }
}
