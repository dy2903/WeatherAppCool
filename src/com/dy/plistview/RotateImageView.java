package com.dy.plistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.way.weather.R;

public class RotateImageView extends ImageView {
	private Animation mRotateAnimation;
	private boolean isAnim;
//	构造函数
	public RotateImageView(Context context) {
		this(context, null);
//		RotateAnimation        画面旋转
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}

	public RotateImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}

	public RotateImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}
	public boolean isStartAnim(){
		return isAnim;
	}
	public synchronized void startAnim() {
//		先停止再启动
		stopAnim();
		this.startAnimation(mRotateAnimation);
//		启动动画
		isAnim = true;

	}

	public synchronized void stopAnim() {
		if (isAnim){
			this.clearAnimation();
			isAnim = false;
		}
	}
}
