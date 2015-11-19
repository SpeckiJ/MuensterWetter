package com.ifgi.klimastationms.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.fragments.CurrentDataFragment;
import com.ifgi.klimastationms.fragments.CurrentDataFragment.CurrentDataFragmentListener;
import com.ifgi.klimastationms.fragments.GraphFragment;
import com.ifgi.klimastationms.fragments.GraphFragment.GraphPhenomenon;

public class MainActivity extends FragmentActivity implements CurrentDataFragmentListener
{
	private static final String FRAGMENT_TAG_CURRENT_DATA = "FRAGMENT_TAG_CURRENT_DATA";
	private static final String FRAGMENT_TAG_GRAPH = "FRAGMENT_TAG_GRAPH";
//	private static final String GRAPH_DATA_IDENTIFIER = "GRAPH_DATA_IDENTIFIER";
	private static final String PREFERENCES_FILENAME = "com.klimastation.app";

	CurrentDataFragment currentDataFragment;
	GraphFragment graphFragment;

	@Override
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (this.isLandscape())
		{
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		this.setContentView(R.layout.activity_main);
		
		FragmentManager fragmentManager = this.getSupportFragmentManager();

		if (this.isTablet())
		{
			this.currentDataFragment = (CurrentDataFragment) fragmentManager.findFragmentById(R.id.currentDataFragment);
			this.graphFragment = (GraphFragment) fragmentManager.findFragmentById(R.id.graphFragment);
		}
		else
		{
			this.graphFragment = (GraphFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_GRAPH);
			this.currentDataFragment = (CurrentDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_CURRENT_DATA);

			if (this.graphFragment == null)
			{
				this.graphFragment = new GraphFragment();
				this.graphFragment.setArguments(getIntent().getExtras());

				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.add(R.id.fragment_container, this.graphFragment, FRAGMENT_TAG_GRAPH);
				transaction.commit();
			}

			if (this.currentDataFragment == null)
			{
				this.currentDataFragment = new CurrentDataFragment();
				this.currentDataFragment.setArguments(getIntent().getExtras());

				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.add(R.id.fragment_container, this.currentDataFragment, FRAGMENT_TAG_CURRENT_DATA);
				transaction.commit();
			}

			if (this.isLandscape())
			{
				this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.hide(this.currentDataFragment);
				transaction.show(this.graphFragment);
				transaction.commit();
			}
			else
			{
				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.hide(this.graphFragment);
				transaction.show(this.currentDataFragment);
				transaction.commit();
			}

			this.showFirstStartDialog();
		}
		
	}

	@Override
	public void onWebcamButtonPressed(ImageButton webcamButton)
	{
		Intent intent = new Intent(this, WebcamActivity.class);
		startActivity(intent);
	}

	@Override
	public void onInfoButtonPressed(ImageButton infoButton)
	{
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	@Override
	public void onListViewItemSelected(GraphPhenomenon graphPhenomenon)
	{

	}

	private void showFirstStartDialog()
	{
		SharedPreferences sharedPreferences = this.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);

		String versionString = this.getResources().getString(R.string.version);
		boolean isFirstStart = sharedPreferences.getBoolean(versionString, true);

		if (isFirstStart)
		{
			Intent intent = new Intent(this, IntroductionActivity.class);
			startActivity(intent);

			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(versionString, false);
			editor.commit();
		}
	}

	private boolean isTablet()
	{
		return (this.findViewById(R.id.fragment_container) == null);
	}

	private boolean isLandscape()
	{
		return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
	}
}
