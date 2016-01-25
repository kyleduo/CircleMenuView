package com.kyleduo.circlemenuviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Convenient class for custom views.
 * Created by kyle on 15/11/21.
 */
public class RayView extends View {
	protected int mWidth, mHeight;

	public RayView(Context context) {
		super(context);
		init(null);
	}

	public RayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public RayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	protected void init(AttributeSet attrs) {

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (customOnMeasure()) {
			setMeasuredDimension(measure(widthMeasureSpec, true), measure(heightMeasureSpec, false));
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	protected int measure(int measureSpec, boolean WOH) {
		int size = MeasureSpec.getSize(measureSpec);
		int mode = MeasureSpec.getMode(measureSpec);
		int measured;

		int measureMinimum = WOH ? getMinimumMeasureWidth() : getMinimumMeasureHeight();

		measureMinimum = Math.max(measureMinimum, measureMinimum + getPaddingLeft() + getPaddingRight());
		measureMinimum = Math.max(measureMinimum, getSuggestedMinimumWidth());

		if (mode == MeasureSpec.EXACTLY) {
			measured = Math.max(measureMinimum, size);
		} else {
			measured = measureMinimum;
			if (mode == MeasureSpec.AT_MOST) {
				measured = Math.min(measured, size);
			}
		}

		return measured;
	}

	protected int getMinimumMeasureWidth() {
		return getSuggestedMinimumWidth();
	}

	protected int getMinimumMeasureHeight() {
		return getSuggestedMinimumHeight();
	}

	protected boolean customOnMeasure() {
		return true;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (w != oldw || h != oldh) {
			mWidth = w;
			mHeight = h;
			onSizeRealChanged(w, h);
		}
	}

	protected void onSizeRealChanged(int w, int h) {

	}
}
