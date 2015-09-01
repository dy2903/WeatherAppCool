package com.dy.plistview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PinnedHeaderListView extends ListView {
	/*
	 * 内部接口
	 */
	public interface PinnedHeaderAdapter {
		public static final int PINNED_HEADER_GONE = 0;
		public static final int PINNED_HEADER_VISIBLE = 1;
		public static final int PINNED_HEADER_PUSHED_UP = 2;
//		ListView当前指定的position的数据的状态，比如指定position的数据可能是该组的header
		int getPinnedHeaderState(int position);
//		如设置Header View的文本，图片等，这个方法是由调用者去实现的。
		void configurePinnedHeader(View header, int position, int alpha);
	};

	private static final int MAX_ALPHA = 255;
	
	private PinnedHeaderAdapter mAdapter;
	private View mHeaderView;
	private boolean mHeaderViewVisible;
	private int mHeaderViewWidth;
	private int mHeaderViewHeight;

	/**
	 * 构造方法
	 * @param context
	 */
	public PinnedHeaderListView(Context context) {
		super(context);
	}

	public PinnedHeaderListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PinnedHeaderListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/*
	 * 控制Header View位置和大小
	 * @see android.widget.AbsListView#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mHeaderView != null) {
			mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
			
			this.configureHeaderView(getFirstVisiblePosition());
		}
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mHeaderView != null) {
			measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
			mHeaderViewWidth = mHeaderView.getMeasuredWidth();
			mHeaderViewHeight = mHeaderView.getMeasuredHeight();
		}
	}
	/*
	 * 设置的是HeaderView
	 */
	public void setPinnedHeaderView(View view) {
		mHeaderView = view;
		if (mHeaderView != null) {
			setFadingEdgeLength(0);
		}
		requestLayout();
	}

	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
		mAdapter = (PinnedHeaderAdapter) adapter;
	}

	/**
	 * 配置Header View的位置
	 * @param position
	 */
	public void configureHeaderView(int position) {
		
		if (mHeaderView == null) {
			return;
		}
//		根据不同的状态 , 进行不同的操作
		int state = mAdapter.getPinnedHeaderState(position);
		switch (state) {	
//		不绘制Header View
		case PinnedHeaderAdapter.PINNED_HEADER_GONE: {
			mHeaderViewVisible = false;
			break;
		}
		
		case PinnedHeaderAdapter.PINNED_HEADER_VISIBLE: {
//			设置Header View的文本,图片
			mAdapter.configurePinnedHeader(mHeaderView, position, MAX_ALPHA);
			
			if (mHeaderView.getTop() != 0) {
				mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
			}
			mHeaderViewVisible = true;
			break;
		}
		
//		可能需要根据不同的位移来计算Header View的移动位移。
		case PinnedHeaderAdapter.PINNED_HEADER_PUSHED_UP: {
//			开头的view
			View firstView = getChildAt(0);
			
			int bottom = firstView.getBottom();
//			当前的View的位置
			int headerHeight = mHeaderView.getHeight();
			int y;
			int alpha;
//			firstView没有到达底部
			if (bottom < headerHeight) {
//				修剪
				y = (bottom - headerHeight);
				alpha = MAX_ALPHA * (headerHeight + y) / headerHeight;
			} else {
				y = 0;
				alpha = MAX_ALPHA;
			}
			
			mAdapter.configurePinnedHeader(mHeaderView, position, alpha);
			
			if (mHeaderView.getTop() != y) {
				mHeaderView.layout(0, y, mHeaderViewWidth, mHeaderViewHeight
						+ y);
			}
			
			mHeaderViewVisible = true;
			break;
		}
		
		}
	};
	
	
	/**
	 * 绘制Header View
	 */
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHeaderViewVisible) {
			this.drawChild(canvas, mHeaderView, getDrawingTime());
		}
	}
}