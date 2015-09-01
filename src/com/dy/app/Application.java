package com.dy.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dy.activity.MainActivity;
import com.dy.bean.City;
import com.dy.bean.WeatherInfo;
import com.dy.db.CityDB;
import com.dy.util.LogUtil;
import com.dy.util.NetUtil;
import com.dy.util.SharePreferenceUtil;
import com.dy.util.ToastUtil;
import com.way.weather.R;

/**
 * 自定义的Application,可以存放全局的属性,变量,传递数据等
 * @author DX2903
 *
 */
public class Application extends android.app.Application {
	public static final int CITY_LIST_SCUESS = 100;
	/*
	 * 静态变量
	 */
//	只能是字母
	private static final String FORMAT = "^[a-z,A-Z].*$";
	private static Application mApplication;
	
	private CityDB mCityDB;
//	图标
	private HashMap<String, Integer> mWeatherIcon;// 天气图标
	private HashMap<String, Integer> mWidgetWeatherIcon;// 插件天气图标
	//选择City
	private List<City> mCityList;
	// 首字母集
	private List<String> mSections;
	// 根据首字母存放数据
	private Map<String, List<City>> mMap;
	// 首字母位置集
	private List<Integer> mPositions;
	// 首字母对应的位置
	private Map<String, Integer> mIndexer;

	private LocationClient mLocationClient = null;
	private SharePreferenceUtil mSpUtil;
	
	private WeatherInfo mAllWeather;
	public static int mNetWorkState;
	private NotificationManager mNotificationManager;
	
//	单例模式
	public static synchronized Application getInstance() {
		return mApplication;
	}
	/*
	 * 开启程序,自动执行,比其他的组件都要早
	 * (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;
//		开启dB
		mCityDB = this.openCityDB();// 这个必须最先复制完,所以我放在单线程中处理,待优化
		initData();
	}
	
	@Override
	public void onTerminate() {
		LogUtil.i("Application onTerminate...");
		super.onTerminate();
		if (mCityDB != null && mCityDB.isOpen())
			mCityDB.close();
	}

	/*
	 * 当程序在后台运行时，释放这部分最占内存的资源
	 */
	public void free() {
		mCityList = null;
		mSections = null;
		mMap = null;
		mPositions = null;
		mIndexer = null;
		mWeatherIcon = null;
		mAllWeather = null;
		System.gc();
	}
	/*
	 * //定位
	 */
	private LocationClientOption getLocationClientOption() {
		LocationClientOption option = new LocationClientOption();
//		加上各种参数
		option.setOpenGps(true);
		option.setAddrType("all");
		option.setServiceName(this.getPackageName());
		option.setScanSpan(0);
		option.disableCache(true);
		return option;
	}
	/*
	 * 初始化数据
	 */
	public void initData() {
//		获得当前网络状态
		mNetWorkState = NetUtil.getNetworkState(this);
//		初始化CityList
		this.initCityList();
//		获得locationClient
		mLocationClient = new LocationClient(this, getLocationClientOption());
//		初始化图片ID和key的对应关系
		this.initWeatherIconMap();
		this.initWidgetWeather();
//		新建一个SharePreferenceUtil
		mSpUtil = new SharePreferenceUtil(this,
				SharePreferenceUtil.CITY_SHAREPRE_FILE);
//		通知管理器
		mNotificationManager = (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	}
	/**
	 * 获得dB连接
	 * @return
	 */
	public synchronized CityDB getCityDB() {
		if (mCityDB == null || !mCityDB.isOpen())
			mCityDB = this.openCityDB();
		return mCityDB;
	}

	/*
	 * 获得SharePreferenceUtil
	 */
	public synchronized SharePreferenceUtil getSharePreferenceUtil() {
		if (mSpUtil == null)
			mSpUtil = new SharePreferenceUtil(this,
					SharePreferenceUtil.CITY_SHAREPRE_FILE);
		return mSpUtil;
	}
	
	public synchronized LocationClient getLocationClient() {
		if (mLocationClient == null)
			mLocationClient = new LocationClient(this,
					getLocationClientOption());
		return mLocationClient;
	}
	/**
	 * 打开dB
	 * @return
	 */
	private CityDB openCityDB() {
		String path = "/data"
				+ Environment.getDataDirectory().getAbsolutePath()
				+ File.separator + "com.way.weather" + File.separator
				+ CityDB.CITY_DB_NAME;
		File db = new File(path);
		if (!db.exists() || getSharePreferenceUtil().getVersion() < 0) {
			LogUtil.i("db is not exists");
			try {
			/*
			 * 复制一份
			 */				
//				Return an AssetManager instance for your application's package.
				InputStream is = this.getAssets().open(CityDB.CITY_DB_NAME);
				FileOutputStream fos = new FileOutputStream(db);
				int len = -1;
				byte[] buffer = new byte[1024];
				while ((len = is.read(buffer)) != -1) {
					fos.write(buffer, 0, len);
					fos.flush();
				}
				fos.close();
				is.close();
				// 用于管理数据库版本，如果数据库有重大更新时使用
				getSharePreferenceUtil().setVersion(1);
				
			} catch (IOException e) {
				e.printStackTrace();
				ToastUtil.showLong(mApplication, e.getMessage());
				System.exit(0);
			}
		}
		return new CityDB(this, path);
	};

	public List<City> getCityList() {
		return mCityList;
	}

	public List<String> getSections() {
		return mSections;
	}

	public Map<String, List<City>> getMap() {
		return mMap;
	}

	public List<Integer> getPositions() {
		return mPositions;
	}

	public Map<String, Integer> getIndexer() {
		return mIndexer;
	}


	public Map<String, Integer> getWeatherIconMap() {
		if (mWeatherIcon == null || mWeatherIcon.isEmpty())
			mWeatherIcon = initWeatherIconMap();
		return mWeatherIcon;
	}

	public NotificationManager getNotificationManager() {
		return mNotificationManager;
	}

	/**
	 * 获得天气图片
	 * 晴转多云
	 * @param climate
	 * @return int
	 */
	public int getWeatherIcon(String climate) {
		
		int weatherRes = R.drawable.biz_plugin_weather_qing;
		
		if (TextUtils.isEmpty(climate))
			return weatherRes;
//		初始化
		String[] strs = { "晴", "晴" };
//		对climate这个字符串进行处理
		if (climate.contains("转")) {// 天气带转字，取前面那部分
			strs = climate.split("转");
			climate = strs[0];
			if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
				strs = climate.split("到");
				climate = strs[1];
			}
		}
		if (mWeatherIcon == null || mWeatherIcon.isEmpty())
			mWeatherIcon = this.initWeatherIconMap();
		
		if (mWeatherIcon.containsKey(climate)) {
			weatherRes = mWeatherIcon.get(climate);
		}
		return weatherRes;
	};

