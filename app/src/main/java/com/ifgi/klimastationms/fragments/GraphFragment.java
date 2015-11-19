package com.ifgi.klimastationms.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import android.R.integer;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.UrlQuerySanitizer.ValueSanitizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.datamodel.DatabaseHelper;
import com.ifgi.klimastationms.datamodel.FileDownloader;
import com.ifgi.klimastationms.datamodel.FontUtil;
import com.ifgi.klimastationms.datamodel.GraphRange;
import com.ifgi.klimastationms.datamodel.WeatherData;
import com.ifgi.klimastationms.datamodel.WeatherDataProvider;
import com.ifgi.klimastationms.datamodel.WeatherDataProvider.ArchiveDataCompletionHandler;
import com.ifgi.klimastationms.views.FontableTextView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

public class GraphFragment extends Fragment
{
	public enum GraphPhenomenon
	{
		TEMPERATURE, WIND_SPEED, WIND_DIRECTION, HUMIDITY, ATMOSPHERIC_PRESSURE, RADIATION;
	}

	private enum GraphLabelStepsize
	{
		Temperature(5), AtmosphericPressure(20), Humidity(20), WindSpeed(2), Radiation(200);

		private GraphLabelStepsize(int size)
		{
			this.size = size;
		}

		public int size;
	}

	private static final int MAX_VALUE_INDEX = 0;
	private static final int MIN_VALUE_INDEX = 1;
	private static final String PREFERENCES_FILENAME = "com.klimastation.app";
	private static final String INDEX_LAST_VIEWED_PHENOMENON = "INDEX_LAST_VIEWED_PHENOMENON";

	private WeatherDataProvider dataProvider;
	private DatabaseHelper dbHelper;
	private GraphPhenomenon graphPhenomenon;
	private GraphRange selectedRange = GraphRange.ONE_DAY;

	private boolean databaseAccessLocked = false;
	private int temperatureScalaMinimumRange = 20;
	
	private LineGraphView graphView;
	private GridView selectionGridView;
	private View currentlySelectedView;
	private SeekBar seekBar;

	private double[] graphValues = new double[0];
	private double[] graphTimeValues = new double[0];
	private double[] graphValuesAdditional = new double[0];

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (this.dataProvider == null)
		{
			this.dbHelper = DatabaseHelper.getInstance(this.getActivity());
			this.dataProvider = new FileDownloader(this.getActivity());
			this.dataProvider.loadArchiveWeatherData(this.getCompletionHandler());
		}

		this.selectWeatherDataFromDatabase();
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
		super.onCreateView(inflater, viewGroup, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_graph, viewGroup, false);

		this.setupGraphView(view);
		this.setupGridView(view);
		this.setupSeekBar(view);

