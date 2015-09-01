package com.dy.receiver;

import java.util.ArrayList;

import com.dy.app.Application;
import com.dy.util.NetUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/**
 * 网络变化广播
 * @author DX2903
 *
 */
public class NetBroadcastReceiver extends BroadcastReceiver {
//	内部类的List
	public static ArrayList<EventHandler> mListeners = new ArrayList<EventHandler>();
	private static String NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		观察者模式
		if (intent.getAction().equals(NET_CHANGE_ACTION)) {
			Application.mNetWorkState = NetUtil.getNetworkState(context);
			// 通知接口完成加载
			if (mListeners.size() > 0)
				for (EventHandler handler : mListeners) {
					handler.onNetChange();
				}
		}
	}
	
//	接口对netChange进行处理
	public static abstract interface EventHandler {

		public abstract void onNetChange();
	};
	
}
