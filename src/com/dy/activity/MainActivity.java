package com.dy.activity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.dy.adapter.WeatherPagerAdapter;
import com.dy.app.Application;
import com.dy.bean.City;
import com.dy.bean.WeatherInfo;
import com.dy.db.CityDB;
import com.dy.fragment.FirstWeatherFragment;
import com.dy.fragment.SecondWeatherFragment;
import com.dy.indicator.CirclePageIndicator;
import com.dy.plistview.RotateImageView;
import com.dy.receiver.NetBroadcastReceiver;
import com.dy.receiver.NetBroadcastReceiver.EventHandler;
import com.dy.util.GetWeatherTask;
import com.dy.util.IphoneDialog;
import com.dy.util.NetUtil;
import com.dy.util.SharePreferenceUtil;
import com.dy.util.ShareUtil;
import com.dy.util.ToastUtil;
import com.dy.util.TimeUtil;
import com.way.weather.R;

public class MainActivity extends FragmentActivity implements EventHandler,
		OnClickListener {
	
	public static final String UPDATE_WIDGET_WEATHER_ACTION = "com.way.action.update_weather";
	private static final int LOACTION_OK = 0;
	private static final int UPDATE_EXISTS_CITY = 2;
	public static final int GET_WEATHER_SCUESS = 3;
	public static final int GET_WEATHER_FAIL = 4;
	
	private LocationClient mLocationClient;
	private CityDB mCityDB;
	private SharePreferenceUtil mSpUtil;
	private Application mApplication;
	private View mSplashView;
	private static final int SHOW_TIME_MIN = 3000;// 最小显示时间
	private long mStartTime;// 开始时间
	private ImageView mCityManagerBtn, mLocationBtn, mShareBtn;
	private RotateImageView mUpdateProgressBar;
	private TextView mTitleTextView;
	private WeatherPagerAdapter mWeatherPagerAdapter;

	private TextView cityTv, timeTv, weekTv, aqiDataTv, aqiQualityTv,
			temperatureTv, climateTv, windTv;
	
	private View mAqiRootView;
	private View mShareView;
	private ImageView weatherImg, aqiImg;;
	private ViewPager mViewPager;
	private List<Fragment> fragments;
	private ShareUtil mShareUtil;
	/**
	 * 建立Handler的实例,处理从其他的Application中传递的信息
	 */

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
//			如果定位成功,更新天气
			case LOACTION_OK:
//				获得当前的城市名
				City curCity = (City) msg.obj;
				if (curCity != null) {
//					T.showShort(MainActivity.this, "定位到：" + curCity.getName());
					mSpUtil.setCity(curCity.getName());
//					更新天气
					MainActivity.this.updateWeather(curCity);
				}
				break;
				
				
//			更新城市名称	
			case UPDATE_EXISTS_CITY:
//				从SharePreferences中获得城市名
				String sPCityName = mSpUtil.getCity();
				MainActivity.this.updateWeather(mCityDB.getCity(sPCityName));
				break;				
				
//				成功获得天气信息
			case GET_WEATHER_SCUESS:
//				通过Application的方法来获得Weather信息
				WeatherInfo weatherInfo = (mApplication.getAllWeather());
//				更新天气界面
				MainActivity.this.updateWeatherInfo(weatherInfo);
//				更新AQI
				MainActivity.this.updateAqiInfo(weatherInfo);
//				更新组件
				MainActivity.this.updateWidgetWeather();
//				停止更新的动画
				mUpdateProgressBar.stopAnim();
//				计算花费的时间
				long loadingTime = System.currentTimeMillis() - mStartTime;// 计算一下总共花费的时间
				if (loadingTime < SHOW_TIME_MIN) {// 如果比最小显示时间还短，就延时进入MainActivity，否则直接进入
//					延时 , splashGone是一个线程.
					mHandler.postDelayed(splashGone, SHOW_TIME_MIN
							- loadingTime);
				} else {
					mHandler.post(splashGone);
				}
				break;
				
//				获取天气失败
			case GET_WEATHER_FAIL:
				MainActivity.this.updateWeatherInfo(null);
				MainActivity.this.updateAqiInfo(null);
				ToastUtil.show(MainActivity.this, "获取天气失败，请重试", Toast.LENGTH_SHORT);
				MainActivity.this.mUpdateProgressBar.stopAnim();
				break;
				
			default:
				break;
			}
		}
	};
	
	
	// 正式进入MainActivity
	Runnable splashGone = new Runnable() {

		@Override
		public void run() {
			
			Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
					R.anim.push_right_out);
//			监听
			anim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					mSplashView.setVisibility(View.GONE);
				}
			});