	public int getWidgetWeatherIcon(String climate) {
		int weatherRes = R.drawable.na;
		if (TextUtils.isEmpty(climate))
			return weatherRes;
		String[] strs = { "晴", "晴" };
		if (climate.contains("转")) {// 天气带转字，取前面那部分
			strs = climate.split("转");
			climate = strs[0];
			if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
				strs = climate.split("到");
				climate = strs[1];
			}
		}
		if (mWidgetWeatherIcon == null || mWidgetWeatherIcon.isEmpty())
			mWidgetWeatherIcon = initWidgetWeather();
		if (mWidgetWeatherIcon.containsKey(climate)) {
			weatherRes = mWidgetWeatherIcon.get(climate);
		}
		return weatherRes;
	};

	public WeatherInfo getAllWeather() {
		return mAllWeather;
	};

	public void SetAllWeather(WeatherInfo allWeather) {
		mAllWeather = allWeather;
	}
	/**
	 * 把图片ID和关键字联系起来
	 * @return
	 */
	private HashMap<String, Integer> initWeatherIconMap() {
		if (mWeatherIcon != null && !mWeatherIcon.isEmpty())
			return mWeatherIcon;
		mWeatherIcon = new HashMap<String, Integer>();
		mWeatherIcon.put("暴雪", R.drawable.biz_plugin_weather_baoxue);
		mWeatherIcon.put("暴雨", R.drawable.biz_plugin_weather_baoyu);
		mWeatherIcon.put("大暴雨", R.drawable.biz_plugin_weather_dabaoyu);
		mWeatherIcon.put("大雪", R.drawable.biz_plugin_weather_daxue);
		mWeatherIcon.put("大雨", R.drawable.biz_plugin_weather_dayu);

		mWeatherIcon.put("多云", R.drawable.biz_plugin_weather_duoyun);
		mWeatherIcon.put("雷阵雨", R.drawable.biz_plugin_weather_leizhenyu);
		mWeatherIcon.put("雷阵雨冰雹",
				R.drawable.biz_plugin_weather_leizhenyubingbao);
		mWeatherIcon.put("晴", R.drawable.biz_plugin_weather_qing);
		mWeatherIcon.put("沙尘暴", R.drawable.biz_plugin_weather_shachenbao);

		mWeatherIcon.put("特大暴雨", R.drawable.biz_plugin_weather_tedabaoyu);
		mWeatherIcon.put("雾", R.drawable.biz_plugin_weather_wu);
		mWeatherIcon.put("小雪", R.drawable.biz_plugin_weather_xiaoxue);
		mWeatherIcon.put("小雨", R.drawable.biz_plugin_weather_xiaoyu);
		mWeatherIcon.put("阴", R.drawable.biz_plugin_weather_yin);

		mWeatherIcon.put("雨夹雪", R.drawable.biz_plugin_weather_yujiaxue);
		mWeatherIcon.put("阵雪", R.drawable.biz_plugin_weather_zhenxue);
		mWeatherIcon.put("阵雨", R.drawable.biz_plugin_weather_zhenyu);
		mWeatherIcon.put("中雪", R.drawable.biz_plugin_weather_zhongxue);
		mWeatherIcon.put("中雨", R.drawable.biz_plugin_weather_zhongyu);
		return mWeatherIcon;
	};
	
	private HashMap<String, Integer> initWidgetWeather() {
		if (mWidgetWeatherIcon != null && !mWidgetWeatherIcon.isEmpty())
			return mWidgetWeatherIcon;
		mWidgetWeatherIcon = new HashMap<String, Integer>();
		mWidgetWeatherIcon.put("暴雪", R.drawable.w17);
		mWidgetWeatherIcon.put("暴雨", R.drawable.w10);
		mWidgetWeatherIcon.put("大暴雨", R.drawable.w10);
		mWidgetWeatherIcon.put("大雪", R.drawable.w16);
		mWidgetWeatherIcon.put("大雨", R.drawable.w9);

		mWidgetWeatherIcon.put("多云", R.drawable.w1);
		mWidgetWeatherIcon.put("雷阵雨", R.drawable.w4);
		mWidgetWeatherIcon.put("雷阵雨冰雹", R.drawable.w19);
		mWidgetWeatherIcon.put("晴", R.drawable.w0);
		mWidgetWeatherIcon.put("沙尘暴", R.drawable.w20);

		mWidgetWeatherIcon.put("特大暴雨", R.drawable.w10);
		mWidgetWeatherIcon.put("雾", R.drawable.w18);
		mWidgetWeatherIcon.put("小雪", R.drawable.w14);
		mWidgetWeatherIcon.put("小雨", R.drawable.w7);
		mWidgetWeatherIcon.put("阴", R.drawable.w2);

		mWidgetWeatherIcon.put("雨夹雪", R.drawable.w6);
		mWidgetWeatherIcon.put("阵雪", R.drawable.w13);
		mWidgetWeatherIcon.put("阵雨", R.drawable.w3);
		mWidgetWeatherIcon.put("中雪", R.drawable.w15);
		mWidgetWeatherIcon.put("中雨", R.drawable.w8);
		return mWidgetWeatherIcon;
	};
	/*
//	 * 初始化CityList,通过一个线程
	 */
	private void initCityList() {
	
		new Thread(new Runnable() {

			@Override
			public void run() {
				Application.this.prepareCityList();
			}
		}).start();
	};
	/**
	 * 被initCityList的线程调用,不断的从数据库里面获得城市信息.
	 * @return
	 */
	private boolean prepareCityList() {
		
//		//选择City
//		private List<City> mCityList;
//		// 首字母集
//		private List<String> mSections;		
		mCityList = new ArrayList<City>();
		mSections = new ArrayList<String>();
		mMap = new HashMap<String, List<City>>();
		mPositions = new ArrayList<Integer>();
		mIndexer = new HashMap<String, Integer>();
		
		// 获取数据库中所有城市
		mCityList = mCityDB.getAllCity();
//		取出每一个city对象
		for (City city : mCityList) {
			// 第一个字拼音的第一个字母,统一为大写.
			String firstName = city.getPy().substring(0, 1).toUpperCase();
			
			if (firstName.matches(FORMAT)) {
//				// 根据首字母存放数据
//				private Map<String, List<City>> mMap;				
				if (mSections.contains(firstName)) {
//					通过首字母存放city信息
					mMap.get(firstName).add(city);
				} else {
//					首字母集加上此不存在的首字母
					mSections.add(firstName);
					List<City> list = new ArrayList<City>();
					list.add(city);
					mMap.put(firstName, list);
				}
//				如果首字母不是字母,统一放到"#"中
			} else {
				if (mSections.contains("#")) {
					mMap.get("#").add(city);
				} else {
					mSections.add("#");
					List<City> list = new ArrayList<City>();
					list.add(city);
					mMap.put("#", list);
				}
			}
		};
		
		// 按照字母对首字母集进行重新排序
		Collections.sort(mSections);
		
//		// 首字母位置集
//		private List<Integer> mPositions;
//		// 首字母对应的位置
//		private Map<String, Integer> mIndexer;
		
		int position = 0;
		for (int i = 0; i < mSections.size(); i++) {
			// 存入map中，key为首字母字符串，value为首字母在listview中位置
			mIndexer.put(mSections.get(i), position);
			mPositions.add(position);// 首字母在listview中位置，存入list中
			// 计算下一个首字母在listview的位置
			position += mMap.get(mSections.get(i)).size();
		}
		return true;
	};
	
	/*
	 * 显示预警通知
	 */
	public void showNotification() {
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = mAllWeather.getYujing();
		long when = System.currentTimeMillis();
		
		Notification mNotification = new Notification(icon, tickerText, when);

		mNotification.defaults |= Notification.DEFAULT_SOUND;
		mNotification.contentView = null;

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		// 指定内容意图
		mNotification.setLatestEventInfo(mApplication, "简洁天气预警", tickerText,
				contentIntent);

		mNotificationManager.notify(0x001, mNotification);
	};
};
