package com.jtalics.n3mo;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

public class Constants {
	static final double PI2 = Math.PI * 2;
	public static final double HoursPerDay = 24.0; 
	public static final double MinutesPerDay = HoursPerDay * 60.0;
	public static final double SecondsPerDay = 60.0 * MinutesPerDay;
	static final double HalfSecond = 0.5 / SecondsPerDay;
	static final double EarthRadius = 6378.16; /* Kilometers */
	static final double C = 2.997925e5; /* Kilometers/Second */
	static final double TropicalYear = 365.24199; /* Mean solar days */
	static final double EarthEccentricity = 0.016713;
	public static final double DegreesPerRadian = 180.0 / Math.PI;
	public static final double RadiansPerDegree = Math.PI / 180.0;
	static final String DayNames[] = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	static final int MonthDays[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
	static String VersionStr = "N3EMO Orbit Simulator  v3.7";
	static final double GM = 398600; /* Kilometers^3/seconds^2 G=Newton's universal gravitational constant, M is mass of earth */ 
	static final double EarthFlat = (1 / 298.25); /* Earth Flattening Coeff. */
	static final ZonedDateTime epoch1900 = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	/*
	 * If non zero, use ellipsoidal earth model when calculating longitude,
	 * latitude, and height
	 */
	static final boolean SSPELLIPSE = false;
	static List<String> getLines(File file) throws FileNotFoundException {
		List<String> list = new ArrayList<>();
		Scanner sc = new Scanner(file);
		while (sc.hasNext()) {
			list.add(sc.nextLine());
		}
		sc.close();
		return list;
	}

	
	public static int[] getDate(int DayNum) {
/**
		int M, L;
		long Y, retval[] = new long[3];

		Y = 4 * DayNum;
		Y /= 1461;

		DayNum = DayNum - 365 - (((Y - 1) * 1461) >> 2);

		L = 0;
		if (Y % 4 == 0 && DayNum > MonthDays[2])
			L = 1;

		M = 1;

		while (DayNum > MonthDays[M] + L)
			M++;

		DayNum -= (MonthDays[M - 1]);
		if (M > 2)
			DayNum -= L;

		retval[0] = Y + 1900;
		retval[1] = M;
		retval[2] = DayNum;
		return retval;
**/
		ZonedDateTime plus = epoch1900.plusDays(DayNum);
		int[] retval = new int[3];
		retval[0] = plus.getYear();
		retval[1] = plus.getMonthValue();
		retval[2] = plus.getDayOfMonth();
		return retval;
	}

	public static double getDurationInDaysSinceEpoch1900(int[] dateTime) {
		ZonedDateTime date = ZonedDateTime.of(dateTime[0], dateTime[1], dateTime[2], dateTime[3], dateTime[4], dateTime[5], dateTime[6], ZoneOffset.UTC);
		return java.time.temporal.ChronoUnit.DAYS.between(epoch1900,date);
	}

	
	public static int[] getDateTime(double durationInDaysSince1900) {
/**
		int M, L;
		long Y, retval[] = new long[3];

		Y = 4 * DayNum;
		Y /= 1461;

		DayNum = DayNum - 365 - (((Y - 1) * 1461) >> 2);

		L = 0;
		if (Y % 4 == 0 && DayNum > MonthDays[2])
			L = 1;

		M = 1;

		while (DayNum > MonthDays[M] + L)
			M++;

		DayNum -= (MonthDays[M - 1]);
		if (M > 2)
			DayNum -= L;

		retval[0] = Y + 1900;
		retval[1] = M;
		retval[2] = DayNum;
		return retval;
**/
		ZonedDateTime plus = epoch1900.plus((long)(durationInDaysSince1900*Constants.SecondsPerDay),ChronoUnit.SECONDS);
		int[] retval = new int[7];
		retval[0] = plus.getYear();
		retval[1] = plus.getMonthValue();
		retval[2] = plus.getDayOfMonth();
		retval[3] = plus.getHour();
		retval[4] = plus.getMinute();
		retval[5] = plus.getSecond();
		retval[6] = plus.getNano();
		return retval;
	}

	/* get number of days since 1900 started, ignoring remainder */
	static long getDayCountSince1900(int Year, int Month, int Day) {

		ZonedDateTime date = ZonedDateTime.of(Year, Month, Day, 0, 0, 0, 0, ZoneOffset.UTC);
		return java.time.temporal.ChronoUnit.DAYS.between(epoch1900,date);
		/* Heuristic to allow 4 or 2 digit year specifications
		if (Year < 50) {
			Year += 2000;
		}
		else if (Year < 100) {
			Year += 1900;
		}
		long retval = ((((long) Year - 1901) * 1461) >> 2) + MonthDays[Month - 1] + Day + 365;
		if (Year % 4 == 0 && Month > 2) {
			retval++;
		}

		return retval;
		*/
	}

	static int letterNum(char c) throws Exception {
		if (c >= 'a' && c <= 'z') {
			return c - 'a' + 1;
		} else if (c >= 'A' && c <= 'Z') {
			return c - 'A' + 27;
		} else if (c >= '0' && c <= '9') {
			return c - '0' + 53;
		}
		throw new Exception();
	}

	public static String printTime(double time) {

		int day, hours, minutes, seconds;

		day = (int) time;
		time -= day;
		if (time < 0)
			time += 1.0; /* Correct for truncation problems with negatives */

		hours = (int) (time * 24);
		time -= hours / 24.0;

		minutes = (int) (time * Constants.MinutesPerDay);
		time -= minutes / Constants.MinutesPerDay;

		seconds = (int) (time * Constants.SecondsPerDay);
		seconds -= seconds / Constants.SecondsPerDay;

		return "" + hours + ":" + minutes + ":" + seconds;
	}


	public static int[] parseStandardDateTimeFormattedString(String text) {
		// TODO validate text
		String[] s = text.split(":");
		String[] s2 = s[5].split("\\.");
		int[] retval = new int[7];
		for (int i=0; i<4; i++) {
			retval[i]=Integer.parseInt(s[i]);
		}
		retval[5] = Integer.parseInt(s2[0]); // seconds
		retval[6] = (int)(1000000000.0*Double.parseDouble("0."+s2[1])); // nano seconds

		return retval;
	}


	public static String makeStandardDateTimeFormattedString(int[] dateTime) {
		return dateTime[0] + ":" + dateTime[1] + ":" + dateTime[2] + ":" + dateTime[3] + ":" 
				+ dateTime[4] + ":" + dateTime[5]+"."+dateTime[6]; // TODO fix nano
	}
}

// #define ABS(x) ((x) < 0 ? (-(x)) : (x))
// #define SQR(x) ((x)*(x))