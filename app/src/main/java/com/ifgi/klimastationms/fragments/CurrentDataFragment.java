package com.ifgi.klimastationms.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.datamodel.CurrentDataAdapter;
import com.ifgi.klimastationms.datamodel.DatabaseHelper;
import com.ifgi.klimastationms.datamodel.FileDownloader;
import com.ifgi.klimastationms.datamodel.WeatherData;
import com.ifgi.klimastationms.datamodel.WeatherDataProvider;
import com.ifgi.klimastationms.datamodel.WeatherDataProvider.CurrentDataCompletionHandler;
import com.ifgi.klimastationms.fragments.GraphFragment.GraphPhenomenon;
import com.ifgi.klimastationms.views.FontableTextView;

public class CurrentDataFragment extends Fragment
{
	public interface CurrentDataFragmentListener
	{
		public void onWebcamButtonPressed(ImageButton webcamButton);

		public void onInfoButtonPressed(ImageButton infoButton);

		public void onListViewItemSelected(GraphPhenomenon listToDatabase);
	}

	private CurrentDataFragmentListener fragmentListener;

	private ImageButton refreshButton;
	private ImageButton webcamButton;
	private ImageButton infoButton;

	private FontableTextView temperatureTextView;
	private FontableTextView timestampTextView;
	private GridView currentDataGridView;

	private WeatherDataProvider dataProvider;
	private DatabaseHelper dbHelper;

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		try
		{
			this.fragmentListener = (CurrentDataFragmentListener) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement CurrentDataFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_current_data, viewGroup, false);
		this.refreshButton = (ImageButton) view.findViewById(R.id.refreshButton);
		this.webcamButton = (ImageButton) view.findViewById(R.id.webcamButton);
		this.infoButton = (ImageButton) view.findViewById(R.id.infoButton);
		this.temperatureTextView = (FontableTextView) view.findViewById(R.id.temperatureTextView);
		this.timestampTextView = (FontableTextView) view.findViewById(R.id.timeStampTextView);
		this.currentDataGridView = (GridView) view.findViewById(R.id.weatherDataGridView);

		this.setupListeners();

		return view;
	}

	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (this.dataProvider != null)
		{
			if (this.dataProvider.getCurrentDataDownloadStatus() == Status.FINISHED)
			{
				this.updateUI(this.dataProvider.getCachedWeatherData());
			}
		}
		else
		{
			this.dbHelper = DatabaseHelper.getInstance(this.getActivity());
			this.dataProvider = new FileDownloader(this.getActivity());
			this.dataProvider.loadCurrentWeatherData(this.getCompletionHandler());
			this.setRefreshButtonRotating(true);
			this.updateUI(null);
		}
	}

	private void setupListeners()
	{
		this.refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				CurrentDataFragment.this.setRefreshButtonRotating(true);
				CurrentDataFragment.this.dataProvider.loadCurrentWeatherData(CurrentDataFragment.this.getCompletionHandler());
			}
		});

		this.infoButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				CurrentDataFragment.this.fragmentListener.onInfoButtonPressed(CurrentDataFragment.this.infoButton);
			}
		});
  
		this.webcamButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				CurrentDataFragment.this.fragmentListener.onWebcamButtonPressed(CurrentDataFragment.this.infoButton);
			}
		});
	}

	private void updateUI(WeatherData weatherData)
	{
		if (weatherData == null)
		{
			weatherData = this.dbHelper.fetchLatestWeatherData();
			
			if (weatherData == null)
			{
				return;
			}
		}

		this.temperatureTextView.setText(String.format(Locale.getDefault(), "%.1f °C", weatherData.getTemperature()));

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		if(weatherData.getTimestamp() != null) {
			dateFormat.setTimeZone(TimeZone.getDefault());;
			this.timestampTextView.setText(dateFormat.format(weatherData.getTimestamp()));
		}

		this.currentDataGridView.setAdapter(new CurrentDataAdapter(this.getActivity(), weatherData));
	}

	private CurrentDataCompletionHandler getCompletionHandler()
	{
		return new CurrentDataCompletionHandler()
		{
			@Override
			public void progressedDownloadingCurrentWeatherData(WeatherData weatherData)
			{
				CurrentDataFragment.this.updateUI(weatherData);
			}

			@Override
			public void finishedDownloadingCurrentWeatherData(WeatherData weatherData)
			{
				ArrayList<WeatherData> weatherDataArray = new ArrayList<WeatherData>();
				weatherDataArray.add(weatherData);
				CurrentDataFragment.this.dbHelper.insertWeatherData(weatherDataArray);
				CurrentDataFragment.this.updateUI(weatherData);
				CurrentDataFragment.this.setRefreshButtonRotating(false);
			}
		};
	}

	private void setRefreshButtonRotating(boolean rotating)
	{
		if (rotating)
		{
			final Animation myRotation = AnimationUtils.loadAnimation(this.getActivity().getApplicationContext(), R.anim.rotator);
			this.refreshButton.startAnimation(myRotation);
		}
		else
		{

			this.refreshButton.clearAnimation();
		}
	}

}