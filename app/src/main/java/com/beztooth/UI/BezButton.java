package com.beztooth.UI;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BezButton extends AppCompatButton
{
    private final String TAG = "BezButton";

    private Context m_Context;
    private OnClick m_OnClick;
    private boolean m_IsDown;

    public interface OnClick
    {
        void Do(View view);
    }

    public BezButton(Context context)
    {
        super(context);
        Init(context);
    }

    public BezButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Init(context);
    }

    public BezButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        Init(context);
    }

    private void Init(Context context)
    {
        m_Context = context;
        m_IsDown = false;
        m_OnClick = null;
    }

    public void SetOnClick(OnClick onClick)
    {
        m_OnClick = onClick;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (!m_IsDown)
                {
                    BezAnimation.StartButtonDownAnimation(m_Context, this);
                }

                m_IsDown = true;
                return true;

            case MotionEvent.ACTION_UP:
                performClick();
            case MotionEvent.ACTION_CANCEL:
                BezAnimation.StartButtonUpAnimation(m_Context, this);

                m_IsDown = false;
                return true;
        }

        return false;
    }

    @Override
    public boolean performClick()
    {
        super.performClick();

        if (m_OnClick != null)
        {
            m_OnClick.Do(this);
        }

        return true;
    }
}
