/**
 * This file is part of GraphView.
 *
 * GraphView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphView.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 *
 * Copyright Jonas Gehring
 */

package com.jjoe64.graphview;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.ifgi.klimastationms.R;
import com.ifgi.klimastationms.datamodel.FontUtil;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.compatible.ScaleGestureDetector;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs. This
 * is the abstract base class for all graphs. Extend this class and implement
 * {@link #drawSeries(Canvas, GraphViewDataInterface[], float, float, float, double, double, double, double, float)}
 * to display a custom graph. Use {@link LineGraphView} for creating a line
 * chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser
 *         General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout
{
	static final private class GraphViewConfig
	{
		static final float BORDER = 20;
	}

	private class GraphViewContentView extends View
	{
		private float graphwidth;
		private float lastTouchEventX;
		private boolean scrollingStarted;

		/**
		 * @param context
		 */
		public GraphViewContentView(Context context)
		{
			super(context);
			this.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.FILL_PARENT));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas)
		{
			GraphView.this.paint.setAntiAlias(true);

			// normal
			GraphView.this.paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float horstart = 0;
			float height = this.getHeight();
			float width = this.getWidth() - 1;
			double maxY = GraphView.this.getMaxY();
			double minY = GraphView.this.getMinY();
			double maxX = GraphView.this.getMaxX(false);
			double minX = GraphView.this.getMinX(false);
			double diffX = maxX - minX;

			// measure bottom text
			if (GraphView.this.labelTextHeight == null || GraphView.this.horLabelTextWidth == null)
			{
				GraphView.this.paint.setTextSize(GraphView.this.getGraphViewStyle().getTextSize());
				double testX = ((GraphView.this.getMaxX(true) - GraphView.this.getMinX(true)) * 0.783) + GraphView.this.getMinX(true);
				String testLabel = GraphView.this.formatLabel(testX, true);
				GraphView.this.paint.getTextBounds(testLabel, 0, testLabel.length(), GraphView.this.textBounds);
				GraphView.this.labelTextHeight = Math.abs(GraphView.this.textBounds.height());
				GraphView.this.horLabelTextWidth = Math.abs(GraphView.this.textBounds.width());
			}
			border += GraphView.this.labelTextHeight;

			float graphheight = height - (2 * border);
			this.graphwidth = width;

			if (GraphView.this.horlabels == null)
			{
				GraphView.this.horlabels = GraphView.this.generateHorlabels(this.graphwidth);
			}
			if (GraphView.this.verlabels == null)
			{
				GraphView.this.verlabels = GraphView.this.generateVerlabels(graphheight);
			}

			// horizontal lines
			GraphView.this.paint.setTextAlign(Align.LEFT);
			int vers = GraphView.this.verlabels.length - 1;
			for (int i = 0; i < GraphView.this.verlabels.length; i++)
			{
				GraphView.this.paint.setColor(GraphView.this.graphViewStyle.getGridColor());
				GraphView.this.paint.setShadowLayer(0, 0, 0, this.getResources().getColor(R.color.clear));
				
				float y = ((graphheight / vers) * i) + border;
				canvas.drawLine(horstart, y, width, y, GraphView.this.paint);
			}

			GraphView.this.drawHorizontalLabels(canvas, border, horstart, height, GraphView.this.horlabels, this.graphwidth);

			GraphView.this.paint.setTextAlign(Align.CENTER);

			Typeface typef = FontUtil.getFont(this.getContext(), "SourceSansPro-Regular.ttf");
			GraphView.this.paint.setTypeface(typef);
			GraphView.this.paint.setColor(Color.WHITE);
			GraphView.this.paint.setShadowLayer(1, 0, 1, this.getResources().getColor(R.color.textShadow));
			
			canvas.drawText(GraphView.this.title, (this.graphwidth / 2) + horstart, border - 4, GraphView.this.paint);

			if (maxY == minY)
			{
				// if min/max is the same, fake it so that we can render a line
				if (maxY == 0)
				{
					// if both are zero, change the values to prevent division
					// by zero
					maxY = 1.0d;
					minY = 0.0d;
				}
				else
				{
					maxY = maxY * 1.05d;
					minY = minY * 0.95d;
				}
			}

			double diffY = maxY - minY;
			GraphView.this.paint.setStrokeCap(Paint.Cap.ROUND);

			for (int i = 0; i < GraphView.this.graphSeries.size(); i++)
			{
				GraphView.this.drawSeries(canvas, GraphView.this._values(i), this.graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart, GraphView.this.graphSeries.get(i).style);
			}

			if (GraphView.this.showLegend)
			{
				GraphView.this.drawLegend(canvas, height, width);
			}
		}

		private void onMoveGesture(float f)
		{
			// view port update
			if (GraphView.this.viewportSize != 0)
			{
				GraphView.this.viewportStart -= f * GraphView.this.viewportSize / this.graphwidth;

				// minimal and maximal view limit
				double minX = GraphView.this.getMinX(true);
				double maxX = GraphView.this.getMaxX(true);
				if (GraphView.this.viewportStart < minX)
				{
					GraphView.this.viewportStart = minX;
				}
				else if (GraphView.this.viewportStart + GraphView.this.viewportSize > maxX)
				{
					GraphView.this.viewportStart = maxX - GraphView.this.viewportSize;
				}

				// labels have to be regenerated
				if (!GraphView.this.staticHorizontalLabels)
				{
					GraphView.this.horlabels = null;
				}
				if (!GraphView.this.staticVerticalLabels)
				{
					GraphView.this.verlabels = null;
				}
				GraphView.this.viewVerLabels.invalidate();
			}
			this.invalidate();
		}

		/**
		 * @param event
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			if (!GraphView.this.isScrollable() || GraphView.this.isDisableTouch())
			{
				return super.onTouchEvent(event);
			}

			boolean handled = false;
			// first scale
			if (GraphView.this.scalable && GraphView.this.scaleDetector != null)
			{
				GraphView.this.scaleDetector.onTouchEvent(event);
				handled = GraphView.this.scaleDetector.isInProgress();
			}
			if (!handled)
			{
				// Log.d("GraphView",
				// "on touch event scale not handled+"+lastTouchEventX);
				// if not scaled, scroll
				if ((event.getAction() & MotionEvent.ACTION_DOWN) == MotionEvent.ACTION_DOWN && (event.getAction() & MotionEvent.ACTION_MOVE) == 0)
				{
					this.scrollingStarted = true;
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP)
				{
					this.scrollingStarted = false;
					this.lastTouchEventX = 0;
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_MOVE) == MotionEvent.ACTION_MOVE)
				{
					if (this.scrollingStarted)
					{
						if (this.lastTouchEventX != 0)
						{
							this.onMoveGesture(event.getX() - this.lastTouchEventX);
						}
						this.lastTouchEventX = event.getX();
						handled = true;
					}
				}
				if (handled)
				{
					this.invalidate();
				}
			}
			else
			{
				// currently scaling
				this.scrollingStarted = false;
				this.lastTouchEventX = 0;
			}
			return handled;
		}
	}

	/**
	 * one data set for a graph series
	 */
	static public class GraphViewData implements GraphViewDataInterface
	{
		public final double valueX;
		public final double valueY;

		public GraphViewData(double valueX, double valueY)
		{
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}

		@Override
		public double getX()
		{
			return this.valueX;
		}

		@Override
		public double getY()
		{
			return this.valueY;
		}
	}

	public enum LegendAlign
	{
		BOTTOM, MIDDLE, TOP
	}

	private class VerLabelsView extends View
	{
		/**
		 * @param context
		 */
		public VerLabelsView(Context context)
		{
			super(context);
			this.setLayoutParams(new LayoutParams(GraphView.this.getGraphViewStyle().getVerticalLabelsWidth() == 0 ? 100 : GraphView.this.getGraphViewStyle().getVerticalLabelsWidth(), android.view.ViewGroup.LayoutParams.FILL_PARENT));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas)
		{
			// normal
			GraphView.this.paint.setStrokeWidth(0);

			// measure bottom text
			if (GraphView.this.labelTextHeight == null || GraphView.this.verLabelTextWidth == null)
			{
				GraphView.this.paint.setTextSize(GraphView.this.getGraphViewStyle().getTextSize());
				double testY = ((GraphView.this.getMaxY() - GraphView.this.getMinY()) * 0.783) + GraphView.this.getMinY();
				String testLabel = GraphView.this.formatLabel(testY, false);
				GraphView.this.paint.getTextBounds(testLabel, 0, testLabel.length(), GraphView.this.textBounds);
				GraphView.this.labelTextHeight = (GraphView.this.textBounds.height());
				GraphView.this.verLabelTextWidth = (GraphView.this.textBounds.width());
			}
			if (GraphView.this.getGraphViewStyle().getVerticalLabelsWidth() == 0 && this.getLayoutParams().width != GraphView.this.verLabelTextWidth + GraphViewConfig.BORDER)
			{
				this.setLayoutParams(new LayoutParams((int) (GraphView.this.verLabelTextWidth + GraphViewConfig.BORDER), android.view.ViewGroup.LayoutParams.FILL_PARENT));
			}
			else if (GraphView.this.getGraphViewStyle().getVerticalLabelsWidth() != 0 && GraphView.this.getGraphViewStyle().getVerticalLabelsWidth() != this.getLayoutParams().width)
			{
				this.setLayoutParams(new LayoutParams(GraphView.this.getGraphViewStyle().getVerticalLabelsWidth(), android.view.ViewGroup.LayoutParams.FILL_PARENT));
			}

			float border = GraphViewConfig.BORDER;
			border += GraphView.this.labelTextHeight;
			float height = this.getHeight();
			float graphheight = height - (2 * border);

			if (GraphView.this.verlabels == null)
			{
				GraphView.this.verlabels = GraphView.this.generateVerlabels(graphheight);
			}

			// vertical labels
			GraphView.this.paint.setTextAlign(GraphView.this.getGraphViewStyle().getVerticalLabelsAlign());
			int labelsWidth = this.getWidth();
			int labelsOffset = 0;
			if (GraphView.this.getGraphViewStyle().getVerticalLabelsAlign() == Align.RIGHT)
			{
				labelsOffset = labelsWidth;
			}
			else if (GraphView.this.getGraphViewStyle().getVerticalLabelsAlign() == Align.CENTER)
			{
				labelsOffset = labelsWidth / 2;
			}
			int vers = GraphView.this.verlabels.length - 1;
			for (int i = 0; i < GraphView.this.verlabels.length; i++)
			{
				float y = ((graphheight / vers) * i) + border;
				GraphView.this.paint.setColor(GraphView.this.graphViewStyle.getVerticalLabelsColor());
				GraphView.this.paint.setShadowLayer(1, 0, 1, this.getResources().getColor(R.color.textShadow));
				GraphView.this.paint.setTypeface(FontUtil.getFont(this.getContext(), "SourceSansPro-Regular.ttf"));

				canvas.drawText(GraphView.this.verlabels[i], labelsOffset, y, GraphView.this.paint);
			}

			// reset
			GraphView.this.paint.setTextAlign(Align.LEFT);
		}
	}

	private CustomLabelFormatter customLabelFormatter;
	private boolean disableTouch;
	private final List<GraphViewSeries> graphSeries;
	private final GraphViewContentView graphViewContentView;
	protected GraphViewStyle graphViewStyle;
	private String[] horlabels;
	private Integer horLabelTextWidth;
	private Integer labelTextHeight;
	private LegendAlign legendAlign = LegendAlign.MIDDLE;
	private double manualMaxYValue;
	private double manualMinYValue;
	private boolean manualYAxis;
	private final NumberFormat[] numberformatter = new NumberFormat[2];
	protected final Paint paint;
	private boolean scalable;
	private ScaleGestureDetector scaleDetector;
	private boolean scrollable;
	private boolean showLegend = false;
	private boolean staticHorizontalLabels;
	public int[] verticalLinesValues;
	private boolean staticVerticalLabels;
	private final Rect textBounds = new Rect(1,1,1,1);
	private String title;
	private String[] verlabels;
	private Integer verLabelTextWidth;
	private double viewportSize;
	private double viewportStart;
	private final View viewVerLabels;

	public GraphView(Context context, AttributeSet attrs)
	{
		this(context, "");
	}

	/**
	 * @param context
	 * @param title
	 *            [optional]
	 */
	public GraphView(Context context, String title)
	{
		super(context);
		this.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.FILL_PARENT));

		if (title == null)
		{
			this.title = "";
		}
		else
		{
			this.title = title;
		}

		this.graphViewStyle = new GraphViewStyle();
		this.graphViewStyle.useTextColorFromTheme(context);

		this.paint = new Paint();
		this.graphSeries = new ArrayList<GraphViewSeries>();

		this.viewVerLabels = new VerLabelsView(context);
		this.addView(this.viewVerLabels);
		this.graphViewContentView = new GraphViewContentView(context);
		this.addView(this.graphViewContentView, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.FILL_PARENT, 1));
	}

	private GraphViewDataInterface[] _values(int idxSeries)
	{
		GraphViewDataInterface[] values = this.graphSeries.get(idxSeries).values;
		synchronized (values)
		{
			if (this.viewportStart == 0 && this.viewportSize == 0)
			{
				// all data
				return values;
			}
			else
			{
				// viewport
				List<GraphViewDataInterface> listData = new ArrayList<GraphViewDataInterface>();
				for (GraphViewDataInterface value : values)
				{
					if (value.getX() >= this.viewportStart)
					{
						if (value.getX() > this.viewportStart + this.viewportSize)
						{
							listData.add(value); // one more for nice
													// scrolling
							break;
						}
						else
						{
							listData.add(value);
						}
					}
					else
					{
						if (listData.isEmpty())
						{
							listData.add(value);
						}
						listData.set(0, value); // one before, for nice
												// scrolling
					}
				}
				return listData.toArray(new GraphViewDataInterface[listData.size()]);
			}
		}
	}

	/**
	 * add a series of data to the graph
	 * 
	 * @param series
	 */
	public void addSeries(GraphViewSeries series)
	{
		series.addGraphView(this);
		this.graphSeries.add(series);
		this.redrawAll();
	}

	protected void drawHorizontalLabels(Canvas canvas, float border, float horstart, float height, String[] horlabels, float graphwidth)
	{ 
		if (this.verticalLinesValues == null)
		{
			return;
		}
		// horizontal labels + lines
		int hors = horlabels.length - 1;
		for (int i = 0; i < horlabels.length; i++)
		{
			Typeface typef = FontUtil.getFont(this.getContext(), "SourceSansPro-Regular.ttf");
			this.paint.setTypeface(typef);
			this.paint.setColor(this.getContext().getResources().getColor(R.color.textMain));
			this.paint.setShadowLayer(1, 0, 1, this.getResources().getColor(R.color.textShadow));
			this.paint.setTextAlign(Align.CENTER);
			if (i == horlabels.length - 1 || i == 0)
			{
				this.paint.setColor(this.getContext().getResources().getColor(R.color.clear));
				this.paint.setShadowLayer(0, 0, 0, this.getResources().getColor(R.color.clear));
			}

			double x = ((graphwidth / this.getMaxX(false)) * this.verticalLinesValues[i]) + horstart;
			canvas.drawText(horlabels[i], (float)x, height - 4, this.paint);
			
			this.paint.setColor(GraphView.this.graphViewStyle.getGridColor());
			canvas.drawLine((float)x, height - border, (float)x, border, this.paint);
		}
	}

	protected void drawLegend(Canvas canvas, float height, float width)
	{
		float textSize = this.paint.getTextSize();
		int spacing = this.getGraphViewStyle().getLegendSpacing();
		int border = this.getGraphViewStyle().getLegendBorder();
		int legendWidth = this.getGraphViewStyle().getLegendWidth();

		int shapeSize = (int) (textSize * 0.8d);

		// rect
		this.paint.setARGB(180, 100, 100, 100);
		float legendHeight = (shapeSize + spacing) * this.graphSeries.size() + 2 * border - spacing;
		float lLeft = width - legendWidth - border * 2;
		float lTop;
		switch (this.legendAlign)
		{
			case TOP:
				lTop = 0;
				break;
			case MIDDLE:
				lTop = height / 2 - legendHeight / 2;
				break;
			default:
				lTop = height - GraphViewConfig.BORDER - legendHeight - this.getGraphViewStyle().getLegendMarginBottom();
		}
		float lRight = lLeft + legendWidth;
		float lBottom = lTop + legendHeight;
		canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8, this.paint);

		for (int i = 0; i < this.graphSeries.size(); i++)
		{
			this.paint.setColor(this.graphSeries.get(i).style.color);
			canvas.drawRect(new RectF(lLeft + border, lTop + border + (i * (shapeSize + spacing)), lLeft + border + shapeSize, lTop + border + (i * (shapeSize + spacing)) + shapeSize), this.paint);
			if (this.graphSeries.get(i).description != null)
			{
				this.paint.setTypeface(FontUtil.getFont(this.getContext(), "SourceSansPro-Light.ttf"));
				this.paint.setColor(Color.WHITE);
				this.paint.setTextAlign(Align.LEFT);
				canvas.drawText(this.graphSeries.get(i).description, lLeft + border + shapeSize + spacing, lTop + border + shapeSize + (i * (shapeSize + spacing)), this.paint);
			}
		}
	}

	abstract protected void drawSeries(Canvas canvas, GraphViewDataInterface[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style);

	/**
	 * formats the label use #setCustomLabelFormatter or static labels if you
	 * want custom labels
	 * 
	 * @param value
	 *            x and y values
	 * @param isValueX
	 *            if false, value y wants to be formatted
	 * @deprecated use {@link #setCustomLabelFormatter(CustomLabelFormatter)}
	 * @return value to display
	 */
	@Deprecated
	protected String formatLabel(double value, boolean isValueX)
	{
		if (this.customLabelFormatter != null)
		{
			String label = this.customLabelFormatter.formatLabel(value, isValueX);
			if (label != null)
			{
				return label;
			}
		}
		
		int i = isValueX ? 1 : 0;
		if (this.numberformatter[i] == null)
		{
			this.numberformatter[i] = NumberFormat.getNumberInstance();
			double highestvalue = isValueX ? this.getMaxX(false) : this.getMaxY();
			double lowestvalue = isValueX ? this.getMinX(false) : this.getMinY();
			if (highestvalue - lowestvalue < 0.1)
			{
				this.numberformatter[i].setMaximumFractionDigits(6);
			}
			else if (highestvalue - lowestvalue < 1)
			{
				this.numberformatter[i].setMaximumFractionDigits(4);
			}
			else if (highestvalue - lowestvalue < 20)
			{
				this.numberformatter[i].setMaximumFractionDigits(3);
			}
			else if (highestvalue - lowestvalue < 100)
			{
				this.numberformatter[i].setMaximumFractionDigits(1);
			}
			else
			{
				this.numberformatter[i].setMaximumFractionDigits(0);
			}
		}
		return this.numberformatter[i].format(value);
	}

	private String[] generateHorlabels(float graphwidth)
	{
		int numLabels = this.getGraphViewStyle().getNumHorizontalLabels() - 1;
		if (numLabels < 0)
		{
			numLabels = (int) (graphwidth / (this.horLabelTextWidth * 2));
		}

		String[] labels = new String[numLabels + 1];

		double min = this.getMinX(false);
		double max = this.getMaxX(false);
		
		for (int i = 0; i <= numLabels; i++)
		{
			if (this.verticalLinesValues != null && this.verticalLinesValues.length > 0 && i < this.verticalLinesValues.length)
			{
				labels[i] = this.formatLabel(this.verticalLinesValues[i], true);
			}
		}
		
		return labels;
	}

	synchronized private String[] generateVerlabels(float graphheight)
	{
		int numLabels = this.getGraphViewStyle().getNumVerticalLabels() - 1;
		if (numLabels < 0)
		{
			numLabels = (int) (graphheight / (this.labelTextHeight * 3));
			numLabels = Math.abs(numLabels);
			if (numLabels == 0)
			{
				Log.w("GraphView", "Height of Graph is smaller than the label text height, so no vertical labels were shown!");
			}
		}
		
		String[] labels = new String[numLabels + 1];
		double min = this.getMinY();
		double max = this.getMaxY();
		if (max == min)
		{
			// if min/max is the same, fake it so that we can render a line
			if (max == 0)
			{
				// if both are zero, change the values to prevent division by
				// zero
				max = 1.0d;
				min = 0.0d;
			}
			else
			{
				max = max * 1.05d;
				min = min * 0.95d;
			}
		}

		for (int i = 0; i <= numLabels; i++)
		{
			labels[numLabels - i] = this.formatLabel(min + ((max - min) * i / numLabels), false);
		}
		
		return labels;
	}

	/**
	 * @return the custom label formatter, if there is one. otherwise null
	 */
	public CustomLabelFormatter getCustomLabelFormatter()
	{
		return this.customLabelFormatter;
	}

	/**
	 * @return the graphview style. it will never be null.
	 */
	public GraphViewStyle getGraphViewStyle()
	{
		return this.graphViewStyle;
	}

	/**
	 * get the position of the legend
	 * 
	 * @return
	 */
	public LegendAlign getLegendAlign()
	{
		return this.legendAlign;
	}

	/**
	 * @return legend width
	 * @deprecated use {@link GraphViewStyle#getLegendWidth()}
	 */
	@Deprecated
	public float getLegendWidth()
	{
		return this.getGraphViewStyle().getLegendWidth();
	}

	/**
	 * returns the maximal X value of the current viewport (if viewport is set)
	 * otherwise maximal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're
	 *            doing!
	 */
	public double getMaxX(boolean ignoreViewport)
	{
		// if viewport is set, use this
		if (!ignoreViewport && this.viewportSize != 0)
		{
			return this.viewportStart + this.viewportSize;
		}
		else
		{
			// otherwise use the max x value
			// values must be sorted by x, so the last value has the largest X
			// value
			double highest = 0;
			if (this.graphSeries.size() > 0)
			{
				GraphViewDataInterface[] values = this.graphSeries.get(0).values;
				if (values.length == 0)
				{
					highest = 0;
				}
				else
				{
					highest = values[values.length - 1].getX();
				}
				for (int i = 1; i < this.graphSeries.size(); i++)
				{
					values = this.graphSeries.get(i).values;
					if (values.length > 0)
					{
						highest = Math.max(highest, values[values.length - 1].getX());
					}
				}
			}
			return highest;
		}
	}

	/**
	 * returns the maximal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	public double getMaxY()
	{
		double largest;
		if (this.manualYAxis)
		{
			largest = this.manualMaxYValue;
		}
		else
		{
			largest = Integer.MIN_VALUE;
			for (int i = 0; i < this.graphSeries.size(); i++)
			{
				GraphViewDataInterface[] values = this._values(i);
				for (GraphViewDataInterface value : values)
				{
					if (value.getY() > largest)
					{
						largest = value.getY();
					}
				}
			}
		}
		return largest;
	}

	/**
	 * returns the minimal X value of the current viewport (if viewport is set)
	 * otherwise minimal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're
	 *            doing!
	 */
	public double getMinX(boolean ignoreViewport)
	{
		// if viewport is set, use this
		if (!ignoreViewport && this.viewportSize != 0)
		{
			return this.viewportStart;
		}
		else
		{
			// otherwise use the min x value
			// values must be sorted by x, so the first value has the smallest X
			// value
			double lowest = 0;
			if (this.graphSeries.size() > 0)
			{
				GraphViewDataInterface[] values = this.graphSeries.get(0).values;
				if (values.length == 0)
				{
					lowest = 0;
				}
				else
				{
					lowest = values[0].getX();
				}
				for (int i = 1; i < this.graphSeries.size(); i++)
				{
					values = this.graphSeries.get(i).values;
					if (values.length > 0)
					{
						lowest = Math.min(lowest, values[0].getX());
					}
				}
			}
			return lowest;
		}
	}

	/**
	 * returns the minimal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	public double getMinY()
	{
		double smallest;
		if (this.manualYAxis)
		{
			smallest = this.manualMinYValue;
		}
		else
		{
			smallest = Integer.MAX_VALUE;
			for (int i = 0; i < this.graphSeries.size(); i++)
			{
				GraphViewDataInterface[] values = this._values(i);
				for (GraphViewDataInterface value : values)
				{
					if (value.getY() < smallest)
					{
						smallest = value.getY();
					}
				}
			}
		}
		return smallest;
	}

	public boolean isDisableTouch()
	{
		return this.disableTouch;
	}

	public boolean isScrollable()
	{
		return this.scrollable;
	}

	public boolean isShowLegend()
	{
		return this.showLegend;
	}

	/**
	 * forces graphview to invalide all views and caches. Normally there is no
	 * need to call this manually.
	 */
	public void redrawAll()
	{
		if (!this.staticVerticalLabels)
		{
			this.verlabels = null;
		}
		if (!this.staticHorizontalLabels)
		{
			this.horlabels = null;
		}
		this.numberformatter[0] = null;
		this.numberformatter[1] = null;
		this.labelTextHeight = null;
		this.horLabelTextWidth = null;
		this.verLabelTextWidth = null;

		this.invalidate();
		this.viewVerLabels.invalidate();
		this.graphViewContentView.invalidate();
	}

	/**
	 * removes all series
	 */
	public void removeAllSeries()
	{
		for (GraphViewSeries s : this.graphSeries)
		{
			s.removeGraphView(this);
		}
		while (!this.graphSeries.isEmpty())
		{
			this.graphSeries.remove(0);
		}
		this.redrawAll();
	}

	/**
	 * removes a series
	 * 
	 * @param series
	 *            series to remove
	 */
	public void removeSeries(GraphViewSeries series)
	{
		series.removeGraphView(this);
		this.graphSeries.remove(series);
		this.redrawAll();
	}

	/**
	 * removes series
	 * 
	 * @param index
	 */
	public void removeSeries(int index)
	{
		if (index < 0 || index >= this.graphSeries.size())
		{
			throw new IndexOutOfBoundsException("No series at index " + index);
		}

		this.removeSeries(this.graphSeries.get(index));
	}

	/**
	 * scrolls to the last x-value
	 * 
	 * @throws IllegalStateException
	 *             if scrollable == false
	 */
	public void scrollToEnd()
	{
		if (!this.scrollable)
		{
			throw new IllegalStateException("This GraphView is not scrollable.");
		}
		double max = this.getMaxX(true);
		this.viewportStart = max - this.viewportSize;

		// don't clear labels width/height cache
		// so that the display is not flickering
		if (!this.staticVerticalLabels)
		{
			this.verlabels = null;
		}
		if (!this.staticHorizontalLabels)
		{
			this.horlabels = null;
		}

		this.invalidate();
		this.viewVerLabels.invalidate();
		this.graphViewContentView.invalidate();
	}

	/**
	 * set a custom label formatter
	 * 
	 * @param customLabelFormatter
	 */
	public void setCustomLabelFormatter(CustomLabelFormatter customLabelFormatter)
	{
		this.customLabelFormatter = customLabelFormatter;
	}

	/**
	 * The user can disable any touch gestures, this is useful if you are using
	 * a real time graph, but don't want the user to interact
	 * 
	 * @param disableTouch
	 */
	public void setDisableTouch(boolean disableTouch)
	{
		this.disableTouch = disableTouch;
	}

	/**
	 * set custom graphview style
	 * 
	 * @param style
	 */
	public void setGraphViewStyle(GraphViewStyle style)
	{
		this.graphViewStyle = style;
		this.labelTextHeight = null;
	}

	/**
	 * set's static horizontal labels (from left to right)
	 * 
	 * @param horlabels
	 *            if null, labels were generated automatically
	 */
	public void setHorizontalLabels(String[] horlabels)
	{
		this.staticHorizontalLabels = horlabels != null;
		this.horlabels = horlabels;
	}

	/**
	 * legend position
	 * 
	 * @param legendAlign
	 */
	public void setLegendAlign(LegendAlign legendAlign)
	{
		this.legendAlign = legendAlign;
	}

	/**
	 * legend width
	 * 
	 * @param legendWidth
	 * @deprecated use {@link GraphViewStyle#setLegendWidth(int)}
	 */
	@Deprecated
	public void setLegendWidth(float legendWidth)
	{
		this.getGraphViewStyle().setLegendWidth((int) legendWidth);
	}

	/**
	 * you have to set the bounds {@link #setManualYAxisBounds(double, double)}.
	 * That automatically enables manualYAxis-flag. if you want to disable the
	 * menual y axis, call this method with false.
	 * 
	 * @param manualYAxis
	 */
	public void setManualYAxis(boolean manualYAxis)
	{
		this.manualYAxis = manualYAxis;
	}

	/**
	 * set manual Y axis limit
	 * 
	 * @param max
	 * @param min
	 */
	public void setManualYAxisBounds(double max, double min)
	{
		this.manualMaxYValue = max;
		this.manualMinYValue = min;
		this.manualYAxis = true;
	}

	/**
	 * this forces scrollable = true
	 * 
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable)
	{
		this.scalable = scalable;
		if (scalable == true && this.scaleDetector == null)
		{
			this.scrollable = true; // automatically forces this
			this.scaleDetector = new ScaleGestureDetector(this.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener()
			{
				@Override
				public boolean onScale(ScaleGestureDetector detector)
				{
					double center = GraphView.this.viewportStart + GraphView.this.viewportSize / 2;
					GraphView.this.viewportSize /= detector.getScaleFactor();
					GraphView.this.viewportStart = center - GraphView.this.viewportSize / 2;

					// viewportStart must not be < minX
					double minX = GraphView.this.getMinX(true);
					if (GraphView.this.viewportStart < minX)
					{
						GraphView.this.viewportStart = minX;
					}

					// viewportStart + viewportSize must not be > maxX
					double maxX = GraphView.this.getMaxX(true);
					if (GraphView.this.viewportSize == 0)
					{
						GraphView.this.viewportSize = maxX;
					}
					double overlap = GraphView.this.viewportStart + GraphView.this.viewportSize - maxX;
					if (overlap > 0)
					{
						// scroll left
						if (GraphView.this.viewportStart - overlap > minX)
						{
							GraphView.this.viewportStart -= overlap;
						}
						else
						{
							// maximal scale
							GraphView.this.viewportStart = minX;
							GraphView.this.viewportSize = maxX - GraphView.this.viewportStart;
						}
					}
					GraphView.this.redrawAll();
					return true;
				}
			});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you
	 * use a viewport {@link #setViewPort(double, double)} which doesn't
	 * displays all data.
	 * 
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable)
	{
		this.scrollable = scrollable;
	}

	public void setShowLegend(boolean showLegend)
	{
		this.showLegend = showLegend;
	}

	/**
	 * sets the title of graphview
	 * 
	 * @param title
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * set's static vertical labels (from top to bottom)
	 * 
	 * @param verlabels
	 *            if null, labels were generated automatically
	 */
	public void setVerticalLabels(String[] verlabels)
	{
		this.staticVerticalLabels = verlabels != null;
		this.verlabels = verlabels;
	}

	public double getViewPortSize()
	{
		return this.viewportSize;
	}
	
	/**
	 * set's the viewport for the graph.
	 * 
	 * @see #setManualYAxisBounds(double, double) to limit the y-viewport
	 * @param start
	 *            x-value
	 * @param size
	 */
	public void setViewPort(double start, double size)
	{
		if (size < 0)
		{
			throw new IllegalArgumentException("Viewport size must be greater than 0!");
		}
		this.viewportStart = start;
		this.viewportSize = size;
	}
}
