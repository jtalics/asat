package n3mo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Constants {
	static final double PI2 = Math.PI * 2;
	static final double MinutesPerDay = 24.0 * 60.0;
	static final double SecondsPerDay = 60.0 * MinutesPerDay;
	static final double HalfSecond = 0.5 / SecondsPerDay;
	static final double EarthRadius = 6378.16; /* Kilometers */
	static final double C = 2.997925e5; /* Kilometers/Second */
	static final double TropicalYear = 365.24199; /* Mean solar days */
	static final double EarthEccentricity = 0.016713;
	static final double DegreesPerRadian = 180.0 / Math.PI;
	static final double RadiansPerDegree = Math.PI / 180.0;
	static final String DayNames[] = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	static final int MonthDays[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
	static String VersionStr = "N3EMO Orbit Simulator  v3.7";
	static final double GM = 398600; /* Kilometers^3/seconds^2 */
	static final double EarthFlat = (1 / 298.25); /* Earth Flattening Coeff. */
	static final boolean SSPELLIPSE = false; /* If non zero, use ellipsoidal earth model when calculating longitude, latitude, and height */


	static List<String> getLines(File file) throws FileNotFoundException {
		List<String> list = new ArrayList<>();
		Scanner sc = new Scanner(file);
		while (sc.hasNext()) {
			list.add(sc.nextLine());
		}
		sc.close();
		return list;
	}


	static long[] GetDate(long DayNum) {
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
	}

	static long GetDayNum(int Year, int Month, int Day) {
		long Result;

		/* Heuristic to allow 4 or 2 digit year specifications */
		if (Year < 50)
			Year += 2000;
		else if (Year < 100)
			Year += 1900;

		Result = ((((long) Year - 1901) * 1461) >> 2) + MonthDays[Month - 1] + Day + 365;
		if (Year % 4 == 0 && Month > 2)
			Result++;

		return Result;
	}

	static int LetterNum(char c) throws Exception {
		if (c >= 'a' && c <= 'z') {
			return c - 'a' + 1;
		} else if (c >= 'A' && c <= 'Z') {
			return c - 'A' + 27;
		} else if (c >= '0' && c <= '9') {
			return c - '0' + 53;
		}
		throw new Exception();
	}
	
	static void PrintTime(PrintStream OutFile, double Time) {
		int day, hours, minutes, seconds;

		day = (int) Time;
		Time -= day;
		if (Time < 0)
			Time += 1.0; /* Correct for truncation problems with negatives */

		hours = (int) (Time * 24);
		Time -= hours / 24.0;

		minutes = (int) (Time * Constants.MinutesPerDay);
		Time -= minutes / Constants.MinutesPerDay;

		seconds = (int) (Time * Constants.SecondsPerDay);
		seconds -= seconds / Constants.SecondsPerDay;

		OutFile.print("" + hours + ":" + minutes + ":" + seconds);
	}


}
// #define ABS(x) ((x) < 0 ? (-(x)) : (x))
// #define SQR(x) ((x)*(x))