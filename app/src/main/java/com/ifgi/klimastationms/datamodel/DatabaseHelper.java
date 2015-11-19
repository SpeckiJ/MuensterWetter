package com.ifgi.klimastationms.datamodel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ifgi.klimastationms.fragments.GraphFragment.GraphPhenomenon;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final String TABLE_WEATHER_DATA = "weatherData";

	private static final String COLUMN_HUMIDITY = "humidity";
	private static final String COLUMN_WINDSPEED = "windspeed";
	private static final String COLUMN_MAX_WINDSPEED = "windspeed_max";
	private static final String COLUMN_WIND_DIRECTION = "wind_direction";
	private static final String COLUMN_RADIATION = "radiation";
	private static final String COLUMN_POTENTIAL_RADIATION = "potential_radiation";
	private static final String COLUMN_ATMOSPHERIC_PRESSURE = "air_pressure";
	private static final String COLUMN_TEMPERATURE = "temperature";
	private static final String COLUMN_TIMESTAMP = "timestamp";

	private static final String DATABASE_CREATE_WEATHER_DATA = "create table " + TABLE_WEATHER_DATA + " (" + COLUMN_TIMESTAMP + " INTEGER primary key not null, " + COLUMN_TEMPERATURE + " FLOAT, " + COLUMN_HUMIDITY + " FLOAT, " + COLUMN_WINDSPEED + " FLOAT, " + COLUMN_MAX_WINDSPEED + " FLOAT, " + COLUMN_WIND_DIRECTION + " FLOAT, " + COLUMN_RADIATION + " FLOAT, " + COLUMN_POTENTIAL_RADIATION + " FLOAT, " + COLUMN_ATMOSPHERIC_PRESSURE + " FLOAT " + ");";
	private static final String DATABASE_FETCH_LATEST_WEATHERDATA = "SELECT * FROM " + TABLE_WEATHER_DATA + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 1";
	private static final String DATABASE_FETCH_WEATHERDATA = "SELECT * FROM " + TABLE_WEATHER_DATA + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT ?";
	private static final String PURGE_WHERE_CLAUSE = COLUMN_TIMESTAMP + "< ?";

	private static final String DATABASE_NAME = "weatherdata.db";
	private static final int DATABASE_VERSION = 2;

	public static final String HASHMAP_KEY_VALUES = "values";
	public static final String HASHMAP_KEY_ADDITIONAL_VALUES = "additionalValues";
	public static final String HASHMAP_KEY_TIME_VALUES = "timeValues";
	
	private static final int DAYS_TO_PURGE = 20;
	private static DatabaseHelper singletonInstance;
	/**
	 * Gets the singleton instance of the database helper. Singleton is needed
	 * for database access to prevent locking.
	 */
	public static DatabaseHelper getInstance(Context context)
	{
		if (singletonInstance == null)
		{
			singletonInstance = new DatabaseHelper(context.getApplicationContext());
		}

		return singletonInstance;
	}

	/**
	 * Private constructor, instances must be retrieved via the getInstance
	 * method to ensure that the singleton instance is used
	 */
	private DatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database)
	{
		try
		{
			database.execSQL(DATABASE_CREATE_WEATHER_DATA);
		}
		catch (SQLException e)
		{
			Log.e("SQLError", e.getLocalizedMessage());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_WEATHER_DATA);
		onCreate(database);
	}

	public HashMap<String, double[]> fetchWeatherDataSinceDate(int limit, GraphPhenomenon graphPhenomenon)
	{
		HashMap<String, double[]> resultHashMap = new HashMap<String, double[]>();
		
		SQLiteDatabase database = this.getReadableDatabase();
		
		String columnName = this.columnNameFromGraphPhenomenon(graphPhenomenon);
		
		String[] selectionArguments =
		{
			String.valueOf(limit)
		};
		
		Cursor cursor = database.rawQuery(DATABASE_FETCH_WEATHERDATA, selectionArguments);

		double[] values = new double[cursor.getCount()];
		double[] timeValues = new double[cursor.getCount()];
		int index = values.length - 1;
		while (cursor.moveToNext())
		{
			timeValues[index] = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP));
			values[index--] = cursor.getDouble(cursor.getColumnIndex(columnName));
		}

		resultHashMap.put(HASHMAP_KEY_VALUES, values);
		resultHashMap.put(HASHMAP_KEY_TIME_VALUES, timeValues);
		
		if (graphPhenomenon == GraphPhenomenon.WIND_SPEED || graphPhenomenon == GraphPhenomenon.RADIATION)
		{		
			columnName = (graphPhenomenon == GraphPhenomenon.WIND_SPEED) ? COLUMN_MAX_WINDSPEED : COLUMN_POTENTIAL_RADIATION;

			cursor.moveToFirst();
			
			double[] additionalValues = new double[cursor.getCount()];
			index = additionalValues.length - 1;
			while (cursor.moveToNext())
			{
				double value = cursor.getDouble(cursor.getColumnIndex(columnName));
				additionalValues[index--] = value;
			}
			
			resultHashMap.put(HASHMAP_KEY_ADDITIONAL_VALUES, additionalValues);
		}

		cursor.close();
		
		
		return resultHashMap;
	}

	public WeatherData fetchLatestWeatherData() throws SQLException
	{
		SQLiteDatabase database = this.getReadableDatabase();
		String sqlStatement = DATABASE_FETCH_LATEST_WEATHERDATA;
		Cursor cursor = database.rawQuery(sqlStatement, null);

		WeatherData weatherData = null;

		while (cursor.moveToNext())
		{
			weatherData = this.weatherDataFromCursor(cursor);
		}

		cursor.close();

		return weatherData;
	}

	public void insertWeatherData(List<WeatherData> weatherDataArray)
	{
		if (weatherDataArray == null)
		{
			return;
		}

		SQLiteDatabase database = this.getReadableDatabase();
		database.beginTransaction();

		ContentValues values;

		long returnCode = 0;

		for (com.ifgi.klimastationms.datamodel.WeatherData weatherdata : weatherDataArray)
		{
			try
			{
				values = this.contentValuesFromWeatherData(weatherdata);
				returnCode = database.insertOrThrow(TABLE_WEATHER_DATA, null, values);
			}
			catch (SQLException exception)
			{
				Log.e("SQLException", "ReturnCode: " + returnCode + " | " + exception.getLocalizedMessage());
			}
			finally
			{
				
			}
		}

		if (returnCode != -1)
		{
			database.setTransactionSuccessful();
		}

		database.endTransaction();
	}

	public void purgeDatabase()
	{
		long twentyDaysInMilliseconds = DAYS_TO_PURGE * 24 * 60 * 60 * 1000;
		long purgeTimeStamp = new Date().getTime() - twentyDaysInMilliseconds;

		String[] arguments =
		{
			String.valueOf(purgeTimeStamp)
		};
		SQLiteDatabase database = this.getReadableDatabase();
		
		if (database.isOpen())
		{
			database.delete(TABLE_WEATHER_DATA, PURGE_WHERE_CLAUSE, arguments);			
		}

	}

	private WeatherData weatherDataFromCursor(Cursor cursor)
	{
		WeatherData weatherData = new WeatherData();

		weatherData.setTimestamp(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))));
		weatherData.setTemperature(cursor.getDouble(cursor.getColumnIndex(COLUMN_TEMPERATURE)));
		weatherData.setHumidity(cursor.getDouble(cursor.getColumnIndex(COLUMN_HUMIDITY)));
		weatherData.setAtmosphericPressure(cursor.getDouble(cursor.getColumnIndex(COLUMN_ATMOSPHERIC_PRESSURE)));
		weatherData.setWindSpeed(cursor.getDouble(cursor.getColumnIndex(COLUMN_WINDSPEED)));
		weatherData.setMaxWindSpeed(cursor.getDouble(cursor.getColumnIndex(COLUMN_MAX_WINDSPEED)));
		weatherData.setWindDirection(cursor.getDouble(cursor.getColumnIndex(COLUMN_WIND_DIRECTION)));
		weatherData.setRadiation(cursor.getDouble(cursor.getColumnIndex(COLUMN_RADIATION)));
		weatherData.setPotentialRadiation(cursor.getDouble(cursor.getColumnIndex(COLUMN_POTENTIAL_RADIATION)));

		return weatherData;
	}

	private ContentValues contentValuesFromWeatherData(WeatherData weatherData)
	{
		if (weatherData == null)
		{
			return null;
		}

		ContentValues values = new ContentValues();

		values.put(COLUMN_TIMESTAMP, weatherData.getTimestamp().getTime());
		values.put(COLUMN_TEMPERATURE, weatherData.getTemperature());
		values.put(COLUMN_HUMIDITY, weatherData.getHumidity());
		values.put(COLUMN_ATMOSPHERIC_PRESSURE, weatherData.getAtmosphericPressure());
		values.put(COLUMN_WINDSPEED, weatherData.getWindSpeed());
		values.put(COLUMN_MAX_WINDSPEED, weatherData.getMaxWindSpeed());
		values.put(COLUMN_WIND_DIRECTION, weatherData.getWindDirection());
		values.put(COLUMN_RADIATION, weatherData.getRadiation());
		values.put(COLUMN_POTENTIAL_RADIATION, weatherData.getPotentialRadiation());

		return values;
	}

	private String columnNameFromGraphPhenomenon(GraphPhenomenon graphPhenomenon)
	{
		String columnName = "";

		if (graphPhenomenon == GraphPhenomenon.ATMOSPHERIC_PRESSURE)
		{
			columnName = COLUMN_ATMOSPHERIC_PRESSURE;
		}
		else if (graphPhenomenon == GraphPhenomenon.HUMIDITY)
		{
			columnName = COLUMN_HUMIDITY;
		}
		else if (graphPhenomenon == GraphPhenomenon.WIND_SPEED)
		{
			columnName = COLUMN_WINDSPEED;
		}
		else if (graphPhenomenon == GraphPhenomenon.WIND_DIRECTION)
		{
			columnName = COLUMN_WIND_DIRECTION;
		}
		else if (graphPhenomenon == GraphPhenomenon.RADIATION)
		{
			columnName = COLUMN_RADIATION;
		}
		else if (graphPhenomenon == GraphPhenomenon.TEMPERATURE)
		{
			columnName = COLUMN_TEMPERATURE;
		}

		return columnName;
	}
}
