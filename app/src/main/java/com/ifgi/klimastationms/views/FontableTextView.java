package com.ifgi.klimastationms.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.datamodel.FontUtil;

public class FontableTextView extends TextView
{
    public FontableTextView(Context context)
    {
        super(context);
    }

    public FontableTextView(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
    
        FontUtil.setCustomFont(this, context, attrs, R.styleable.com_ifgi_klimastationms_FontableTextView, R.styleable.com_ifgi_klimastationms_FontableTextView_font);
    }

    public FontableTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    
        FontUtil.setCustomFont(this, context, attrs, R.styleable.com_ifgi_klimastationms_FontableTextView, R.styleable.com_ifgi_klimastationms_FontableTextView_font);
    }
}
