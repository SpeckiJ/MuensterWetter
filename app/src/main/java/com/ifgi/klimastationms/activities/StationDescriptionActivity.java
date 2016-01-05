package com.ifgi.klimastationms.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.datamodel.StationInfoListAdapter;

public class StationDescriptionActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_station_description);
	
		TextView descriptionTextView = (TextView)this.findViewById(R.id.stationDescriptionTextView);
		descriptionTextView.setText("Die Station befindet sich auf dem Dach des Instituts für Landschaftsökologie in der Heisenbergstraße 2.");
		
		
		ArrayList<HashMap<String, String>> dataArray = new ArrayList<HashMap<String,String>>();
		dataArray.add(this.createHashmapWithTitleAndDetail("Position", "Geografische Breite:\t51°58'09.34806”N \nGeografische Länge:\t7°35'45.16282”E\nHöhe über NN:\t\t84 m"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Lufttemperatur / Luftfeuchte", "Young Typ 41382VC Kombisensor (Platin-Widerstandsthermometer + kapazitiver Feuchtesensor)"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Windgeschwindigkeit und Windrichtung", "Gill WindSonic Anemometer RS232\nYoung Wind Monitor 05103-45"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Niederschlag", "TRwS204 Niederschlagswaage"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Luftdruck", "Young Typ 61302V\nKapazitiver Drucksensor"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Kurzwellige Strahlung", "Kipp & Zonen Typ CMP6 Pyranometer"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Sichtweite", "Present Weather Sensor Biral SWS100"));
		dataArray.add(this.createHashmapWithTitleAndDetail("Wolkenhöhe und ‑bedeckungsgrad", "Lufft Ceilometer CHM 15k \"Nimbus\""));
		
		ListView listView = (ListView)this.findViewById(R.id.stationInfoListView);
		listView.setAdapter(new StationInfoListAdapter(this, dataArray));
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3)
			{
				String uriString = "";
				switch (index)
				{
					case 0:
						uriString = "geo:51.969114,7.596054";
						break;
					case 1:
						uriString = "http://www.uni-muenster.de/Klima/wetter/temp_feuchte.shtml";
						break;
					case 2:
						uriString = "http://www.uni-muenster.de/Klima/wetter/windmessung.shtml";
						break;
					case 3:
						uriString = "http://www.uni-muenster.de/Klima/wetter/niederschlagmessung.shtml";
						break;
					case 4:
						uriString = "http://www.uni-muenster.de/Klima/wetter/luftdruck.shtml";
						break;
					case 5:
						uriString = "http://www.uni-muenster.de/Klima/wetter/strahlungsmessung.shtml";
						break;
					case 6:
						uriString = "http://www.uni-muenster.de/Klima/wetter/sichtweite.shtml";
						break;
					case 7:
						uriString = "http://www.uni-muenster.de/Klima/wetter/Ceilometer.shtml";
						break;
					default:
						break;
				}
				

				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
				startActivity(intent);	
				
			}
		});
		
		
	}
	
	private HashMap<String, String> createHashmapWithTitleAndDetail(String title, String detail)
	{
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("title", title);
		hashMap.put("detail", detail);
		
		return hashMap;
	}
	
	
}

