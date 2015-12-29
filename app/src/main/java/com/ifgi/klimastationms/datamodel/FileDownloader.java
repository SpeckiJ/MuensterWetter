package com.ifgi.klimastationms.datamodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

@SuppressLint("SimpleDateFormat")
public class FileDownloader implements WeatherDataProvider
{
	private enum DataFile
	{
		TIMESTAMP("Zeit", "http://www.uni-muenster.de/Klima/data/time_de.txt"),
		TEMPERATURE("Temperatur", "http://www.uni-muenster.de/Klima/data/0001tdhg_de.txt"),
		HUMIDITY("Relative Luftfeuchte", "http://www.uni-muenster.de/Klima/data/0002rhhg_de.txt"),
		PRESSURE("Luftdruck", "http://www.uni-muenster.de/Klima/data/0005aphg_de.txt"),
		WINDSPEED("Windgeschwindigkeit", "http://www.uni-muenster.de/Klima/data/0003wshg_de.txt"),
		WINDSPEED_BEAUFORT("Windgeschwindigkeit (Beaufort)", "http://www.uni-muenster.de/Klima/data/0003wshg_de_bft.txt"),
		WINDSPEED_MAXIMUM("Windgeschwindigkeit (max)", "http://www.uni-muenster.de/Klima/data/0016wshgmx_de.txt"),
		DIRECTION("Windrichtung", "http://www.uni-muenster.de/Klima/data/0004wdhg_de.txt"),
		RADIATION("Kurzwellige Einstrahlung", "http://www.uni-muenster.de/Klima/data/0014sihg_de_html.txt"),
		VISIBILITY("Sichtweite", "http://www.uni-muenster.de/Klima/data/0006vihg_de_html.txt"),
		WEATHERCODE("Wettercode", "http://www.uni-muenster.de/Klima/data/0007cdhg_de.txt"),
		WEATHERCODE_DESCRIPTION("Wettercode (Beschreibung)", "http://www.uni-muenster.de/Klima/data/0007cdhg_de_txt.txt"),
		CLOUD_AMOUNT("Bedeckungsgrad", "http://www.uni-muenster.de/Klima/data/0017behg_de.txt"),
		CLOUD_HEIGHT("Wolkenhöhe", "http://www.uni-muenster.de/Klima/data/0017whhg_de.txt");

		private final String title;
		private final String downloadURL;

		DataFile(String title, String downloadURL)
		{
			this.title = title;
			this.downloadURL = downloadURL;
		}

		public String getDownloadURL()
		{
			return this.downloadURL;
		}
	}

	private enum FileDownloadTimespan
	{
		TimespanNone,
		Timespan24h,
		Timespan5d,
		Timespan20d
	}
	
	private static final int TIMESTAMP = 0;
	private static final int TEMPERATURE = 2;
	private static final int RADIATION = 3;
	private static final int WIND_SPEED = 4;
	private static final int WIND_DIRECTION = 5;
	private static final int WIND_SPEED_MAXIMUM = 7;
	private static final int HUMIDITY = 8;
	private static final int PRESSURE = 9;
	private static final String TIMESTAMP_LAST_DOWNLOAD = "timestampLastDownload";
	private static final String PREFERENCES_FILENAME = "com.klimastation.app";
	private static final String FILENAME_24H = "http://www.uni-muenster.de/Klima/data/CR3000_Data24h.dat";
	private static final String FILENAME_5D = "http://www.uni-muenster.de/Klima/data/CR3000_Data5d.dat";
	private static final String FILENAME_20D = "http://www.uni-muenster.de/Klima/data/CR3000_Data20d.dat";
	private static final String FILENAME_POTENTIAL_RADIATION_24h = "http://www.uni-muenster.de/Klima/data/PotRad_24h.txt";
	private static final String FILENAME_POTENTIAL_RADIATION_5d = "http://www.uni-muenster.de/Klima/data/PotRad_05d.txt";
	private static final String FILENAME_POTENTIAL_RADIATION_20d = "http://www.uni-muenster.de/Klima/data/PotRad_20d.txt";

	CurrentDataCompletionHandler currentDataCompletionHandler;
	ArchiveDataCompletionHandler archiveDataCompletionHandler;

	ArchiveDataDownloadTask archiveDownloadTask;
	CurrentDataDownloadTask currentDataDownloadTask;

	WeatherData cachedWeatherData;

	private Context context;

	public FileDownloader(Context context)
	{
		this.context = context;
	}

	@Override
	public void loadCurrentWeatherData(CurrentDataCompletionHandler completionHandler)
	{
		this.currentDataCompletionHandler = completionHandler;
		this.currentDataDownloadTask = new CurrentDataDownloadTask();
		this.currentDataDownloadTask.execute();
	}

	@Override
	public void loadArchiveWeatherData(ArchiveDataCompletionHandler completionHandler)
	{
		this.archiveDataCompletionHandler = completionHandler;

		SharedPreferences sharedPreferences = this.context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);

