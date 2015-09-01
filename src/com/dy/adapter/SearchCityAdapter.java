/*
 * 查找时筛选的结果
 * 显示城市 --> 省份
 */
package com.dy.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.dy.bean.City;
import com.way.weather.R;

public class SearchCityAdapter extends BaseAdapter implements Filterable {

	private List<City> mAllCities;
	private List<City> mResultCities;
	private LayoutInflater mInflater;
	private Context mContext;

	// private String mFilterStr;

	public SearchCityAdapter(Context context, List<City> allCities) {
		mContext = context;
		mAllCities = allCities;
		mResultCities = new ArrayList<City>();
		mInflater = LayoutInflater.from(mContext);
	}

	@Override
	public int getCount() {
		return mResultCities.size();
	}

	@Override
	public City getItem(int position) {
		return mResultCities.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	/*
	 * ListView的Item显示布局
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.search_city_item, null);
		}
//		城市 -> 省份
		TextView provinceTv = (TextView) convertView
				.findViewById(R.id.search_province);
		provinceTv.setText(mResultCities.get(position).getProvince());
		TextView cityTv = (TextView) convertView
				.findViewById(R.id.column_title);
		cityTv.setText(mResultCities.get(position).getName());
		return convertView;
	}

	
	/*
	 * 获得滤除规则(non-Javadoc)
	 * @see android.widget.Filterable#getFilter()
	 */
	@Override
	public Filter getFilter() {
		
		Filter filter = new Filter() {
			
			@SuppressWarnings("unchecked")
//			过滤的规则
			protected FilterResults performFiltering(CharSequence s) {
				
				String str = s.toString().toLowerCase();
//				遍历所有的City信息,如果有匹配的,保存到List里面,然后设置到FilterResults中中去
				FilterResults results = new FilterResults();
				ArrayList<City> cityList = new ArrayList<City>();
				
				if (mAllCities != null && mAllCities.size() != 0) {
					for (City cb : mAllCities) {
						// 匹配全拼、首字母、和城市名中文
						if (cb.getPy().indexOf(str) > -1
								|| cb.getPinyin().indexOf(str) > -1
								|| cb.getName().indexOf(str) > -1) {
//							将符合要求的city保存到List中
							cityList.add(cb);
						}
					}
				}
				results.values = cityList;
				results.count = cityList.size();
				return results;
			};
			
			/*
			 	保存结果,通知UI进行显示
				把performFiltering中的results传递进来
			 * @see android.widget.Filter#publishResults(java.lang.CharSequence, android.widget.Filter.FilterResults)
			 */
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
//				设置到成员变量中
				mResultCities = (ArrayList<City>) results.values;
				if (results.count > 0) {
					SearchCityAdapter.this.notifyDataSetChanged();
				} else {
					SearchCityAdapter.this.notifyDataSetInvalidated();
				}
			};
			
		};
		
		
		return filter;
	};

}
