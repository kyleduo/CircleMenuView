package com.kyleduo.circlemenuviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	private ImageView mLickBt;
	private int[] icons = new int[]{
			R.drawable.icon1,
			R.drawable.icon2,
			R.drawable.icon3,
			R.drawable.icon4,
			R.drawable.icon5,
			R.drawable.icon6,
			R.drawable.icon0,
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mLickBt = (ImageView) findViewById(R.id.like_bt);

		mLickBt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup(v);
			}
		});
	}


	private void showPopup(View v) {
		View content = LayoutInflater.from(this).inflate(R.layout.layout_popup, null);
		final PopupWindow window = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		window.setOutsideTouchable(true);
		window.setFocusable(true);
		window.setTouchable(true);
		window.setAnimationStyle(R.style.FadeStyle);

		final CircleMenuView cdv = (CircleMenuView) content.findViewById(R.id.pop_circle);
		final ImageView iv = (ImageView) content.findViewById(R.id.pop_iv);
		if (v instanceof ImageView) {
			iv.setImageDrawable(((ImageView) v).getDrawable());
		}

		int anchorWidth = v.getMeasuredWidth();
		int anchorHeight = v.getMeasuredHeight();
		int space = (int) (getResources().getDisplayMetrics().density * 12);
		int inset = (int) (getResources().getDisplayMetrics().density * 8);

		cdv.setTrackRadius(Math.max(anchorHeight, anchorWidth) * 0.7f);
		cdv.setInset(inset);
		cdv.setAngleRange(-45, 135, false);
		cdv.setOnItemClickListener(new CircleMenuView.OnItemClickListener() {
			@Override
			public void onItemClick(int index) {
				Toast.makeText(MainActivity.this, "click index: " + index, Toast.LENGTH_SHORT).show();
				window.dismiss();
				iv.setImageResource(icons[index % icons.length]);
				mLickBt.setImageResource(icons[index % icons.length]);
			}
		});
		cdv.setCircleMenuAdapter(new CircleMenuView.CircleDialAdapter() {
			@Override
			public int getCount() {
				return icons.length;
			}

			@Override
			public Bitmap getBitmap(int index) {
				return BitmapFactory.decodeResource(getResources(), icons[index % icons.length]);
			}
		});

		content.measure(View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST));
		int cWidth = content.getMeasuredWidth();
		int cHeight = content.getMeasuredHeight();

		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) iv.getLayoutParams();
		lp.width = v.getMeasuredWidth();
		lp.height = v.getMeasuredHeight();
		lp.rightMargin = space;
		lp.bottomMargin = space;
//		iv.requestLayout();

		window.showAsDropDown(v, space - cWidth + v.getMeasuredWidth(), space - cHeight);
		window.update();

//		Handler handler = new Handler();
//
//		handler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				cdv.smoothScrollTo(6);
//			}
//		}, 1000);
//
//		handler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				cdv.smoothScrollTo(0);
//			}
//		}, 2000);
	}
}
