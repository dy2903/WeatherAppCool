package com.dy.util;

import java.net.URLEncoder;

import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;

import com.dy.activity.MainActivity;
import com.dy.app.Application;
import com.dy.bean.City;
import com.dy.bean.WeatherInfo;

public class GetWeatherTask extends AsyncTask<Void, Void, Integer> {
	
	private static final String BASE_URL = "http://sixweather.3gpk.net/SixWeather.aspx?city=%s";
	private static final int SCUESS = 0;
	private static final int SCUESS_YUJING = 1;
	private static final int FAIL = -1;
	private Handler mHandler;
	private City mCity;
	private Application mApplication;

	public GetWeatherTask(Handler handler, City city) {
		this.mHandler = handler;
		this.mCity = city;
		mApplication = Application.getInstance();
	};

	@Override
	protected Integer doInBackground(Void... params) {
		try {			
//			替换掉网络URL里面的参数值
			String url = String.format(BASE_URL,
					URLEncoder.encode(mCity.getName(), "utf-8"));
			// 为了避免频繁刷新浪费流量，所以先读取内存中的信息
//			if (mApplication.getAllWeather() != null
//					&& mApplication.getAllWeather().getCity()
//							.equals(mCity.getName())) {
//				L.i("lwp", "get the weather info from memory");
//				return SCUESS;// 直接返回，不继续执行
//			}
			// 再读取文件中的缓存信息
			String fileResult = ConfigCache.getUrlCache(mCity.getPinyin());// 读取文件中的缓存
			if (!TextUtils.isEmpty(fileResult)) {
//				XML解析
				WeatherInfo allWeather = XmlPullParseUtil
						.parseWeatherInfo(fileResult);
				if (allWeather != null) {
					mApplication.SetAllWeather(allWeather);
					LogUtil.i("lwp", "get the weather info from file");
					return SCUESS;
				}
			}
			// 最后才执行网络请求
			String netResult = HttpUtil.connServerForResult(url);
			
			if (!TextUtils.isEmpty(netResult)) {
				WeatherInfo allWeather = XmlPullParseUtil
						.parseWeatherInfo(netResult);
				
				if (allWeather != null) {
					mApplication.SetAllWeather(allWeather);
					ConfigCache.setUrlCache(netResult, mCity.getPinyin());
					LogUtil.i("lwp", "get the weather info from network");
					String yujin = allWeather.getYujing();
					if (!TextUtils.isEmpty(yujin) && !yujin.contains("暂无预警"))
						return SCUESS_YUJING;
					return SCUESS;
				}
				
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FAIL;
	};
//执行以后
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		
		if(result < 0 ){
//			获取天气失败,通过handler把信息传递到MainActivity
			mHandler.sendEmptyMessage(MainActivity.GET_WEATHER_FAIL);// 获取天气信息失败
			LogUtil.i("lwp", "get weather fail");
		}else{
			mHandler.sendEmptyMessage(MainActivity.GET_WEATHER_SCUESS);// 获取天气信息成功，通知主线程更新
			LogUtil.i("lwp", "get weather scuess");
			LogUtil.i("lwp", mApplication.getAllWeather().toString());
//			预警
			if(result == SCUESS_YUJING){
				mApplication.showNotification();
			}
			
		}
	};
}
