package com.ifgi.klimastationms.datamodel;

import java.util.ArrayList;

import android.os.AsyncTask.Status;

public interface WeatherDataProvider
{
	void loadCurrentWeatherData(CurrentDataCompletionHandler completionHandler);
	void loadArchiveWeatherData(ArchiveDataCompletionHandler completionHandler);
	WeatherData getCachedWeatherData();
	Status getCurrentDataDownloadStatus();

	interface CurrentDataCompletionHandler
	{
		void progressedDownloadingCurrentWeatherData(WeatherData weatherData);
		void finishedDownloadingCurrentWeatherData(WeatherData weatherData);
	}

	interface ArchiveDataCompletionHandler
	{
		void progressededDownloadingArchiveWeatherData(WeatherData weatherData);
		void finishedDownloadingArchiveWeatherData(ArrayList<WeatherData> weatherDataArray);
	}
}
 