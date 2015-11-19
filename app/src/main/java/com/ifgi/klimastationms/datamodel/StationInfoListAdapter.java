package com.ifgi.klimastationms.datamodel;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ifgi.klimastationms.R;

public class StationInfoListAdapter extends BaseAdapter 
{	 
    private ArrayList<HashMap<String, String>> listData;
 
    private LayoutInflater layoutInflater;
 
    public StationInfoListAdapter(Context context, ArrayList<HashMap<String, String>> listData) 
    {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
    }
 
    @Override
    public int getCount() 
    {
        return listData.size();
    }
 
    @Override
    public Object getItem(int position) 
    {
        return listData.get(position);
    }
 
    @Override
    public long getItemId(int position)
    {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.station_info_row, null);
            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.textViewInfoTitle);
            holder.detailTextView = (TextView) convertView.findViewById(R.id.textViewInfoDetail);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
 
        holder.titleTextView.setText(listData.get(position).get("title"));
        holder.detailTextView.setText(listData.get(position).get("detail"));
 
        return convertView;
    }
 
    static class ViewHolder 
    {
        TextView titleTextView;
        TextView detailTextView;
    }
}