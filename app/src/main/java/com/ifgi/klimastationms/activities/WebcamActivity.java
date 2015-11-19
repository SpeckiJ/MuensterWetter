package com.ifgi.klimastationms.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ifgi.klimastationms.R;

public class WebcamActivity extends Activity
{
	ImageView imageView;
	TextView textView;
	ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_webcam);

		this.imageView = (ImageView) this.findViewById(R.id.imageView);
		this.textView = (TextView) this.findViewById(R.id.descriptionTextView);
		this.progressBar = (ProgressBar) this.findViewById(R.id.progressBar);

		this.textView.setText(this.getResources().getString(R.string.webcamDescriptionText));		
		this.imageView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				WebcamActivity.this.loadImage();
			}
		});
		
		this.loadImage(); 
	}

	private void loadImage()
	{
		new ImageDownloader().execute(this.getResources().getString(R.string.webcam_url));				
	}
	
	private class ImageDownloader extends AsyncTask<String, Object, Bitmap>
	{
		protected Bitmap doInBackground(String... arg0)
		{
			try
			{
				Bitmap bitmap = BitmapFactory.decodeStream(new java.net.URL((String) arg0[0]).openStream());
				return bitmap;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}

		protected void onPostExecute(Bitmap bitmap)
		{
			if (bitmap != null)
			{
				WebcamActivity.this.imageView.clearAnimation();
				//WebcamActivity.this.imageView.setScaleType(ImageView.ScaleType.FIT_END);
				WebcamActivity.this.imageView.setImageBitmap(bitmap);
			}
		}
	}

}
