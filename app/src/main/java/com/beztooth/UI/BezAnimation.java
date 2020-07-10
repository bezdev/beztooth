package com.beztooth.UI;

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
        android.view.animation.Animation animation = AnimationUtils.loadAnimation(context, R.anim.button_down);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }

    public static void StartButtonUpAnimation(Context context, View view)
    {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.button_up);
        animation.setInterpolator(new OvershootInterpolator(3));
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }
}
