package de.robv.android.xposed.mods.smileys.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class MovieDrawable extends Drawable implements Runnable {
	private static final int REFRESH_RATE = 100;
	
	private static final Paint PAINT_MODE
		= new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
	
	private final Movie mMovie;
	private final int mMovieDuration;
	private final int mMovieHeight;
	private final int mMovieWidth;
	private long mMovieStart = 0;
	
    private final Bitmap mTmpBitmap;
    private final Canvas mTmpCanvas;
	
	public MovieDrawable(Movie movie) {
		mMovie = movie;
		mMovieDuration = movie.duration();
		mMovieHeight = movie.height();
		mMovieWidth = movie.width();
		mTmpBitmap = Bitmap.createBitmap(mMovieWidth, mMovieHeight, Bitmap.Config.ARGB_8888);
		mTmpCanvas = new Canvas(mTmpBitmap);
	}
	
	@Override
    public void draw(Canvas canvas) {
	    long now = SystemClock.uptimeMillis();
	    if (mMovieStart == 0) {
	    	mMovieStart = now;
	    }
	    mMovie.setTime((int) ((now - mMovieStart) % mMovieDuration));
        //mMovie.draw(canvas, 0, 0, PAINT_MODE);
	    mTmpBitmap.eraseColor(Color.TRANSPARENT);
	    mMovie.draw(mTmpCanvas, 0, 0);
	    canvas.drawBitmap(mTmpBitmap, 0, 0, PAINT_MODE);
        
		scheduleSelf(this, now + REFRESH_RATE - (now % REFRESH_RATE));
    }
	
	@Override
	public int getIntrinsicHeight() {
		return mMovieHeight;
	}

	@Override
	public int getIntrinsicWidth() {
		return mMovieWidth;
	}
	
	@Override
	public void run() {
		invalidateSelf();
	}

	@Override
    public void setAlpha(int alpha) {}

	@Override
    public void setColorFilter(ColorFilter cf) {}

	@Override
    public int getOpacity() {
	    return PixelFormat.UNKNOWN ;
    }
}