		return view;
	}

	private void setupGraphView(View view)
	{
		this.graphView = new LineGraphView(this.getActivity(), "");
		this.graphView.getGraphViewStyle().setGridColor(this.getResources().getColor(R.color.graphGridColor));
		this.graphView.getGraphViewStyle().setHorizontalLabelsColor(this.getResources().getColor(R.color.textMain));
		this.graphView.getGraphViewStyle().setVerticalLabelsColor(this.getResources().getColor(R.color.textMain));
		this.graphView.getGraphViewStyle().setTextSize(this.getActivity().getResources().getDimension(R.dimen.textSizeSecondary));
		this.graphView.getGraphViewStyle().setVerticalLabelsWidth(75);
		this.graphView.setDrawBackground((this.getGraphPhenomenon() != GraphPhenomenon.WIND_DIRECTION));
		this.graphView.setDrawDataPoints((this.getGraphPhenomenon() == GraphPhenomenon.WIND_DIRECTION));
		this.graphView.setBackgroundColor(this.getResources().getColor(R.color.graphBackgroundColor));
		this.graphView.getGraphViewStyle().setNumHorizontalLabels(6);
		
		this.graphView.setScalable(false);
		this.graphView.setCustomLabelFormatter(this.getLabelFormatter());
		this.setYAxis();

		LinearLayout layout = (LinearLayout) view.findViewById(R.id.graphContainer);
		layout.addView(this.graphView);

		Animation zeroAlpha = AnimationUtils.loadAnimation(this.getActivity(), R.anim.zero_alpha);
		this.graphView.startAnimation(zeroAlpha);
	}

	private void setupGridView(View view)
	{
		this.selectionGridView = (GridView) view.findViewById(R.id.selectionGridView);

		SelectionAdapter adapter = new SelectionAdapter(this.getActivity());
		this.selectionGridView.setAdapter(adapter);
		this.selectionGridView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				if (!GraphFragment.this.databaseAccessLocked)
				{
					GraphPhenomenon newPhenomenon;
					switch (arg2)
					{

						case 0:
							newPhenomenon = GraphPhenomenon.TEMPERATURE;
							break;

						case 1:
							newPhenomenon = GraphPhenomenon.WIND_SPEED;
							break;

						case 2:
							newPhenomenon = GraphPhenomenon.ATMOSPHERIC_PRESSURE;
							break;

						case 3:
							newPhenomenon = GraphPhenomenon.HUMIDITY;
							break;

						case 4:
							newPhenomenon = GraphPhenomenon.WIND_DIRECTION;
							break;

						case 5:
							newPhenomenon = GraphPhenomenon.RADIATION;
							break;

						default:
							newPhenomenon = GraphPhenomenon.TEMPERATURE;
							break;
					}

					if (GraphFragment.this.graphPhenomenon == newPhenomenon)
					{
						return;
					}

					GraphFragment.this.graphPhenomenon = newPhenomenon;

					GraphFragment.this.selectWeatherDataFromDatabase();
					GraphFragment.this.graphView.setDrawBackground((GraphFragment.this.getGraphPhenomenon() != GraphPhenomenon.WIND_DIRECTION));
					GraphFragment.this.graphView.setDrawDataPoints((GraphFragment.this.getGraphPhenomenon() == GraphPhenomenon.WIND_DIRECTION));

					SharedPreferences.Editor editor = GraphFragment.this.getActivity().getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE).edit();
					editor.putInt(INDEX_LAST_VIEWED_PHENOMENON, arg2);
					editor.commit();
					
					if (GraphFragment.this.currentlySelectedView != null && GraphFragment.this.currentlySelectedView != arg1)
					{
						this.unhighlightCurrentRow(GraphFragment.this.currentlySelectedView);
					}

					GraphFragment.this.currentlySelectedView = arg1;
					highlightCurrentRow(GraphFragment.this.currentlySelectedView);
				}
			}

			private void unhighlightCurrentRow(View rowView)
			{
				FontableTextView textView = (FontableTextView) rowView.findViewById(R.id.selectionTextView);
				FontUtil.setCustomFont(textView, GraphFragment.this.getActivity(), "SourceSansPro-Regular.ttf");
			}

			private void highlightCurrentRow(View rowView)
			{
				FontableTextView textView = (FontableTextView) rowView.findViewById(R.id.selectionTextView);
				FontUtil.setCustomFont(textView, GraphFragment.this.getActivity(), "SourceSansPro-Bold.ttf");
			}
		});
	}

	private void setupSeekBar(View view)
	{
		this.seekBar = (SeekBar) view.findViewById(R.id.seekBar);
		this.seekBar.setMax(this.selectedRange.numberOfRanges - 1);
		
		this.seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{	
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{	
				switch (progress)
				{
					case 0:
						GraphFragment.this.selectedRange = GraphRange.ONE_DAY;
						break;
					case 1:
						GraphFragment.this.selectedRange = GraphRange.TWO_DAYS;
						break;
					case 2:
						GraphFragment.this.selectedRange = GraphRange.THREE_DAYS;
						break;
					case 3:
						GraphFragment.this.selectedRange = GraphRange.FIVE_DAYS;
						break;
					case 4:
						GraphFragment.this.selectedRange = GraphRange.SEVEN_DAYS;
						break;
					case 5:
						GraphFragment.this.selectedRange = GraphRange.TEN_DAYS;
						break;
					case 6:
						GraphFragment.this.selectedRange = GraphRange.TWELVE_DAYS;
						break;
					case 7:
						GraphFragment.this.selectedRange = GraphRange.FOURTEEN_DAYS;
						break;
					case 8:
						GraphFragment.this.selectedRange = GraphRange.SEVENTEEN_DAYS;
						break;
					case 9:
						GraphFragment.this.selectedRange = GraphRange.TWENTY_DAYS;
						break;
					default:
						break;
				}
				
				GraphFragment.this.selectWeatherDataFromDatabase();
			}
			
			public void onStartTrackingTouch(SeekBar seekBar)
			{				
			}
			public void onStopTrackingTouch(SeekBar seekBar)
			{				
			}
		});
	}
	
	private void setYAxis()
	{
		int max = 0;
		int min = 0;
		ArrayList<String> labelStrings = new ArrayList<String>();

		switch (this.getGraphPhenomenon())
		{
			case TEMPERATURE:
				max = (int) Math.ceil(this.getMinMaxValues()[MAX_VALUE_INDEX]);
				min = (int) Math.floor(this.getMinMaxValues()[MIN_VALUE_INDEX]);

				int temperatureScalaStep = GraphLabelStepsize.Temperature.size;

				if (max > 0)
				{
					max += temperatureScalaStep - (max % temperatureScalaStep);
				}
				else
				{
					max -= (max % temperatureScalaStep);
				}

				if (min > 0)
				{
					min -= (min % temperatureScalaStep);
				}
				else
				{
					min -= temperatureScalaStep + (min % temperatureScalaStep);
				}

				if (max - min < temperatureScalaMinimumRange)
				{
					max = min + temperatureScalaMinimumRange;
				}

				labelStrings = this.calculateLabelStringsForStepsize(GraphLabelStepsize.Temperature, max, min);

				break;
			case ATMOSPHERIC_PRESSURE:
				max = 1040;
				min = 960;

				labelStrings = this.calculateLabelStringsForStepsize(GraphLabelStepsize.AtmosphericPressure, max, min);

				break;
			case HUMIDITY:
				max = 100;
				min = 20;

				labelStrings = this.calculateLabelStringsForStepsize(GraphLabelStepsize.Humidity, max, min);

				break;
			case RADIATION:
				max = 1200;
				min = 0;

				labelStrings = this.calculateLabelStringsForStepsize(GraphLabelStepsize.Radiation, max, min);

				break;
			case WIND_DIRECTION:
				max = 360;
				min = 0;

				labelStrings.add("N");
				labelStrings.add("W");
				labelStrings.add("S");
				labelStrings.add("E");
				labelStrings.add("N");

				break;
			case WIND_SPEED:
				max = (int) Math.round(this.getMinMaxValues()[MAX_VALUE_INDEX]);
				min = 0;

				labelStrings = this.calculateLabelStringsForStepsize(GraphLabelStepsize.WindSpeed, max, min);

				break;
			default:
				break;
		}

		this.graphView.setManualYAxisBounds(max, min);
		this.graphView.setVerticalLabels(labelStrings.toArray(new String[labelStrings.size()]));
	}

	private String getGraphLegendDescription()
	{
		String graphPhenomenonName = "";

		if (this.getGraphPhenomenon() == GraphPhenomenon.WIND_SPEED)
		{
			graphPhenomenonName = "Durchschnitt";
		}
		else if (this.getGraphPhenomenon() == GraphPhenomenon.RADIATION)
		{
			graphPhenomenonName = "Gemessen";
		}
		return graphPhenomenonName;
	}

	private double[] getMinMaxValues()
	{
		double minValue = Integer.MAX_VALUE;
		double maxValue = Integer.MIN_VALUE;

		if (this.graphPhenomenon == GraphPhenomenon.WIND_SPEED)
		{
			for (int i = 0; i < this.graphValuesAdditional.length; i++)
			{
				double value = this.graphValuesAdditional[i];
				minValue = Math.min(value, minValue);
				maxValue = Math.max(value, maxValue);
			}
		}
		else
		{
			for (int i = 0; i < this.graphValues.length; i++)
			{
				double value = this.graphValues[i];
				minValue = Math.min(value, minValue);
				maxValue = Math.max(value, maxValue);
			}
		}

		double[] minMaxValues = new double[2];
		minMaxValues[MAX_VALUE_INDEX] = maxValue;
		minMaxValues[MIN_VALUE_INDEX] = minValue;

		return minMaxValues;
	}

	private ArrayList<String> calculateLabelStringsForStepsize(GraphLabelStepsize stepsize, int max, int min)
	{
		ArrayList<String> labelStrings = new ArrayList<String>();

		for (int i = max; i >= min; i--)
		{
			if (i % stepsize.size == 0)
			{
				labelStrings.add(String.valueOf(i));
			}
		}

		return labelStrings;
	}

	private CustomLabelFormatter getLabelFormatter()
	{
		return new CustomLabelFormatter()
		{
			@Override
			public String formatLabel(double value, boolean isValueX)
			{
				String result = "";

				if (isValueX && GraphFragment.this.graphTimeValues.length > 0)
				{
					Date date = new Date((long)GraphFragment.this.graphTimeValues[(int)value]);
					SimpleDateFormat dateFormat = new SimpleDateFormat(GraphFragment.this.selectedRange.dateFormatPattern);
					result = dateFormat.format(date);
				}

				return result;
			}
		};
	}

	private GraphPhenomenon getGraphPhenomenon()
	{
		if (this.graphPhenomenon == null)
		{
			SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
			int lastViewdIndex = sharedPreferences.getInt(INDEX_LAST_VIEWED_PHENOMENON, 0);

			switch (lastViewdIndex)
			{
				case 0:
					this.graphPhenomenon = GraphPhenomenon.TEMPERATURE;
					break;
				case 1:
					this.graphPhenomenon = GraphPhenomenon.WIND_SPEED;
					break;
				case 2:
					this.graphPhenomenon = GraphPhenomenon.HUMIDITY;
					break;
				case 3:
					this.graphPhenomenon = GraphPhenomenon.ATMOSPHERIC_PRESSURE;
					break;
				case 4:
					this.graphPhenomenon = GraphPhenomenon.WIND_DIRECTION;
					break;
				case 5:
					this.graphPhenomenon = GraphPhenomenon.RADIATION;
					break;
				default:
					break;
			}
		}

		return this.graphPhenomenon;
	}

	private ArchiveDataCompletionHandler getCompletionHandler()
	{
		return new ArchiveDataCompletionHandler()
		{
			@Override
			public void progressededDownloadingArchiveWeatherData(WeatherData weatherData)
			{
			}

			@Override
			public void finishedDownloadingArchiveWeatherData(ArrayList<WeatherData> weatherDataArray)
			{
				GraphFragment.DatabaseInsertTask insertTask = new DatabaseInsertTask();
				insertTask.execute(weatherDataArray);
			}
		};
	}

	private void selectWeatherDataFromDatabase()
	{
		if (!this.databaseAccessLocked)
		{
			DatabaseSelectionTask dbSelectionTask = new DatabaseSelectionTask();
			dbSelectionTask.execute(GraphFragment.this.selectedRange.length);
		}
	}

	private void purgeDatabase()
	{
		DataBasePurgeTask dbPurgeTask = new DataBasePurgeTask();
		dbPurgeTask.execute();
	}

	private void updateGUI()
	{
		this.graphView.removeAllSeries();

		GraphViewData[] graphViewData;

		if (this.graphValuesAdditional != null && this.graphValuesAdditional.length > 0)
		{
			graphViewData = new GraphViewData[this.graphValuesAdditional.length];

			for (int i = 0; i < this.graphValuesAdditional.length; i++)
			{
				graphViewData[i] = new GraphViewData(i, this.graphValuesAdditional[i]);
			}

			GraphViewSeriesStyle style = new GraphViewSeriesStyle();
			style.color = this.getResources().getColor(R.color.graphLineColorSecondary);
			style.thickness = 1;

			String legendDescriptionString = (this.graphPhenomenon == GraphPhenomenon.WIND_SPEED) ? "Böen" : "Wolkenlos";
			this.graphView.addSeries(new GraphViewSeries(legendDescriptionString, style, graphViewData));
			this.graphView.setShowLegend(true);
			this.graphView.setLegendAlign(LegendAlign.TOP);
			this.graphView.getGraphViewStyle().setLegendWidth((this.graphPhenomenon == GraphPhenomenon.WIND_SPEED) ? 220 : 200);
		}
		else
		{
			this.graphView.setShowLegend(false);
		}

		graphViewData = new GraphViewData[this.graphValues.length];

		for (int i = 0; i < this.graphValues.length; i++)
		{
			graphViewData[i] = new GraphViewData(i, this.graphValues[i]);
		}
		
		this.calculateVerticalLines();
		
		GraphViewSeriesStyle style = new GraphViewSeriesStyle();
		style.color = this.getResources().getColor(R.color.graphLineColorPrimary);
		style.thickness = 1;

		this.graphView.addSeries(new GraphViewSeries(this.getGraphLegendDescription(), style, graphViewData));
		this.setYAxis();
	}

	private void calculateVerticalLines()
	{
		ArrayList<Integer> values = new ArrayList<Integer>();
		values.add(Integer.valueOf(0));
		
		Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH.mm");

		for (int value = 0; value < this.graphValues.length; value++)
		{
			Date date = new Date((long)this.graphTimeValues[value]);
			calendar.setTime(date);
			Log.e("", dateFormat.format(date));
			
			if (calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.HOUR_OF_DAY) % this.selectedRange.significantDistanceHours == 0 && calendar.get(Calendar.DAY_OF_YEAR) % this.selectedRange.significantDistanceDays == 0)
			{
				values.add(Integer.valueOf(value));
			}
		}
		
		values.add(Integer.valueOf(this.graphValues.length - 1));

		int[] intValues = new int[values.size()];
		
		for (int i = 0; i < values.size(); i++)
		{
			intValues[i] = values.get(i).intValue();
		}
		
		this.graphView.verticalLinesValues = intValues;
		this.graphView.getGraphViewStyle().setNumHorizontalLabels(intValues.length);
	}
	
	private class DatabaseSelectionTask extends AsyncTask<Integer, WeatherData, HashMap<String, double[]>>
	{
		@Override
		protected HashMap<String, double[]> doInBackground(Integer... limit)
		{
			HashMap<String, double[]> values = GraphFragment.this.dbHelper.fetchWeatherDataSinceDate(limit[0].intValue(), GraphFragment.this.getGraphPhenomenon());

			return values;
		}

		@Override
		protected void onPostExecute(HashMap<String, double[]> weatherDataHashMap)
		{
			if (GraphFragment.this.getActivity() != null && weatherDataHashMap.get(DatabaseHelper.HASHMAP_KEY_VALUES).length >= 144)
			{
				GraphFragment.this.graphValues = weatherDataHashMap.get(DatabaseHelper.HASHMAP_KEY_VALUES);
				GraphFragment.this.graphTimeValues = weatherDataHashMap.get(DatabaseHelper.HASHMAP_KEY_TIME_VALUES);

				if (weatherDataHashMap.get(DatabaseHelper.HASHMAP_KEY_ADDITIONAL_VALUES) != null)
				{
					GraphFragment.this.graphValuesAdditional = weatherDataHashMap.get(DatabaseHelper.HASHMAP_KEY_ADDITIONAL_VALUES);
				}
				else
				{
					GraphFragment.this.graphValuesAdditional = null;
				}

				GraphFragment.this.updateGUI();

				Animation fadeIn = AnimationUtils.loadAnimation(GraphFragment.this.getActivity(), R.anim.fade_in);
				fadeIn.setStartOffset(0);
				fadeIn.setRepeatCount(0);
				fadeIn.setDuration(0);
				GraphFragment.this.graphView.startAnimation(fadeIn);
			}
			
			GraphFragment.this.databaseAccessLocked = false;
		}
	}

	private class DatabaseInsertTask extends AsyncTask<List<WeatherData>, WeatherData, List<WeatherData>>
	{
		@Override
		protected List<WeatherData> doInBackground(List<WeatherData>... params)
		{
			List<WeatherData> weatherDataArray = params[0];

			if (weatherDataArray == null)
			{
				return null;
			}

			GraphFragment.this.dbHelper.insertWeatherData(weatherDataArray);

			return weatherDataArray;
		}

		@Override
		protected void onPostExecute(List<WeatherData> params)
		{
			Log.w("Done", "Done inserting data into database");
			GraphFragment.this.purgeDatabase();
			GraphFragment.this.selectWeatherDataFromDatabase();
		}
	}

	private class DataBasePurgeTask extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... arg0)
		{
			GraphFragment.this.dbHelper.purgeDatabase();

			return "";
		}

	}

	private class SelectionAdapter extends ArrayAdapter<String>
	{
		private ArrayList<String> values;
	    private LayoutInflater layoutInflater;
	    private Context context;
	    private int firstHighlightedIndex = 0;
	    
		public SelectionAdapter(Context context)
		{
			super(context, 0);
			this.values = new ArrayList<String>();
			this.context = context;
	    	this.layoutInflater = LayoutInflater.from(context);

	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionTemperature) + " (" + context.getResources().getString(R.string.phenomenonUnitTemperature) + ")");
	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionWindSpeed) + " (" + context.getResources().getString(R.string.phenomenonUnitWindSpeed) + ")");
	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionAtmosphericPressure) + " (" + context.getResources().getString(R.string.phenomenonUnitAtmosphericPressure) + ")");
	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionHumidity) + " (" + context.getResources().getString(R.string.phenomenonUnitHumidity) + ")");
	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionWindDirection) + " (" + context.getResources().getString(R.string.phenomenonUnitWindDirection) + ")");
	    	this.values.add(context.getResources().getString(R.string.phenomenonDescriptionRadiation) + " (" + context.getResources().getString(R.string.phenomenonUnitRadiation) + ")");
	    	
			SharedPreferences sharedPreferences = GraphFragment.this.getActivity().getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
			this.firstHighlightedIndex = sharedPreferences.getInt(INDEX_LAST_VIEWED_PHENOMENON, 0);
		}

		@Override
		public int getCount()
		{
			return this.values.size();
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public String getItem(int arg0)
		{
			return this.values.get(arg0);
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView == null)
			{
				convertView = layoutInflater.inflate(R.layout.selection_grid_view_cell, null);
				holder = new ViewHolder();
				holder.textView = (TextView) convertView.findViewById(R.id.selectionTextView);
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			if (this.firstHighlightedIndex == position)
			{
				FontableTextView textView = (FontableTextView) convertView.findViewById(R.id.selectionTextView);
				FontUtil.setCustomFont(textView, GraphFragment.this.getActivity(), "SourceSansPro-Bold.ttf");
				GraphFragment.this.currentlySelectedView = convertView;
				this.firstHighlightedIndex = -1;
			}
			
			holder.textView.setText(this.getItem(position));

			return convertView;
		}

		class ViewHolder
		{
			TextView textView;
		}

	}
}
