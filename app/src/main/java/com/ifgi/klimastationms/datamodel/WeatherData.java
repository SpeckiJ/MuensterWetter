package com.ifgi.klimastationms.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;

import com.ifgi.klimastationms.R;
import com.jjoe64.graphview.GraphView.GraphViewData;

public class WeatherData
{
	private static final double INITIAL_VALUE = 0;

	private Date timestamp;
	private double temperature;
	private double humidity;
	private double atmosphericPressure;
	private double windSpeed;
	private double maxWindSpeed;
	private double windDirection;
	private double radiation;
	private double potentialRadiation;
	private double visibility;
	private double weatherCode;
	private String weatherCodeDescription;
	private String cloudAmount;
	private String cloudHeight;

	public WeatherData()
	{
		super();
		this.temperature = INITIAL_VALUE;
		this.humidity = INITIAL_VALUE;
		this.atmosphericPressure = INITIAL_VALUE;
		this.windSpeed = INITIAL_VALUE;
		this.maxWindSpeed = INITIAL_VALUE;
		this.windDirection = INITIAL_VALUE;
		this.radiation = INITIAL_VALUE;
		this.potentialRadiation = INITIAL_VALUE;
		this.visibility = INITIAL_VALUE;
		this.weatherCode = INITIAL_VALUE;
		this.weatherCodeDescription = "";
		this.cloudHeight = " ";
		this.cloudAmount = " ";
	}

	public String getWeatherCodeDescription()
	{
		return weatherCodeDescription;
	}

	public void setWeatherCodeDescription(String weatherCodeDescription)
	{
		// We need to decode html entities
		this.weatherCodeDescription = Html.fromHtml(weatherCodeDescription).toString();
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public double getWindDirection()
	{
		return windDirection;
	}

	public void setWindDirection(double windDirection)
	{
		this.windDirection = windDirection;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public double getTemperature()
	{
		return temperature;
	}

	public void setTemperature(double temperature)
	{
		this.temperature = temperature;
	}

	public double getHumidity()
	{
		return humidity;
	}

	public void setHumidity(double humidity)
	{
		this.humidity = humidity;
	}

	public double getAtmosphericPressure()
	{
		return atmosphericPressure;
	}

	public void setAtmosphericPressure(double atmosphericPressure)
	{
		this.atmosphericPressure = atmosphericPressure;
	}

	public double getWindSpeed()
	{
		return windSpeed;
	}

	public void setWindSpeed(double windSpeed)
	{
		this.windSpeed = windSpeed;
	}

	public double getWindSpeedBeaufort()
	{
		if (this.windSpeed <= 0.2)
		{
			return 0;	
		}
		else if (this.windSpeed <= 1.5)
		{
			return 1;	
		}
		else if (this.windSpeed <= 3.3)
		{
			return 2;	
		}
		else if (this.windSpeed <= 5.4)
		{
			return 3;	
		}
		else if (this.windSpeed <= 7.9)
		{
			return 4;	
		}
		else if (this.windSpeed <= 10.7)
		{
			return 5;	
		}
		else if (this.windSpeed <= 13.8)
		{
			return 6;	
		}
		else if (this.windSpeed <= 17.1)
		{
			return 7;	
		}
		else if (this.windSpeed <= 20.7)
		{
			return 8;	
		}
		else if (this.windSpeed <= 24.4)
		{
			return 9;	
		}
		else if (this.windSpeed <= 28.4)
		{
			return 10;	
		}
		else if (this.windSpeed <= 32.6)
		{
			return 11;	
		}
		else
		{
			return 12;	
		}
	}

	public double getWindSpeedKmh()
	{
		return this.windSpeed * 36 / 10;
	}

	public double getMaxWindSpeed()
	{
		return maxWindSpeed;
	}

	public void setMaxWindSpeed(double maxWindSpeed)
	{
		this.maxWindSpeed = maxWindSpeed;
	}

	public double getRadiation()
	{
		return radiation;
	}

	public void setRadiation(double radiation)
	{
		this.radiation = radiation;
	}

	public double getPotentialRadiation()
	{
		return potentialRadiation;
	}

	public void setPotentialRadiation(double potentialRadiation)
	{
		this.potentialRadiation = potentialRadiation;
	}

	public double getVisibility()
	{
		return visibility;
	}

	public void setVisibility(double visibility)
	{
		this.visibility = visibility;
	}

	public double getWeatherCode()
	{
		return weatherCode;
	}

	public void setWeatherCode(double weatherCode)
	{
		this.weatherCode = weatherCode;
	}

	public String getWindDirectionDescription(Context context)
	{
		String[] windDirections = context.getResources().getStringArray(R.array.wind_directions);
		int index = (int) (this.getWindDirection() + 22.5) / 45;
		
		if (index < 0 || index >= windDirections.length)
		{
			return "";
		}
		
		return windDirections[index];
	}

	public GraphViewData getGraphViewDataForField(int index, String columnName)
	{				
		return new GraphViewData(1, 1);
	}

	public void setCloudHeight(String cloudHeight){
		this.cloudHeight = cloudHeight;
	}

	public String getCloudHeight() {
		return this.cloudHeight;
	}

	public void setCloudAmount(String cloudAmount){
		this.cloudAmount = cloudAmount;
	}

	public String getCloudAmount() {
		return this.cloudAmount;
	}

}
