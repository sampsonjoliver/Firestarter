package com.sampsonjoliver.firestarter.views;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class FontIconView extends AppCompatTextView {
    private static Typeface iconFontTypeface;
    public static final String iconFontFile = "fonts/MaterialIcons-Regular.ttf";

    public FontIconView(Context context) {
        this(context, null);
    }

    public FontIconView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public FontIconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (!isInEditMode()) {
            if (iconFontTypeface == null)
                iconFontTypeface = Typeface.createFromAsset(context.getAssets(), iconFontFile);

            setTypeface(iconFontTypeface);
        }
    }
}