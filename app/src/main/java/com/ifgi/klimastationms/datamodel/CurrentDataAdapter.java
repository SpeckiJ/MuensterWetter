package com.ifgi.klimastationms.datamodel;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.fragments.GraphFragment.GraphPhenomenon;

public class CurrentDataAdapter extends BaseAdapter 
{
	private static final int HUMIDITY = 0;
	private static final int ATMOSPHERIC_PRESSURE = 1;
	private static final int WIND_SPEED = 2;
	private static final int WIND_SPEED_BEAUFORT = 3;
	private static final int MAX_WIND_SPEED = 4;
	private static final int WIND_DIRECTION = 5;
	private static final int RADIATION = 6;
	private static final int VISIBILITY = 7;
	private static final int WEATHER_CODE = 8;

	private WeatherData weatherData;
    private LayoutInflater layoutInflater;
    Context context;
 
    public CurrentDataAdapter(Context context, WeatherData weatherData)
    {
    	if (context == null)
    	{
    		Log.e("", "Context null");
    	}

    	if (weatherData == null)
    	{
    		Log.e("", "weatherData null");
    	}
    	
    	this.context = context;
    	this.weatherData = weatherData;
    	this.layoutInflater = LayoutInflater.from(context);
    }
 
    @Override
    public int getCount()
    {
        return 9;
    }
 
    @Override
    public long getItemId(int position)
    {
        return position;
    }

	@Override
	public Object getItem(int arg0)
	{
		return "";
	}
	
    public View getView(int position, View convertView, ViewGroup parent) 
    { 
        ViewHolder holder;
        if (convertView == null)
        {
            convertView = layoutInflater.inflate(R.layout.current_data_cell, null);
            holder = new ViewHolder();
            holder.textViewPhenomenonDescription = (TextView) convertView.findViewById(R.id.textViewPhenomenonDescription);
            holder.textViewPhenomenonValue = (TextView) convertView.findViewById(R.id.textViewPhenomenonValue);
            holder.textViewPhenomenonUnit = (TextView) convertView.findViewById(R.id.textViewPhenomenonUnit);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        String phenomenonDescription = "";
        String phenomenonValue = "";
        String phenomenonUnit = "";
       
        switch (position)
		{
			case HUMIDITY:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionHumidity);
				phenomenonValue = String.format("%.1f ", this.weatherData.getHumidity());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitHumidity);
				break;
			case ATMOSPHERIC_PRESSURE:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionAtmosphericPressure);
				phenomenonValue = String.format("%.0f ", this.weatherData.getAtmosphericPressure());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitAtmosphericPressure);
				break;
			case WIND_SPEED:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionWindSpeed);
				phenomenonValue = String.format("%.1f ", this.weatherData.getWindSpeed());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitWindSpeed);
				break;
			case WIND_SPEED_BEAUFORT:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionWindSpeedBeaufort);
				phenomenonValue = String.format("%.0f ", this.weatherData.getWindSpeedBeaufort());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitWindSpeedBeaufort);
				break;
			case MAX_WIND_SPEED:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionMaxWindSpeed);
				phenomenonValue = String.format("%.1f ", this.weatherData.getMaxWindSpeed());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitWindSpeed);
				break;
			case WIND_DIRECTION:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionWindDirection);
				phenomenonValue = String.format("%s - %.0f", this.weatherData.getWindDirectionDescription(this.context), this.weatherData.getWindDirection());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitWindDirection);
				break;
			case RADIATION:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionRadiation);
				phenomenonValue = String.format("%.1f ", this.weatherData.getRadiation());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitRadiation);
				break;
			case VISIBILITY:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionVisibilty);
				phenomenonValue = String.format("> %.0f ", this.weatherData.getVisibility());
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitVisibility);
				break;
			case WEATHER_CODE:
				phenomenonDescription = this.context.getString(R.string.phenomenonDescriptionWeathercode);
				phenomenonValue = this.weatherData.getWeatherCodeDescription();
				phenomenonUnit = this.context.getString(R.string.phenomenonUnitWeathercode);
				break;
			default:
				break;
		}
       
        holder.textViewPhenomenonDescription.setText(phenomenonDescription);
        holder.textViewPhenomenonValue.setText(phenomenonValue);
        holder.textViewPhenomenonUnit.setText(phenomenonUnit);
     
        return convertView;
    }
    
    static class ViewHolder 
    {
        TextView textViewPhenomenonDescription;
        TextView textViewPhenomenonValue;
        TextView textViewPhenomenonUnit;
    }
}