		long lastDownloadTime = sharedPreferences.getLong(TIMESTAMP_LAST_DOWNLOAD, 0);
		long differenceToNow = new Date().getTime() - lastDownloadTime;
		long tenMinutes = 1000 * 60 * 10;
		long twentyFourHours = 1000 * 60 * 60 * 24;
		long fiveDays = twentyFourHours * 5;

		FileDownloadTimespan fileDownloadTimespan;
		if (differenceToNow < tenMinutes)
		{
			fileDownloadTimespan = FileDownloadTimespan.TimespanNone;
		}
		else if (differenceToNow < twentyFourHours)
		{
			fileDownloadTimespan = FileDownloadTimespan.Timespan24h;
		}
		else if (differenceToNow >= twentyFourHours && differenceToNow < fiveDays)
		{
			fileDownloadTimespan = FileDownloadTimespan.Timespan5d;
		}
		else
		{
			fileDownloadTimespan = FileDownloadTimespan.Timespan20d;
		}

		this.archiveDownloadTask = new ArchiveDataDownloadTask();
		this.archiveDownloadTask.execute(fileDownloadTimespan);
	}

	private class ArchiveDataDownloadTask extends AsyncTask<FileDownloadTimespan, WeatherData, ArrayList<WeatherData>>
	{
		@Override
		protected ArrayList<WeatherData> doInBackground(FileDownloadTimespan... params)
		{
			if (params[0] == FileDownloadTimespan.TimespanNone)
			{
				return null;
			}
				
			ArrayList<WeatherData> weatherDataArray = new ArrayList<WeatherData>();

			try
			{
				FileDownloadTimespan fileDownloadTimespan = params[0];
				
				URL urlMainFile;
				URL urlPotentialRadiationFile;
				
				switch (fileDownloadTimespan)
				{
					case Timespan24h:
						urlMainFile = new URL(FILENAME_24H);
						urlPotentialRadiationFile = new URL(FILENAME_POTENTIAL_RADIATION_24h);
						break;
					case Timespan5d:
						urlMainFile = new URL(FILENAME_5D);
						urlPotentialRadiationFile = new URL(FILENAME_POTENTIAL_RADIATION_5d);
						break;
					case Timespan20d:
						urlMainFile = new URL(FILENAME_20D);
						urlPotentialRadiationFile = new URL(FILENAME_POTENTIAL_RADIATION_20d);
						break;
					default:
						urlMainFile = new URL("");
						urlPotentialRadiationFile = new URL("");
						break;
				}

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlMainFile.openStream(), "UTF-8"));
				String inputLine;

				while ((inputLine = bufferedReader.readLine()) != null)
				{
					WeatherData weatherData = this.getWeatherDataFromInputString(inputLine);

					if (weatherData != null)
					{
						publishProgress(weatherData);
						weatherDataArray.add(weatherData);
					}
				}

				bufferedReader.close();
				
				bufferedReader = new BufferedReader(new InputStreamReader(urlPotentialRadiationFile.openStream(), "UTF-8"));
				while ((inputLine = bufferedReader.readLine()) != null)
				{
					String[] lines = inputLine.split(",");

					for (int i = 0; i < lines.length; i++)
					{
						WeatherData weatherData = weatherDataArray.get(i);
						weatherData.setPotentialRadiation(Double.parseDouble(lines[i].replace(",", ".")));
					}
				}
				
			}
			catch (Exception e)
			{
				
			}

			return weatherDataArray;
		}

		@Override
		protected void onProgressUpdate(WeatherData... values)
		{
			FileDownloader.this.archiveDataCompletionHandler.progressededDownloadingArchiveWeatherData(values[0]);
		}

		@Override
		protected void onPostExecute(ArrayList<WeatherData> weatherDataArray)
		{
			SharedPreferences.Editor editor = FileDownloader.this.context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE).edit();
			editor.putLong(TIMESTAMP_LAST_DOWNLOAD, new Date().getTime());
			editor.commit();

			Log.w("Done!", "Done downloading data!");

			FileDownloader.this.archiveDataCompletionHandler.finishedDownloadingArchiveWeatherData(weatherDataArray);
		}

		private WeatherData getWeatherDataFromInputString(String string)
		{
			WeatherData weatherData = new WeatherData();

			try
			{
				String[] components = string.split(",");

				String dateString = components[TIMESTAMP].replace("\"", "");
				weatherData.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString));
				weatherData.setTemperature(Double.parseDouble(components[TEMPERATURE].replace(",", ".")));
				weatherData.setHumidity(Double.parseDouble(components[HUMIDITY].replace(",", ".")));
				weatherData.setAtmosphericPressure(Double.parseDouble(components[PRESSURE].replace(",", ".")));
				weatherData.setWindSpeed(Double.parseDouble(components[WIND_SPEED].replace(",", ".")));
				weatherData.setMaxWindSpeed(Double.parseDouble(components[WIND_SPEED_MAXIMUM].replace(",", ".")));
				weatherData.setWindDirection(Double.parseDouble(components[WIND_DIRECTION].replace(",", ".")));
				weatherData.setRadiation(Double.parseDouble(components[RADIATION].replace(",", ".")));
			}
			catch (ParseException parseException)
			{
				Log.w(FileDownloader.class.getName(), "Error parsing line: " + string + " | " + parseException.toString());
				return null;
			}
			catch (Exception e)
			{
				Log.w(FileDownloader.class.getName(), "Error parsing line: " + string + " | " + e.toString());
				return null;
			}

			return weatherData;
		}
	}

	private class CurrentDataDownloadTask extends AsyncTask<String, WeatherData, WeatherData>
	{
		@Override
		protected WeatherData doInBackground(String... params)
		{
			WeatherData currentWeatherData = new WeatherData();

			try
			{
				currentWeatherData.setTemperature(this.getDoubleFromWeatherDataFile(DataFile.TEMPERATURE));
				currentWeatherData.setTimestamp(this.getDateFromWeatherDataFile(DataFile.TIMESTAMP));
				publishProgress(currentWeatherData);
				currentWeatherData.setHumidity(this.getDoubleFromWeatherDataFile(DataFile.HUMIDITY));
				publishProgress(currentWeatherData);
				currentWeatherData.setAtmosphericPressure(this.getDoubleFromWeatherDataFile(DataFile.PRESSURE));
				publishProgress(currentWeatherData);
				currentWeatherData.setWindSpeed(this.getDoubleFromWeatherDataFile(DataFile.WINDSPEED));
				publishProgress(currentWeatherData);
				currentWeatherData.setMaxWindSpeed(this.getDoubleFromWeatherDataFile(DataFile.WINDSPEED_MAXIMUM));
				publishProgress(currentWeatherData);
				currentWeatherData.setWindDirection(this.getDoubleFromWeatherDataFile(DataFile.DIRECTION));
				publishProgress(currentWeatherData);
				currentWeatherData.setRadiation(this.getDoubleFromWeatherDataFile(DataFile.RADIATION));
				publishProgress(currentWeatherData);
				currentWeatherData.setVisibility(this.getDoubleFromWeatherDataFile(DataFile.VISIBILITY));
				publishProgress(currentWeatherData);
				currentWeatherData.setWeatherCode(this.getDoubleFromWeatherDataFile(DataFile.WEATHERCODE));
				currentWeatherData.setWeatherCodeDescription(this.getStringFromWeatherDataFile(DataFile.WEATHERCODE_DESCRIPTION));
				publishProgress(currentWeatherData);
				currentWeatherData.setCloudAmount(this.getStringFromWeatherDataFile(DataFile.CLOUD_AMOUNT));
				publishProgress(currentWeatherData);
				currentWeatherData.setCloudHeight(this.getStringFromWeatherDataFile(DataFile.CLOUD_HEIGHT));
				publishProgress(currentWeatherData);
			}
			catch (ParseException parseException)
			{
				Log.w(FileDownloader.class.getName(), "Error: " + parseException.toString());

				return null;
			}
			catch (Exception e)
			{
				Log.w(FileDownloader.class.getName(), "Error: " + e.toString());

				return null;
			}

			return currentWeatherData;
		}

		@Override
		protected void onProgressUpdate(WeatherData... values)
		{
			FileDownloader.this.cachedWeatherData = values[0];
			FileDownloader.this.currentDataCompletionHandler.progressedDownloadingCurrentWeatherData(values[0]);
		}

		@Override
		protected void onPostExecute(WeatherData weatherData)
		{
			FileDownloader.this.currentDataCompletionHandler.finishedDownloadingCurrentWeatherData(weatherData);
		}

		private double getDoubleFromWeatherDataFile(DataFile dataFile) throws IOException
		{
			try{
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(dataFile.getDownloadURL()).openStream(), "UTF-8"));
				String inputString = bufferedReader.readLine().replaceAll("[^\\d.,-]", "").replace(",", ".");
				return Double.parseDouble(inputString == "" ? "0" : inputString);
			} catch(Exception e) {
			}
			return Double.NaN;

		}

		private Date getDateFromWeatherDataFile(DataFile dataFile) throws IOException, ParseException
		{
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(dataFile.getDownloadURL()).openStream(), "UTF-8"));
				String inputString = bufferedReader.readLine();
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+1")); // timestamps returned from the server are always GMT+1/MEZ
				return sdf.parse(inputString);
			} catch(Exception e) {
			}
			return null;
		}

		private String getStringFromWeatherDataFile(DataFile dataFile) throws IOException
		{
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(dataFile.getDownloadURL()).openStream(), "UTF-8"));
				String inputString = bufferedReader.readLine();
				return inputString;
			} catch(Exception e) {
			}
			return null;
		}

	}

	@Override
	public WeatherData getCachedWeatherData()
	{
		return this.cachedWeatherData;
	}

	@Override
	public Status getCurrentDataDownloadStatus()
	{
		return this.currentDataDownloadTask.getStatus();
	}
}
