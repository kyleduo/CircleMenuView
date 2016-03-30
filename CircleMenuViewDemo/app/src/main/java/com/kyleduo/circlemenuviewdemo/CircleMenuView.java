package com.kyleduo.circlemenuviewdemo;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.util.ArrayList;

/**
 * Created by kyle on 16/1/20.
 */
public class CircleMenuView extends RayView {
	private final int DEFAULT_FADE_ANGLE = 15;

	private ArrayList<Bitmap> mImages;
	private ArrayList<PointF> mLocations;
	private ArrayList<Float> mAngles;
	private Paint mPaint;
	// padding
	private float mInset;
	// max size of single image
	private float mImgSize;
	private float mTrackRadius;
	// angle offset
	private float mOffsetAngle;
	private float mIntervalAngle;
	private float mMarginAngle;
	private float mStartAngle;
	private float mEndAngle;
	private float mFadeAngle = DEFAULT_FADE_ANGLE;
	private float mCenterX, mCenterY;
	private float mLastTouchDegree;
	private int mTouchTime = ViewConfiguration.getTapTimeout();
	// limit item in arc, work with
	private float mMaxAngle, mMinAngle;
	private boolean mReverse;
	private int mBackCircleColor;
	private Bitmap mMask;
	private RectF mMaskRectF = new RectF();
	private PorterDuffXfermode mFermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
	private CircleDialAdapter mCircleDialAdapter;
	private OnItemClickListener mOnItemClickListener;
	private DisplayMode mDisplayMode = DisplayMode.EDGE;

	public CircleMenuView(Context context) {
		super(context);
		init();
	}

	public CircleMenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CircleMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		mLocations = new ArrayList<>();
		mAngles = new ArrayList<>();