//			开启动画
			mSplashView.startAnimation(anim);
		}		
	};
/**
 * 更新组件
 */
	private void updateWidgetWeather() {
//		发送一条广播即可
		sendBroadcast(new Intent(UPDATE_WIDGET_WEATHER_ACTION));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.initData();
		this.initView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager notificationManager = mApplication
				.getNotificationManager();
		if (notificationManager != null)
			notificationManager.cancelAll();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		NetBroadcastReceiver.mListeners.remove(this);
	}

	/*
	 * 进入SelectCityActivty
	 */
	private void startActivityForResult() {
		Intent i = new Intent(this, SelectCtiyActivity.class);
		this.startActivityForResult(i, 0);
	}
	/*
	 * 初始化界面
	 */
	private void initView() {
		mSplashView = findViewById(R.id.splash_view);
		mCityManagerBtn = (ImageView) findViewById(R.id.title_city_manager);
		mShareBtn = (ImageView) findViewById(R.id.title_share);
		mLocationBtn = (ImageView) findViewById(R.id.title_location);
		mCityManagerBtn.setOnClickListener(this);
		mShareBtn.setOnClickListener(this);
		mLocationBtn.setOnClickListener(this);
		mUpdateProgressBar = (RotateImageView) findViewById(R.id.title_update_progress);
		mUpdateProgressBar.setOnClickListener(this);
		mTitleTextView = (TextView) findViewById(R.id.title_city_name);

		cityTv = (TextView) findViewById(R.id.city);
		timeTv = (TextView) findViewById(R.id.time);
		timeTv.setText("未发布");
		weekTv = (TextView) findViewById(R.id.week_today);

		mAqiRootView = findViewById(R.id.aqi_root_view);
		mAqiRootView.setVisibility(View.INVISIBLE);
		mShareView = findViewById(R.id.weather_share_view);
		aqiDataTv = (TextView) findViewById(R.id.pm_data);
		aqiQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
		aqiImg = (ImageView) findViewById(R.id.pm2_5_img);
		temperatureTv = (TextView) findViewById(R.id.temperature);
		climateTv = (TextView) findViewById(R.id.climate);
		windTv = (TextView) findViewById(R.id.wind);
		weatherImg = (ImageView) findViewById(R.id.weather_img);
//		碎片
		fragments = new ArrayList<Fragment>();
		fragments.add(new FirstWeatherFragment());
		fragments.add(new SecondWeatherFragment());
		
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mWeatherPagerAdapter = new WeatherPagerAdapter(
				getSupportFragmentManager(), fragments);
		mViewPager.setAdapter(mWeatherPagerAdapter);
		((CirclePageIndicator) findViewById(R.id.indicator))
				.setViewPager(mViewPager);
		if (TextUtils.isEmpty(mSpUtil.getCity())) {
			if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
				mLocationClient.start();
				mLocationClient.requestLocation();
				mUpdateProgressBar.startAnim();
			} else {
				ToastUtil.showShort(this, R.string.net_err);
			}
		} else {
			mHandler.sendEmptyMessage(UPDATE_EXISTS_CITY);
		}
	}
	/**
	 * 初始化数据
	 */
	private void initData() {
		mStartTime = System.currentTimeMillis();// 记录开始时间
		
		NetBroadcastReceiver.mListeners.add(this);
//		获得Application的单实例
		mApplication = Application.getInstance();
		mSpUtil = mApplication.getSharePreferenceUtil();
		mLocationClient = mApplication.getLocationClient();
		mLocationClient.registerLocationListener(mLocationListener);
		mCityDB = mApplication.getCityDB();
//		分享功能
		mShareUtil = new ShareUtil(this, mHandler);
	}

	/**
	 * 开启AsyncTask进行天气信息的抓取
	 * @param city
	 */
	private void updateWeather(City city) {
		// 因为在异步任务中有判断网络连接问题，在网络请求前先获取文件中的缓存，所以，此处未处理网络连接问题
		if (city == null) {
			ToastUtil.showLong(mApplication, "未找到此城市,请重新定位或选择...");
			return;
		}
		timeTv.setText("同步中...");
		mTitleTextView.setText(city.getName());
//		更新时的动画
		mUpdateProgressBar.startAnim();
//		开启异步Task进行天气的抓取
		new GetWeatherTask(mHandler, city).execute();
	}

	/**
	 * 更新天气界面
	 */
	private void updateWeatherInfo(WeatherInfo allWeather) {
//		今天的星期
		weekTv.setText("今天 " + TimeUtil.getWeek(0, TimeUtil.XING_QI));
//		如果有fragments,则进行更新
		if (fragments.size() > 0) {
			((FirstWeatherFragment) mWeatherPagerAdapter.getItem(0))
					.updateWeather(allWeather);
			((SecondWeatherFragment) mWeatherPagerAdapter.getItem(1))
					.updateWeather(allWeather);
		}
		
		if (allWeather != null) {
			cityTv.setText(allWeather.getCity());
//			温度
			if (!TextUtils.isEmpty(allWeather.getFeelTemp())) {
				temperatureTv.setText(allWeather.getFeelTemp());
//				保存温度
				mSpUtil.setSimpleTemp(allWeather.getFeelTemp()
						.replace("~", "/").replace("℃", "°"));// 保存一下温度信息，用户小插件
			} else {
				temperatureTv.setText(allWeather.getTemp0());
				mSpUtil.setSimpleTemp(allWeather.getTemp0().replace("~", "/")
						.replace("℃", "°"));
			}
			
			String climate = allWeather.getWeather0();
			climateTv.setText(climate);
			mSpUtil.setSimpleClimate(climate);// 保存一下天气信息，用户小插件

			weatherImg.setImageResource(this.getWeatherIcon(climate));
			windTv.setText(allWeather.getWind0());

			String time = allWeather.getIntime();
			mSpUtil.setTimeSamp(TimeUtil.getLongTime(time));// 保存一下更新的时间戳，记录更新时间
			timeTv.setText(TimeUtil.getDay(mSpUtil.getTimeSamp()) + "发布");
		} else {
			cityTv.setText(mSpUtil.getCity());
			timeTv.setText("未同步");
			temperatureTv.setText("N/A");
			climateTv.setText("N/A");
			windTv.setText("N/A");
			weatherImg.setImageResource(R.drawable.na);
			ToastUtil.showLong(mApplication, "获取天气信息失败");
		};
		
	}

	/**
	 * 更新AQI界面
	 */
	private void updateAqiInfo(WeatherInfo allWeather) {
		
		if (allWeather != null && allWeather.getAQIData() != null) {
			
			mAqiRootView.setVisibility(View.VISIBLE);
			aqiDataTv.setText(allWeather.getAQIData());
			
			int aqi = Integer.parseInt(allWeather.getAQIData());
			int aqi_img = R.drawable.biz_plugin_weather_0_50;
			String aqiText = "无数据";
//			根据不同的污染程度来选择不同的图片
			if (aqi > 300) {
				aqi_img = R.drawable.biz_plugin_weather_greater_300;
				aqiText = "严重污染";
			} else if (aqi > 200) {
				aqi_img = R.drawable.biz_plugin_weather_201_300;
				aqiText = "重度污染";
			} else if (aqi > 150) {
				aqi_img = R.drawable.biz_plugin_weather_151_200;
				aqiText = "中度污染";
			} else if (aqi > 100) {
				aqi_img = R.drawable.biz_plugin_weather_101_150;
				aqiText = "轻度污染";
			} else if (aqi > 50) {
				aqi_img = R.drawable.biz_plugin_weather_51_100;
				aqiText = "良";
			} else {
				aqi_img = R.drawable.biz_plugin_weather_0_50;
				aqiText = "优";
			}
			
			aqiImg.setImageResource(aqi_img);
			aqiQualityTv.setText(aqiText);
			
		} else {
			mAqiRootView.setVisibility(View.INVISIBLE);
			aqiQualityTv.setText("");
			aqiDataTv.setText("");
			aqiImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
			ToastUtil.showShort(mApplication, "该城市暂无空气质量数据");
		}
	};
	/**
	 * 获得天气的图片
	 * @param climate
	 * @return
	 */
	private int getWeatherIcon(String climate) {
		int weatherIcon = R.drawable.biz_plugin_weather_qing;
		if (climate.contains("转")) {// 天气带转字，取前面那部分
			String[] strs = climate.split("转");
			climate = strs[0];
			if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
				strs = climate.split("到");
				climate = strs[1];
			}
		}
		if (mApplication.getWeatherIconMap().containsKey(climate)) {
			weatherIcon = mApplication.getWeatherIconMap().get(climate);
		}
		return weatherIcon;
	}

	/*
	 * 成员变量
	 */
	BDLocationListener mLocationListener = new BDLocationListener() {

		@Override
		public void onReceivePoi(BDLocation arg0) {
			// do nothing
		}

		@Override
		public void onReceiveLocation(BDLocation location) {
			mUpdateProgressBar.stopAnim();
			if (location == null || TextUtils.isEmpty(location.getCity())) {
				MainActivity.this.showLocationFailDialog();
				return;
			}
			// 获取当前城市，
			String cityName = location.getCity();
			mLocationClient.stop();
			// 从数据库中找到该城市
			City curCity = mCityDB.getCity(cityName);
//			发送Handler, 更新UI
			if (curCity != null) {
				Message msg = mHandler.obtainMessage();
				msg.what = LOACTION_OK;
				msg.obj = curCity;
				mHandler.sendMessage(msg);// 更新天气
			} else {// 如果定位到的城市数据库中没有，也弹出定位失败
				MainActivity.this.showLocationFailDialog();
			}
		}
	};

	/**
	 * 定位失败后处理
	 */
	private void showLocationFailDialog() {
		final Dialog dialog = IphoneDialog.getTwoBtnDialog(MainActivity.this,
				"定位失败", "是否手动选择城市?");
		((Button) dialog.findViewById(R.id.ok))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						MainActivity.this.startActivityForResult();
						dialog.dismiss();
					}
				});
		dialog.show();
	}
	/**
	 * 处理得到的City对象
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			City city = (City) data.getSerializableExtra("city");
			mSpUtil.setCity(city.getName());
			this.updateWeather(city);
		}
	}
	
	/*
	 * 网络状态改变以后的处理
	 * @see com.way.app.NetBroadcastReceiver.EventHandler#onNetChange()
	 */
	@Override
	public void onNetChange() {
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
			ToastUtil.showLong(this, R.string.net_err);
		} else {
			if (!TextUtils.isEmpty(mSpUtil.getCity()))
//				更新City
				mHandler.sendEmptyMessage(UPDATE_EXISTS_CITY);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_city_manager:
			this.startActivityForResult();
			break;
//			定位
		case R.id.title_location:
			if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
				if (!mLocationClient.isStarted())
					mLocationClient.start();
				mLocationClient.requestLocation();
				ToastUtil.showShort(this, "正在定位...");
			} else {
				ToastUtil.showShort(this, R.string.net_err);
			}
			break;
