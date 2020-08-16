package com.beztooth.UI.Util;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;

public class ViewInputHandler
{
    public interface OnClick
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
                    BezAnimation.StartButtonDownAnimation(m_Context, m_View);
                }

                m_IsDown = true;
                m_WasPressed = false;
                return true;

            case MotionEvent.ACTION_UP:
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
                            m_OnClick.Do(m_View);
                        }
                    }
                });

                m_IsDown = false;
                m_WasPressed = true;

                return true;
            case MotionEvent.ACTION_CANCEL:
                BezAnimation.StartButtonUpAnimation(m_Context, m_View);

                m_IsDown = false;
                return true;
        }

        return false;
    }

    public void performClick()
    {
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
