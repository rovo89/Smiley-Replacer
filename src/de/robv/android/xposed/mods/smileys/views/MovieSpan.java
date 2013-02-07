package de.robv.android.xposed.mods.smileys.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

public class MovieSpan extends ReplacementSpan {
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
    
    private static final Paint PAINT_MODE
    	= new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    
    private final int mVerticalAlignment;
	private final Movie mMovie;
	private final int mMovieDuration;
	private final int mMovieHeight;
	private final int mMovieWidth;
	private float mScale = 1f;
	private long mMovieStart = 0;
	
    private final Bitmap mTmpBitmap;
    private final Canvas mTmpCanvas;
	
	public MovieSpan(Movie movie) {
		this(movie, ALIGN_BOTTOM);
    }
	
	/**
     * @param verticalAlignment one of {@link MovieSpan#ALIGN_BOTTOM} or
     * {@link MovieSpan#ALIGN_BASELINE}.
     */
	public MovieSpan(Movie movie, int verticalAlignment) {
		mMovie = movie;
		mMovieDuration = movie.duration();
		mMovieHeight = movie.height();
		mMovieWidth = movie.width();
		mVerticalAlignment = verticalAlignment;
		mTmpBitmap = Bitmap.createBitmap(mMovieWidth, mMovieHeight, Bitmap.Config.ARGB_8888);
		mTmpCanvas = new Canvas(mTmpBitmap);
    }
	
	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
	    canvas.save();
        
        float transY = y - (int)(mMovieHeight * mScale + 0.5f);
        if (mVerticalAlignment == ALIGN_BOTTOM)
        	transY += paint.getFontMetrics().descent;
        
        canvas.translate(x, transY);
        canvas.scale(mScale, mScale);
        
	    long now = SystemClock.uptimeMillis();
	    if (mMovieStart == 0) {
	    	mMovieStart = now;
	    }
	    mMovie.setTime((int) ((now - mMovieStart) % mMovieDuration));
	    
	    //mMovie.draw(canvas, 0, 0, PAINT_MODE);
	    mTmpBitmap.eraseColor(Color.TRANSPARENT);
	    mMovie.draw(mTmpCanvas, 0, 0);
	    canvas.drawBitmap(mTmpBitmap, 0, 0, PAINT_MODE);
        
        canvas.restore();
	}
	
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
    	if (fm == null)
    		fm = new FontMetricsInt();
    	
    	paint.getFontMetricsInt(fm);
    	
    	if (mVerticalAlignment == ALIGN_BASELINE)
    		fm.descent = fm.bottom = 0;
    	
    	mScale = (float)(fm.descent - fm.ascent) / mMovieHeight;
        return (int)(mMovieWidth * mScale + 0.5f);
    }
    
    public static boolean hasMovieSpans(CharSequence text) {
		return (text instanceof Spanned) && ((Spanned) text).getSpans(0, text.length(), MovieSpan.class).length > 0;
	}
}