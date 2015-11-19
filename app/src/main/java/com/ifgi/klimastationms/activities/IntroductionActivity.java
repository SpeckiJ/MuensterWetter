package com.ifgi.klimastationms.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.ifgi.klimastationms.R;

public class IntroductionActivity extends Activity
{
	ImageView imageViewData;
	ImageView imageViewGraph;

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_introduction);

		Button startButton = (Button) this.findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				IntroductionActivity.this.finish();
			}
		});

		this.imageViewData = (ImageView) this.findViewById(R.id.dataImageView);
		this.imageViewGraph = (ImageView) this.findViewById(R.id.graphImageView);

		Animation zeroAlpha = AnimationUtils.loadAnimation(this, R.anim.zero_alpha);
		this.imageViewGraph.setAnimation(zeroAlpha);

		Animation animRotate = AnimationUtils.loadAnimation(this, R.anim.rotate_introduction_images);
		Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

		AnimationSet animationSetData = new AnimationSet(false);
		animationSetData.addAnimation(animRotate);
		animationSetData.addAnimation(fadeOut);
		animationSetData.setRepeatMode(Animation.REVERSE);

		AnimationSet animationSetGraph = new AnimationSet(false);
		animationSetGraph.addAnimation(animRotate);
		animationSetGraph.addAnimation(fadeIn);
		animationSetGraph.setRepeatMode(Animation.REVERSE);

		IntroductionActivity.this.imageViewData.startAnimation(animationSetData);
		IntroductionActivity.this.imageViewGraph.startAnimation(animationSetGraph);
		

	}
}
