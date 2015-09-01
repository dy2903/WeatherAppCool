package com.dy.activity;

import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dy.adapter.CityAdapter;
import com.dy.adapter.SearchCityAdapter;
import com.dy.app.Application;
import com.dy.bean.City;
import com.dy.plistview.BladeView;
import com.dy.plistview.PinnedHeaderListView;
import com.dy.plistview.BladeView.OnItemClickListener;
import com.dy.receiver.NetBroadcastReceiver;
import com.dy.receiver.NetBroadcastReceiver.EventHandler;
import com.dy.util.LogUtil;
import com.way.weather.R;

public class SelectCityActivity extends SwipeBackActivity implements TextWatcher,
		OnClickListener, EventHandler {
	private EditText mSearchEditText;
	private ImageButton mClearSearchBtn;
	private View mCityContainer;
	private View mSearchContainer;
	/*
	 * ListView
	 */
	private PinnedHeaderListView mCityPinnedListView;
	private BladeView letterBladeView;
	private ListView mSearchListView;
	private SearchCityAdapter mSearchCityAdapter;
	private CityAdapter mCityAdapter;
	
	/**
	 * City数据
	 */
	private List<City> cityList;
	// 首字母集
	private List<String> initStrList;
	// 根据首字母存放数据
	private Map<String, List<City>> initCitiesMap;
	// 首字母位置集
	private List<Integer> posInLv;
	// 首字母对应的位置
	private Map<String, Integer> initToPosMap;
	private Application mApplication;
	private InputMethodManager mInputMethodManager;

	private TextView mTitleTextView;
	private ImageView mBackBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.biz_plugin_weather_select_city);
		NetBroadcastReceiver.mListeners.add(this);
		this.initView();
		this.initData();

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void initView() {
		/*
		 * 顶部
		 */
		mTitleTextView = (TextView) findViewById(R.id.title_name);
		mBackBtn = (ImageView) findViewById(R.id.title_back);
		mBackBtn.setOnClickListener(this);
		mTitleTextView
			.setText(Application.getInstance()
			.getSharePreferenceUtil().getCity());
		/*
		 * 搜索框
		 */
		mSearchEditText = (EditText) findViewById(R.id.search_edit);
		mSearchEditText.addTextChangedListener(this);
		mClearSearchBtn = (ImageButton) findViewById(R.id.ib_clear_text);
		mClearSearchBtn.setOnClickListener(this);
		/*
		 * 两个container包含了ListView
		 * 在不同的时刻显示
		 */
		mCityContainer = findViewById(R.id.city_content_container);
		mSearchContainer = findViewById(R.id.search_content_container);
		/*
		 * PinnedHeaderListView
		 */
		mCityPinnedListView = (PinnedHeaderListView) findViewById(R.id.citys_list);
		mCityPinnedListView.setEmptyView(findViewById(R.id.citys_list_empty));
		mCityPinnedListView
		.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				LogUtil.i(mCityAdapter.getItem(position).toString());
//				给MainActivity返回City
				SelectCityActivity.this.startActivity(mCityAdapter.getItem(position));
			}
		});
		/*
		 * BladeView
		 */
		letterBladeView = (BladeView) findViewById(R.id.citys_bladeview);
		letterBladeView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(String s) {
				if (initToPosMap.get(s) != null) {
					mCityPinnedListView.setSelection(initToPosMap.get(s));
				}
			}
		});
		letterBladeView.setVisibility(View.GONE);
		
		/*
		 * 搜索ListView
		 */
		mSearchListView = (ListView) findViewById(R.id.search_list);
		mSearchListView.setEmptyView(findViewById(R.id.search_empty));
		mSearchContainer.setVisibility(View.GONE);
		mSearchListView.setOnTouchListener(new OnTouchListener() {
//			关闭软键盘
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mInputMethodManager.hideSoftInputFromWindow(
						mSearchEditText.getWindowToken(), 0);
				return false;
			}
		});		

		mSearchListView
				.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						LogUtil.i(mSearchCityAdapter.getItem(position).toString());
						startActivity(mSearchCityAdapter.getItem(position));
					}
				});
		
		
	};
	

	private void startActivity(City city) {
		Intent i = new Intent();
		i.putExtra("city", city);
		setResult(RESULT_OK, i);
		finish();
	};

	private void initData() {
		mApplication = Application.getInstance();
		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		cityList = mApplication.getCityList();
		initStrList = mApplication.getSections();
		initCitiesMap = mApplication.getMap();
		posInLv = mApplication.getPositions();
		initToPosMap = mApplication.getIndexer();
		/*
		 * PinnedHeaderListView
		 */
		mCityAdapter = new CityAdapter(SelectCityActivity.this, cityList, initCitiesMap,
				initStrList, posInLv);		
		mCityPinnedListView.setAdapter(mCityAdapter);
		mCityPinnedListView.setOnScrollListener(mCityAdapter);
		mCityPinnedListView
				.setPinnedHeaderView(
				LayoutInflater.from(SelectCityActivity.this)
				.inflate(R.layout.biz_plugin_weather_list_group_item, mCityPinnedListView,
				false));
		
		letterBladeView.setVisibility(View.VISIBLE);

	};
//	=====================TextWatcher =================
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// do nothing
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
//		搜索框中内容改变的时候才进行初始化
		mSearchCityAdapter = new SearchCityAdapter(SelectCityActivity.this,
				cityList);
		mSearchListView.setAdapter(mSearchCityAdapter);
		mSearchListView.setTextFilterEnabled(true);
		/*
		 * 没有内容,不显示
		 */
		
		if (cityList.size() < 1 || TextUtils.isEmpty(s)) {
			mCityContainer.setVisibility(View.VISIBLE);
			mSearchContainer.setVisibility(View.INVISIBLE);
			mClearSearchBtn.setVisibility(View.GONE);
			/*
			 * 开始输入
			 */
		} else {
			mClearSearchBtn.setVisibility(View.VISIBLE);
//			pinnedListView , searchListView交替显示
			mCityContainer.setVisibility(View.INVISIBLE);
			mSearchContainer.setVisibility(View.VISIBLE);
//			开始滤除
			mSearchCityAdapter.getFilter().filter(s);
		}
	};

	@Override
	public void afterTextChanged(Editable s) {
		// 如何搜索字符串长度为0，是否隐藏输入法
		// if(TextUtils.isEmpty(s)){
		// mInputMethodManager.hideSoftInputFromWindow(
		// mSearchEditText.getWindowToken(), 0);
		// }

	}
//	=====================onClick =================
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ib_clear_text:
			if (!TextUtils.isEmpty(mSearchEditText.getText().toString())) {
				mSearchEditText.setText("");
				mInputMethodManager.hideSoftInputFromWindow(
						mSearchEditText.getWindowToken(), 0);
			}
			break;
		case R.id.title_back:
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		NetBroadcastReceiver.mListeners.remove(this);
	}

	@Override
	public void onNetChange() {
//		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
//			T.showLong(this, R.string.net_err);
	}
}
