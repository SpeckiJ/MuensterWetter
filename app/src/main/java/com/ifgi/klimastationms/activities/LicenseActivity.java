package com.ifgi.klimastationms.activities;

import com.ifgi.klimastationms.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LicenseActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_license);
	
		Button licenseLinkButtonSummary = (Button)this.findViewById(R.id.buttonLinkLicenseSummary);
		licenseLinkButtonSummary.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LicenseActivity.this.getResources().getString(R.string.about_license_link_summary)));
				startActivity(browserIntent);
			}
		});
		
		Button licenseLinkButtonFull = (Button)this.findViewById(R.id.buttonLinkLicenseFull);
		licenseLinkButtonFull.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{	
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LicenseActivity.this.getResources().getString(R.string.about_license_link_full)));
				startActivity(browserIntent);
			}
		});
		
	}
}
