package com.beztooth.UI;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;

import com.beztooth.Util.Logger;

public class ViewInputHandler
{
    private final String TAG = "BezContainer";

    interface OnClick
    {
        void Do(View view);
    }

    private Context m_Context;
    private View m_View;
    private OnClick m_OnClick;
    private boolean m_IsDown;
    private boolean m_WasPressed;

    ViewInputHandler(Context context, View view)
    {
        m_Context = context;
        m_View = view;
        m_IsDown = false;
        m_WasPressed = false;

        m_View.setClickable(true);
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (!m_IsDown)
                {
                    Logger.Debug(TAG, "ACTION_DOWN");
                    BezAnimation.StartButtonDownAnimation(m_Context, m_View);
                }

                m_IsDown = true;
                m_WasPressed = false;
                return true;

            case MotionEvent.ACTION_UP:
                Logger.Debug(TAG, "ACTION_UP");

                this.performClick();
                BezAnimation.StartButtonUpAnimation(m_Context, m_View, new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        if (m_OnClick != null)
                        {
                            Logger.Debug(TAG, "onAnimationEnd");
                            m_OnClick.Do(m_View);
                        }
                    }
                });

                m_IsDown = false;
                m_WasPressed = true;

                return true;
            case MotionEvent.ACTION_CANCEL:
                Logger.Debug(TAG, "ACTION_CANCEL");
                BezAnimation.StartButtonUpAnimation(m_Context, m_View);

                m_IsDown = false;
                return true;
        }

        return false;
    }

    public void performClick()
    {
        Logger.Debug(TAG, "performClick");
        if (m_OnClick != null && !m_IsDown && !m_WasPressed)
        {
            m_OnClick.Do(m_View);
        }
    }

    public void SetOnClick(OnClick onClick)
    {
        m_OnClick = onClick;
    }
}
