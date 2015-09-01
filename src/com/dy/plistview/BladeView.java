package com.dy.plistview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.way.weather.R;

public class BladeView extends View {
	private OnItemClickListener mOnItemClickListener;
	String[] b = { "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
			"Y", "Z" };
	int choose = -1;
	Paint paint = new Paint();
	boolean showBkg = false;
	private PopupWindow mPopupWindow;
	private TextView mPopupText;
	private int mCharHeight = 15;
	/*
	 * 构造方法.
	 */
	public BladeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mCharHeight = context.getResources().getDimensionPixelSize(R.dimen.blade_view_text_size);
	}

	public BladeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCharHeight = context.getResources().getDimensionPixelSize(R.dimen.blade_view_text_size);
	}

	public BladeView(Context context) {
		super(context);
		mCharHeight = context.getResources().getDimensionPixelSize(R.dimen.blade_view_text_size);
	}

/*
 * 点击侧边栏,会改变字母的背景
 * 针对的只是一个祖母
 * @see android.view.View#onDraw(android.graphics.Canvas)
 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		showBkg用来设置是否显示背景
		if (showBkg) {
			canvas.drawColor(Color.parseColor("#4d000000"));
		} else {
			canvas.drawColor(Color.parseColor("#00000000"));
		}
//		SideBar的尺寸
		int height = getHeight();
		int width = getWidth();
//		单个字母的高度
		int singleHeight = height / b.length;
//		遍历字母数组,如果有选中的设置画笔, 改变背景
		for (int i = 0; i < b.length; i++) {
			paint.setColor(Color.parseColor("#ff555555"));
//			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setTextSize(mCharHeight);
			paint.setFakeBoldText(true);
			paint.setAntiAlias(true);
			
			if (i == choose) {
				paint.setColor(Color.parseColor("#3399ff"));
			}
			
			float xPos = width / 2 - paint.measureText(b[i]) / 2;
			float yPos = singleHeight * i + singleHeight;
			
			canvas.drawText(b[i], xPos, yPos, paint);
			paint.reset();
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		dismissPopup();
		return super.onSaveInstanceState();
	}
	/*
	 * 处理触摸事件.
	 * @see android.view.View#dispatchTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float y = event.getY();
		final int oldChoose = choose;
//		点击的是哪个字母
//		比例 * 数组长度
		final int c = (int) (y / getHeight() * b.length);

		switch (action) {
//		按下
//		只要选中的字母在数组里面,进行操作
		case MotionEvent.ACTION_DOWN:
			showBkg = true;
			if (oldChoose != c) {
				if (c > 0 && c < b.length) {
					this.performItemClicked(c);
					choose = c;
					invalidate();
				}
			}

			break;
		
		case MotionEvent.ACTION_MOVE:
			if (oldChoose != c) {
				if (c > 0 && c < b.length) {
					this.performItemClicked(c);
					choose = c;
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			showBkg = false;
			choose = -1;
			dismissPopup();
			invalidate();
			break;
		}
		return true;
	};

	/*
	 * 显示"弹出"
	 * 设置背景 
	 */
	private void showPopup(int item) {
		if (mPopupWindow == null) {
			mPopupText = new TextView(getContext());
			mPopupText
					.setBackgroundResource(R.drawable.ic_contacts_index_backgroud_sprd);
			mPopupText.setTextColor(Color.CYAN);
			mPopupText.setTextSize(50);
			mPopupText.setGravity(Gravity.CENTER_HORIZONTAL
					| Gravity.CENTER_VERTICAL);
			mPopupWindow = new PopupWindow(mPopupText,
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}

		String text = "";
		if (item == 0) {
			text = "#";
		} else {
			text = Character.toString((char) ('A' + item - 1));
		}
		mPopupText.setText(text);
		
		if (mPopupWindow.isShowing()) {
			mPopupWindow.update();
		} else {
			mPopupWindow.showAtLocation(getRootView(),
					Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
		}
	};

	private void dismissPopup() {
		if (mPopupWindow != null) {
			mPopupWindow.dismiss();
		}
	};

	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}

	
	private void performItemClicked(int item) {
//		如果有Listener
		if (mOnItemClickListener != null) {
			mOnItemClickListener.onItemClick(b[item]);
			this.showPopup(item);
		}
	}
	/*
	 * 内部类,按键监听
	 * 外部调用:可以用来过滤等操作
	 */
	public interface OnItemClickListener {
		void onItemClick(String s);
	};

}