		mBackCircleColor = 0xCCFFFFFF;
	}

	@Override
	protected int getMinimumMeasureWidth() {
		return (int) ((mTrackRadius + mImgSize / 2 + mInset) * 2);
	}

	@Override
	protected int getMinimumMeasureHeight() {
		return (int) ((mTrackRadius + mImgSize / 2 + mInset) * 2);
	}

	private double pow2(double v) {
		return Math.pow(v, 2);
	}

	private void prepareBitmap() {
		if (mCircleDialAdapter == null) {
			if (mImages != null) {
				mImages.clear();
			}
			return;
		}
		if (mImages == null) {
			mImages = new ArrayList<>();
		}
		int count = mImages.size();
		int newCount = mCircleDialAdapter.getCount();
		for (int i = 0; i < newCount; i++) {
			Bitmap b = mCircleDialAdapter.getBitmap(i);
			float factor = Math.min(mImgSize / b.getWidth(), mImgSize / b.getHeight());
			Bitmap bb = Bitmap.createScaledBitmap(b, (int) Math.max(1, b.getWidth() * factor), (int) Math.max(1, b.getHeight() * factor), true);
			b.recycle();
			if (i < count) {
				mImages.set(i, bb);
			} else {
				mImages.add(bb);
			}
		}

		for (int i = count - 1; i >= newCount; i++) {
			mImages.remove(i);
		}
	}

	@Override
	protected void onSizeRealChanged(int w, int h) {
		super.onSizeRealChanged(w, h);
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		mMaskRectF.set(0, 0, mWidth, mHeight);
		makeMask();
	}

	/**
	 * Used for create mask, just in EDGE display mode.
	 */
	private void makeMask() {
		mMask = Bitmap.createBitmap((int) mMaskRectF.width(), (int) mMaskRectF.height(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mMask);
		float start = mStartAngle + 180 + (mReverse ? mFadeAngle + mMarginAngle : -mFadeAngle - mMarginAngle);
		float end = mEndAngle + 180 + (mReverse ? -mFadeAngle - mMarginAngle : mFadeAngle + mMarginAngle);
		float fadeRatio = mFadeAngle / Math.abs(end - start);
		float sPos = start / 360.f;
		float ePos = end / 360.f;
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setShader(new SweepGradient(mCenterX, mCenterY, new int[]{0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF}, new float[]{sPos, sPos + fadeRatio, ePos - fadeRatio, ePos}));
		float sweep = mReverse ? start - end : end - start;
		canvas.drawArc(mMaskRectF, start, sweep, true, paint);
	}

	private boolean atStart() {
		if (mLocations == null || mLocations.size() == 0) {
			return true;
		}
		PointF loc = mLocations.get(0);
		return validLocation(loc) && (mReverse ? mOffsetAngle <= mMinAngle : mOffsetAngle >= mMaxAngle);
	}

	private boolean atEnd() {
		if (mLocations == null || mLocations.size() == 0 || mImages == null || mImages.size() == 0) {
			return true;
		}
		PointF loc = mLocations.get(mImages.size() - 1);
		return validLocation(loc) && (mReverse ? mOffsetAngle >= mMaxAngle : mOffsetAngle <= mMinAngle);
	}

	private float angleOf(int index) {
		return angleOf(index, mOffsetAngle);
	}

	// used for calc limit of offset
	private float angleOf(int index, float offsetAngle) {
		return mStartAngle + (index * mIntervalAngle) * (mReverse ? -1 : 1) + offsetAngle;
	}

	private void refreshLocation() {
		int count = mImages == null ? 0 : mImages.size();
		if (count == 0) {
			return;
		}

		if (mReverse && mEndAngle >= mStartAngle) {
			mEndAngle -= 360;
		} else if (!mReverse && mEndAngle <= mStartAngle) {
			mEndAngle += 360;
		}

		for (int i = 0; i < count; i++) {
			float ang = angleOf(i);
			if (mAngles.size() < i + 1) {
				mAngles.add(ang);
			} else {
				mAngles.set(i, ang);
			}

			if (mLocations.size() < i + 1) {
				mLocations.add(new PointF(-1, -1));
			}

			calcLocation(ang, mLocations.get(i));
		}

		mMinAngle = mReverse ? 0 : mEndAngle - mStartAngle - (count - 1) * mIntervalAngle;
		mMaxAngle = mReverse ? mEndAngle - mStartAngle + (count - 1) * mIntervalAngle : 0;
	}

	private void calcLocation(float angle, PointF loc) {
		float marginAngle = mDisplayMode == DisplayMode.FADE ? 0 : mMarginAngle * 2;

		if (!mReverse && (angle < mStartAngle - mFadeAngle - marginAngle || angle > mEndAngle + mFadeAngle + marginAngle)) {
			loc.set(-1, -1);
			return;
		} else if (mReverse && (angle > mStartAngle + mFadeAngle + marginAngle || angle < mEndAngle - mFadeAngle - marginAngle)) {
			loc.set(-1, -1);
			return;
		}
		float x, y;
		float cos = (float) Math.cos(angle / 180 * Math.PI);
		float d = cos == 0 ? 0 : mTrackRadius * cos;
		x = mCenterX - d;
		float sin = (float) Math.sin(angle / 180 * Math.PI);
		d = sin == 0 ? 0 : mTrackRadius * sin;
		y = mCenterY - d;
		loc.set(x, y);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		mPaint.setColor(mBackCircleColor);
		canvas.drawCircle(mWidth / 2, mHeight / 2, mWidth / 2, mPaint);

		if (mImages == null) {
			return;
		}

		refreshLocation();

		if (mDisplayMode == DisplayMode.FADE) {
			drawBitmap(canvas, true);
		} else if (mDisplayMode == DisplayMode.EDGE) {
			// prevent back color transparent.
			mPaint.setAlpha(255);
			int count = canvas.saveLayer(mMaskRectF.left, mMaskRectF.top, mMaskRectF.right, mMaskRectF.bottom, null, Canvas.ALL_SAVE_FLAG);

			drawBitmap(canvas, false);

			mPaint.setXfermode(mFermode);
			canvas.drawBitmap(mMask, mMaskRectF.left, mMaskRectF.top, mPaint);

			mPaint.setXfermode(null);
			canvas.restoreToCount(count);
		}
	}

	private void drawBitmap(Canvas canvas, boolean fade) {
		for (int i = 0; i < mImages.size(); i++) {
			PointF p = mLocations.get(i);
			if (!validLocation(p)) {
				continue;
			}
			Bitmap b = mImages.get(i);
			if (fade) {
				int alpha = 0;
				float angle = mAngles.get(i);
				if (mReverse ? angle > mStartAngle : angle < mStartAngle) {
					float diff = mReverse ? mStartAngle + mFadeAngle - angle : angle - mStartAngle + mFadeAngle;
					alpha = (int) (Math.sin(diff / mFadeAngle * Math.PI / 2) * 255);
				} else if (mReverse ? angle < mEndAngle : angle > mEndAngle) {
					float diff = mReverse ? angle - mEndAngle + mFadeAngle : mEndAngle + mFadeAngle - angle;
					alpha = (int) (Math.sin(diff / mFadeAngle * Math.PI / 2) * 255);
				}
				if (alpha > 0) {
					mPaint.setAlpha(alpha);
				} else {
					mPaint.setAlpha(255);
				}
			}
			canvas.drawBitmap(b, p.x - b.getWidth() / 2, p.y - b.getHeight() / 2, mPaint);
		}
	}

	private boolean validLocation(PointF point) {
		return point.x > 0 && point.y > 0;
	}

	/**
	 * @return angle in degree
	 */
	private float arcCos(double cos) {
		return (float) (Math.acos(cos) / Math.PI * 180);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();

		float x = event.getX();
		float y = event.getY();

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mLastTouchDegree = angleOf(x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				float currTouchDegree = angleOf(x, y);

				float offsetAng = currTouchDegree - mLastTouchDegree;
				if (Math.abs(offsetAng) > 180) {
					offsetAng = offsetAng > 0 ? offsetAng - 360 : offsetAng + 360;
				}
				//noinspection StatementWithEmptyBody
				if ((atStart() && (mReverse ? offsetAng < 0 : offsetAng > 0) || (atEnd() && (mReverse ? offsetAng > 0 : offsetAng < 0)))) {
					// DO NOTHING: 16/1/23
				} else {
					setOffsetAngle(mOffsetAngle + offsetAng);
				}

				mLastTouchDegree = currTouchDegree;
				break;
			case MotionEvent.ACTION_UP:
				if (event.getEventTime() - event.getDownTime() < mTouchTime) {
					// click
					int index = catchImg(event);
					if (index != -1 && mOnItemClickListener != null) {
						mOnItemClickListener.onItemClick(index);
					}
				}
				break;
		}
		return true;
	}

	/**
	 * angle of point in degree
	 *
	 * @param p point
	 * @return angle in degree
	 */
	private float angleOf(PointF p) {
		return angleOf(p.x, p.y);
	}

	/**
	 * angle of point in degree
	 *
	 * @return angle in degree
	 */
	private float angleOf(double x, double y) {
		// x-pos is left
		double cos = (mCenterX - x) / dotProduct(x - mCenterX, y - mCenterY);
		// y-pos is down
		boolean sign = mCenterY - y > 0;
		float degree = sign ? arcCos(cos) : -arcCos(cos);
		degree = (degree + 360) % 360;
		return degree;
	}

	/**
	 * Calculate dot product.
	 *
	 * @param p point
	 * @return dot product
	 */
	private double dotProduct(PointF p) {
		return dotProduct(p.x, p.y);
	}

	/**
	 * Calculate dot product
	 *
	 * @param x x
	 * @param y y
	 * @return dot product
	 */
	private double dotProduct(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}

	/**
	 * judge witch one to click
	 *
	 * @param event
	 * @return
	 */
	private int catchImg(MotionEvent event) {
		int index = -1;
		for (int i = 0; i < mLocations.size(); i++) {
			PointF loc = mLocations.get(i);
			if (validLocation(loc)) {
				float l = loc.x - mImgSize / 2;
				float t = loc.y - mImgSize / 2;
				float r = l + mImgSize;
				float b = t + mImgSize;
				float x = event.getX();
				float y = event.getY();
				if (x > l && x < r && y > t && y < b) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	public void setCircleMenuAdapter(CircleDialAdapter circleDialAdapter) {
		if (circleDialAdapter == null) {
			return;
		}
		mCircleDialAdapter = circleDialAdapter;
		prepareBitmap();
	}

	public void refreshBitmap() {
		prepareBitmap();
		invalidate();
	}

	public void setAngleRange(float startAngle, float endAngle, boolean reverse) {
		mStartAngle = startAngle;
		mEndAngle = endAngle;
		mReverse = reverse;
		invalidate();
	}

	/**
	 * radius of image circle
	 *
	 * @param trackRadius
	 */
	public void setTrackRadius(float trackRadius) {
		mTrackRadius = trackRadius;
		mImgSize = mTrackRadius * 0.8f;

		double n = 2 * mTrackRadius * mTrackRadius;
		double pow = pow2(mTrackRadius) * 2;

		mIntervalAngle = arcCos((pow - pow2(mImgSize * 1.1)) / n);
		mMarginAngle = arcCos((pow - pow2(mImgSize * 1.4 / 2)) / n);

		requestLayout();
	}

	/**
	 * like padding
	 *
	 * @param inset
	 */
	public void setInset(int inset) {
		mInset = inset;
		requestLayout();
	}

	public void setFadeAngle(float fadeAngle) {
		mFadeAngle = fadeAngle;
		invalidate();
	}

	public void scrollTo(int index) {
		float target = mStartAngle - angleOf(index, 0);

		setOffsetAngle(target);
	}

	public void smoothScrollTo(int index) {
		float target = mStartAngle - angleOf(index, 0);
		ObjectAnimator animator = ObjectAnimator.ofFloat(this, "offsetAngle", mOffsetAngle, target);
		animator.setDuration(300);
		animator.start();
	}

	public float getOffsetAngle() {
		return mOffsetAngle;
	}

	public void setOffsetAngle(float offsetAngle) {
		if (offsetAngle < mMinAngle) {
			offsetAngle = mMinAngle;
		} else if (offsetAngle > mMaxAngle) {
			offsetAngle = mMaxAngle;
		}
		mOffsetAngle = offsetAngle;
		invalidate();
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		mOnItemClickListener = onItemClickListener;
	}

	public void setDisplayMode(DisplayMode displayMode) {
		mDisplayMode = displayMode;
		invalidate();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.startAngle = mStartAngle;
		ss.endAngle = mStartAngle;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		setAngleRange(ss.startAngle, ss.endAngle, ss.reserve);
	}

	public void setBackCircleColor(int backCircleColor) {
		mBackCircleColor = backCircleColor;
		invalidate();
	}

	public enum DisplayMode {
		FADE,
		EDGE
	}

	public interface CircleDialAdapter {
		int getCount();

		Bitmap getBitmap(int index);
	}

	public interface OnItemClickListener {
		void onItemClick(int index);
	}

	static class SavedState extends BaseSavedState {
		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		float startAngle;
		float endAngle;
		boolean reserve;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			startAngle = in.readFloat();
			endAngle = in.readFloat();
			reserve = (boolean) in.readValue(null);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeFloat(startAngle);
			out.writeFloat(endAngle);
			out.writeValue(reserve);
		}
	}
}
