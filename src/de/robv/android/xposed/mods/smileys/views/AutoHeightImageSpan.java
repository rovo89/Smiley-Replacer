package de.robv.android.xposed.mods.smileys.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

public class AutoHeightImageSpan extends ReplacementSpan {
    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the bottom of the surrounding text, i.e., at the same level as the
     * lowest descender in the text.
     */
    public static final int ALIGN_BOTTOM = 0;
    
    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the baseline of the surrounding text.
     */
    public static final int ALIGN_BASELINE = 1;
    
    private final int mVerticalAlignment;
    private final Drawable mDrawable;
    private final int mDrawableHeight;
    private final int mDrawableWidth;
    private static float mZoom = 1f;
	
	public AutoHeightImageSpan(Drawable drawable) {
		this(drawable, ALIGN_BOTTOM);
    }

	public AutoHeightImageSpan(Drawable drawable, int verticalAlignment) {
		mDrawable = drawable;
		mDrawableHeight = drawable.getIntrinsicHeight();
		mDrawableWidth = drawable.getIntrinsicWidth();
	    mVerticalAlignment = verticalAlignment;
    }
	

	@Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
	    canvas.save();
        
        float transY = y - mDrawable.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BOTTOM)
        	transY += paint.getFontMetrics().descent;
        
        canvas.translate(x, transY);
        mDrawable.draw(canvas);
        canvas.restore();
    }
	
	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
    	if (fm == null)
    		fm = new FontMetricsInt();
    	
    	paint.getFontMetricsInt(fm);
    	
    	if (mVerticalAlignment == ALIGN_BASELINE)
    		fm.descent = fm.bottom = 0;
    	
    	if (mZoom != 0f) {
    		int height = fm.descent - fm.ascent;
    		float scale = (float)height / mDrawableHeight;
    		int width = (int)(mDrawableWidth * scale + 0.5f);

    		mDrawable.setBounds(0, 0, width, height);
    		return width;
    	} else {
    		fm.ascent = fm.top = fm.descent - mDrawableHeight;
    		mDrawable.setBounds(0, 0, mDrawableWidth, mDrawableHeight);
    		return mDrawableWidth;
    	}
	}
	
    public static void setDefaultZoom(float zoom) {
    	mZoom = zoom;
    }
}
