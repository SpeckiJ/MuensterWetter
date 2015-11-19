package com.ifgi.klimastationms.datamodel;


public enum GraphRange
{
	ONE_DAY(1, 6, 1, "HH:mm"),
	TWO_DAYS(2, 12, 1, "EE, HH:mm"),
	THREE_DAYS(3, 12, 1, "EE, HH:mm"),
	FIVE_DAYS(5, 24, 1, "EE"),
	SEVEN_DAYS(7, 24, 1, "EE"),
	TEN_DAYS(10, 24, 2, "dd. MMM"),
	TWELVE_DAYS(12, 24, 2, "dd. MMM"),
	FOURTEEN_DAYS(14, 24, 3, "dd. MMM"),
	SEVENTEEN_DAYS(17, 24, 3, "dd. MMM"),
	TWENTY_DAYS(20, 24, 4, "dd. MMM");

	private GraphRange(int size, int numberOfHoursBetweenSteps, int numberOfDaysBetweenSteps, String dateFormatPattern)
	{
		this.length = Integer.valueOf(size * 6 * 24);
		this.numberOfRanges = 10;
		this.significantDistanceHours = numberOfHoursBetweenSteps;
		this.significantDistanceDays = numberOfDaysBetweenSteps;
		this.dateFormatPattern = dateFormatPattern;
	}

	public final Integer length;
	public final int numberOfRanges;
	public final String dateFormatPattern;
	public final long significantDistanceHours;	
	public final long significantDistanceDays;	
}
