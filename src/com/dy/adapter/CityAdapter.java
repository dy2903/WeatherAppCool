package com.dy.adapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.dy.bean.City;
import com.dy.plistview.PinnedHeaderListView;
import com.dy.plistview.PinnedHeaderListView.PinnedHeaderAdapter;
import com.way.weather.R;
/**
 * 实现了PinnedHeaderAdapter
	OnScrollListender
	SectionIndexer:控制分组
 * @author DX2903
 *
 */

public class CityAdapter extends BaseAdapter implements SectionIndexer,
		PinnedHeaderAdapter, OnScrollListener {
	// 首字母集
	private List<City> mCities;
	private Map<String, List<City>> mMap;
	private List<String> mSections;
	private List<Integer> mPositions;
	private LayoutInflater inflater;

	public CityAdapter(Context context, List<City> cities,
			Map<String, List<City>> map, List<String> sections,
			List<Integer> positions) {
		// TODO Auto-generated constructor stub
		inflater = LayoutInflater.from(context);
		mCities = cities;
		mMap = map;
		mSections = sections;
		mPositions = positions;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mCities.size();
	}

	@Override
	public City getItem(int position) {
		// TODO Auto-generated method stub
		int section = getSectionForPosition(position);
		return mMap.get(mSections.get(section)).get(
				position - getPositionForSection(section));
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	/*
	 * 配置ListView的Item项显示的内容
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
//		现在处于那个组里面
		int section = getSectionForPosition(position);
//		获得当前的View
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.select_city_item, null);
		}
//		显示分组Title的背景
		TextView group = (TextView) convertView.findViewById(R.id.group_title);
//		显示城市
		TextView city = (TextView) convertView.findViewById(R.id.column_title);
		
		if (getPositionForSection(section) == position) {
			group.setVisibility(View.VISIBLE);
			group.setText(mSections.get(section));
		} else {
			group.setVisibility(View.GONE);
		}
//		城市名称显示
		City item = mMap.get(mSections.get(section)).get(
				position - getPositionForSection(section));
		city.setText(item.getName());
		return convertView;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		if (view instanceof PinnedHeaderListView) {
			((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
		}

	}

	/*
	 * 覆写PinnedHeaderAdapter中的方法
	 * @see com.way.plistview.PinnedHeaderListView.PinnedHeaderAdapter#getPinnedHeaderState(int)
	 */
	@Override
	public int getPinnedHeaderState(int position) {
		
		int realPosition = position;
//		返回的值在PinnedHeaderListView的configureHeaderView进行处理
		if (realPosition < 0 || position >= getCount()) {
			return PINNED_HEADER_GONE;
		}
//		如果第一个item不是section，第二个item是section的话，就返回状态PINNED_HEADER_PUSHED_UP，
		int section = getSectionForPosition(realPosition);
		int nextSectionPosition = getPositionForSection(section + 1);
		if (nextSectionPosition != -1
				&& realPosition == nextSectionPosition - 1) {
			return PINNED_HEADER_PUSHED_UP;
		}
//		其他情况返回可见
		return PINNED_HEADER_VISIBLE;
	}

	/*
	 * 如设置Header View的文本，图片等，这个方法是由调用者去实现的。
	 * 将item的section字符串设置到header view上面去。
	 * @see com.way.plistview.PinnedHeaderListView.PinnedHeaderAdapter#configurePinnedHeader(android.view.View, int, int)
	 */
	@Override
	public void configurePinnedHeader(View header, int position, int alpha) {
		// TODO Auto-generated method stub
		int realPosition = position;
//		position处于那一段中
		int section = this.getSectionForPosition(realPosition);
		String title = (String)this.getSections()[section];
//		设置HeaderView显示的值(每个分组的首字母)
		((TextView) header.findViewById(R.id.group_title)).setText(title);
	};

	@Override
	public Object[] getSections() {
		// TODO Auto-generated method stub
//		把List转换Array
		return mSections.toArray();
	}

	@Override
	public int getPositionForSection(int section) {
		// TODO Auto-generated method stub
		if (section < 0 || section >= mPositions.size()) {
			return -1;
		}
		return mPositions.get(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		if (position < 0 || position >= getCount()) {
			return -1;
		}
		int index = Arrays.binarySearch(mPositions.toArray(), position);
		return index >= 0 ? index : -index - 2;
	}
};