//			分享
		case R.id.title_share:
			if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
				byte[] bm = getSharePicture();
				if (bm != null && mApplication.getAllWeather() != null)
					mShareUtil.share(bm,
							getShareContent(mApplication.getAllWeather()));
				else
					ToastUtil.showShort(this, "分享失败");
			} else {
				ToastUtil.showShort(this, R.string.net_err);
			}
			break;
			
		case R.id.title_update_progress:
			if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
				if (TextUtils.isEmpty(mSpUtil.getCity())) {
					ToastUtil.showShort(this, "请先选择城市或定位！");
				} else {
					String sPCityName = mSpUtil.getCity();
					City curCity = mCityDB.getCity(mSpUtil.getCity());
					updateWeather(curCity);
				}
			} else {
				ToastUtil.showShort(this, R.string.net_err);
			}
			break;

		default:
			break;
		}
	}

	/**
	 * 获得分享的图片
	 * @return
	 */
	private byte[] getSharePicture() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mShareView.setDrawingCacheEnabled(true);
			Bitmap.createBitmap(mShareView.getDrawingCache()).compress(
					Bitmap.CompressFormat.PNG, 100, baos);
			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
//	分享的链接的格式
	private String mShare = "今日%s天气：%s，温度：%s；空气质量指数(AQI)：%s，PM2.5 浓度值：%s μg/m3。";
	private String mShareSimple = "今日%s天气：%s，温度：%s，湿度：%s，风向：%s。";
	
	private String getShareContent(WeatherInfo weatherInfo) {
		String aqi = weatherInfo.getAQIData();
		String pm2d5 = weatherInfo.getPM2Dot5Data();
//		详细版
		if (!TextUtils.isEmpty(aqi) && !TextUtils.isEmpty(pm2d5))
			return String.format(mShare, new Object[] { weatherInfo.getCity(),
					weatherInfo.getWeather0(), weatherInfo.getTemp0(),
					weatherInfo.getAQIData(), weatherInfo.getPM2Dot5Data() });
//		简洁版
		return String.format(mShareSimple,
				new Object[] { weatherInfo.getCity(),
						weatherInfo.getWeather0(), weatherInfo.getTemp0(),
						weatherInfo.getShidu(), weatherInfo.getWinNow() });
	}

	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
			ToastUtil.showShort(this, R.string.press_again_exit);
		}
	}

}
