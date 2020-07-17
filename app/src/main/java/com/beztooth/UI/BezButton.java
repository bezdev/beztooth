package com.beztooth.UI;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class BezButton extends AppCompatButton
{
    private final String TAG = "BezButton";

    private Context m_Context;
    private ViewInputHandler m_InputHandler;

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
        m_InputHandler = new ViewInputHandler(context, this);
    }

    public void SetOnClick(ViewInputHandler.OnClick onClick)
    {
        m_InputHandler.SetOnClick(onClick);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        return m_InputHandler.onTouchEvent(event);
    }

    @Override
    public boolean performClick()
    {
        super.performClick();

        m_InputHandler.performClick();

        return true;
    }
}
