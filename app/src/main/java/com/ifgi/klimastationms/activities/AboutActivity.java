package com.ifgi.klimastationms.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.ifgi.klimastationms.R;

public class AboutActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_about);
		
		ImageButton klimaButton = (ImageButton)this.findViewById(R.id.imageButtonKli);
		klimaButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.uni-muenster.de/Klima/"));
				startActivity(browserIntent);				
			}
		});
		
		ImageButton iLoekButton = (ImageButton)this.findViewById(R.id.imageButtoniLoek);
		iLoekButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.uni-muenster.de/Landschaftsoekologie/"));
				startActivity(browserIntent);				
			}
		});
		
		Button stationDescriptionButton = (Button)this.findViewById(R.id.buttonStation);
		stationDescriptionButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(AboutActivity.this, StationDescriptionActivity.class);
				startActivity(intent);
			}
		});
		
		Button licenseDescriptionButton = (Button)this.findViewById(R.id.buttonLicense);
		licenseDescriptionButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{	
				Intent intent = new Intent(AboutActivity.this, LicenseActivity.class);
				startActivity(intent);
			}
		});
	}
}
