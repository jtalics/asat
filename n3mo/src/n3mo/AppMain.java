package n3mo;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.stream.Stream;

public class AppMain {
	private static final boolean SSPELLIPSE = false; /* If non zero, use ellipsoidal earth model when calculating
														longitude, latitude, and height */

	static final double MinutesPerDay = 24 * 60.0;
	static final double SecondsPerDay = 60 * MinutesPerDay;
	static final double HalfSecond = 0.5 / SecondsPerDay;
	static final double EarthRadius = 6378.16; /* Kilometers */
	static final double C = 2.997925e5; /* Kilometers/Second */
	static final double TropicalYear = 365.24199; /* Mean solar days */
	static final double EarthEccentricity = 0.016713;
	static final double DegreesPerRadian = 180 / Math.PI;
	static final double RadiansPerDegree = Math.PI / 180;
	static final double PI2 = Math.PI * Math.PI;
	// #define ABS(x) ((x) < 0 ? (-(x)) : (x))
	// #define SQR(x) ((x)*(x))

	static final int MaxModes = 10;

	class ModeRec {
		int MinPhase, MaxPhase;
		String ModeStr;
	}

	private static boolean NOCONSOLE = false;

	String VersionStr = "N3EMO Orbit Simulator  v3.7";

	/* Keplerian Elements and misc. data for the satellite */
	double EpochDay; /* time of epoch */
	double EpochMeanAnomaly; /* Mean Anomaly at epoch */
	long EpochOrbitNum; /* Integer orbit # of epoch */
	double EpochRAAN; /* RAAN at epoch */
	double epochMeanMotion; /* Revolutions/day */
	double OrbitalDecay; /* Revolutions/day^2 */
	double EpochArgPerigee; /* argument of perigee at epoch */
	double Eccentricity;
	double Inclination;
	String SatName = "f";
	int ElementSet;
	double BeaconFreq; /* Mhz, used for doppler calc */
	double MaxPhase; /* Phase units in 1 orbit */
	double perigeePhase;
	int NumModes;
	ModeRec[] Modes = new ModeRec[MaxModes];
	boolean PrintApogee;
	boolean PrintEclipses;
	boolean Flip;
	final String DayNames[] = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	final int MonthDays[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
	/* Keplerian elements for the sun */
	double SunEpochTime, SunInclination, SunRAAN, SunEccentricity, SunArgPerigee, SunMeanAnomaly, SunMeanMotion;

	private final static double GM = 398600; /* Kilometers^3/seconds^2 */
	private final static double SunSemiMajorAxis = 149598845.0; /* Kilometers */
	private final static double SunRadius = 695000;
	private final static double Epsilon = RadiansPerDegree / 3600; /* 1 arc second */
	private final static double SiderealSolar = 1.0027379093;
	private final static double SidRate = (PI2 * SiderealSolar / SecondsPerDay); /* radians/second */

	double SidDay, SidReference; /* Date and sidereal time */
	/* values for shadow geometry */
	double SinPenumbra, CosPenumbra;
	private final static double EarthFlat = (1 / 298.25); /* Earth Flattening Coeff. */

	/* Simulation Parameters */

	double StartTime, EndTime, StepTime; /* In Days, 1 = New Year of reference year */

	/* Site Parameters */
	String SiteName;
	double SiteLat, SiteLong, SiteAltitude, SiteMinElev;

	public static void main(String[] args) throws Exception {

		if (System.console() == null) {
			NOCONSOLE = true; // probably running in IDE or debugger
			// throw new Exception("Cannot open a console");
		}

		AppMain appMain = new AppMain();
		appMain.mainC();
	}

	private void mainC() throws Exception {

		double ReferenceOrbit; /* Floating point orbit # at epoch */
		double CurrentTime, TmpTime, PrevTime; /* In Days */
		double CurrentOrbit;
		double AverageMotion, /* Corrected for drag */
				CurrentMotion;
		double MeanAnomaly, TrueAnomaly;
		double SemiMajorAxis;
		double SiteMatrix[][] = new double[3][3];
		// double RAANPrecession,PerigeePrecession;
		// double SSPLat,SSPLong;
		long OrbitNum, PrevOrbitNum;
		long Day, PrevDay;
		double Doppler;
		int Phase;
		String FileName = null;
		PrintStream OutFile;
		boolean DidApogee;
		boolean PrevVisible = false;

		System.out.println(VersionStr);

		GetSatelliteParams();
		GetSiteParams();
		GetSimulationParams();

		InitOrbitRoutines((StartTime + EndTime) / 2);

		if (!NOCONSOLE) {
			FileName = System.console().readLine("Output file (RETURN for TTY): ");
		}
		if (FileName == null || FileName.length() > 0) {
			File file = new File(FileName, "w");
			OutFile = new PrintStream(file);
		} else
			OutFile = System.out;

		OutFile.println(SatName + " Element Set " + ElementSet);

		OutFile.println(SiteName);

		OutFile.println("Doppler calculated for freq = " + BeaconFreq);

		SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(MinutesPerDay / epochMeanMotion) / 3);
		double[] prec = GetPrecession(SemiMajorAxis, Eccentricity, Inclination);

		ReferenceOrbit = EpochMeanAnomaly / (Math.PI * 2) + EpochOrbitNum;

		PrevDay = -10000;
		PrevOrbitNum = -10000;
		PrevTime = StartTime - 2 * StepTime;

		BeaconFreq *= 1E6; /* Convert to Hz */

		DidApogee = false;

		for (CurrentTime = StartTime; CurrentTime <= EndTime; CurrentTime += StepTime) {

			AverageMotion = epochMeanMotion + (CurrentTime - EpochDay) * OrbitalDecay / 2;
			CurrentMotion = epochMeanMotion + (CurrentTime - EpochDay) * OrbitalDecay;

			SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(MinutesPerDay / CurrentMotion) / 3);

			CurrentOrbit = ReferenceOrbit + (CurrentTime - EpochDay) * AverageMotion;
			OrbitNum = (long) CurrentOrbit;

			MeanAnomaly = (CurrentOrbit - OrbitNum) * Math.PI * 2;

			TmpTime = CurrentTime;
			if (MeanAnomaly < Math.PI) {
				DidApogee = false;
			}
			if (PrintApogee && !DidApogee && MeanAnomaly > Math.PI) {
				/* Calculate Apogee */
				TmpTime -= StepTime; /* So we pick up later where we left off */
				MeanAnomaly = Math.PI;
				CurrentTime = EpochDay + (OrbitNum - ReferenceOrbit + 0.5) / AverageMotion;
			}

			TrueAnomaly = Kepler(MeanAnomaly, Eccentricity);
			// double Radius; /* From geocenter */
			// double SatX, SatY, SatZ; /* In Right Ascension based system */
			// double SatVX, SatVY, SatVZ; /* Kilometers/second */

			double[] sat = GetSatPosition(EpochDay, EpochRAAN, EpochArgPerigee, SemiMajorAxis, Inclination,
					Eccentricity, prec[0], prec[1], CurrentTime, TrueAnomaly); // ,&SatX,&SatY,&SatZ,&Radius,
																				// &SatVX,&SatVY,&SatVZ

			double[] site = GetSitPosition(SiteLat, SiteLong, SiteAltitude, CurrentTime, SiteMatrix); // &SiteX,&SiteY,&SiteZ,&SiteVX,&SiteVY,

			double[] bearings = GetBearings(sat[0], sat[1], sat[2], site[0], site[2], site[3], SiteMatrix); // &Azimuth,&Elevation

			if (bearings[1] >= SiteMinElev && CurrentTime >= StartTime) {

				Day = (long) (CurrentTime + HalfSecond);
				if (((double) Day) > CurrentTime + HalfSecond)
					Day -= 1; /* Correct for truncation of negative values */

				if (OrbitNum == PrevOrbitNum && Day == PrevDay && !PrevVisible) { // TODO? is prevVisible init'd ?
					OutFile.println(); /* Dipped out of sight; print blank */
				}
				if (OrbitNum != PrevOrbitNum || Day != PrevDay) {
					/* Print Header */
					OutFile.print(DayNames[(int) (Day % 7)]);
					OutFile.print(" ");
					long[] date = GetDate(Day);
					OutFile.print(date[2] + " " + date[1] + " " + date[0]);
					OutFile.println("  ----Orbit # " + OrbitNum + "-----");
					OutFile.print(" U.T.C.   Az  El  ");
					if (Flip) {
						OutFile.print(" Az'  El' ");
					}
					OutFile.print("Doppler Range");
					OutFile.print(" Height  Lat  Long  Phase(" + MaxPhase + ")");
				}
				PrevOrbitNum = OrbitNum;
				PrevDay = Day;
				PrintTime(OutFile, CurrentTime + HalfSecond);

				OutFile.print("  " + bearings[0] * DegreesPerRadian + " " + bearings[1] * DegreesPerRadian);
				if (Flip) {
					bearings[0] += Math.PI;
					if (bearings[0] >= Math.PI * 2) {
						bearings[0] -= Math.PI * 2;
					}
					bearings[1] = Math.PI - bearings[1];
					OutFile.print("  " + bearings[0] * DegreesPerRadian + "  " + bearings[1] * DegreesPerRadian);
				}

				double[] range = GetRange(site[0], site[1], site[2], site[3], site[4], sat[0], sat[1], sat[2], sat[3],
						sat[4], sat[5]); // Range,RangeRate

				Doppler = -BeaconFreq * range[1] / C;
				OutFile.print("  " + Doppler + " " + range[0]);

				double[] ssp = GetSubSatPoint(sat[0], sat[1], sat[2], CurrentTime); // ,&SSPLat,&SSPLong,&Height
				OutFile.print(" " + ssp[2] + "  " + ssp[0] * DegreesPerRadian + "  " + ssp[1] * DegreesPerRadian);

				Phase = (int) (MeanAnomaly / (Math.PI * 2) * MaxPhase + perigeePhase);
				while (Phase < 0) {
					Phase += MaxPhase;
				}
				while (Phase >= MaxPhase) {
					Phase -= MaxPhase;
				}

				OutFile.print(" " + Phase + "  ");
				PrintMode(OutFile, Phase);

				if (PrintApogee && (MeanAnomaly == Math.PI)) {
					OutFile.print("    Apogee");
				}
				if (PrintEclipses && Eclipsed(sat[0], sat[1], sat[2], sat[3], CurrentTime)) {
					OutFile.println("  Eclipse");
				}
				OutFile.println();
				PrevVisible = true;
			} else {
				PrevVisible = false;
			}
			if (PrintApogee && (MeanAnomaly == Math.PI)) {
				DidApogee = true;
			}

			PrevTime = CurrentTime;
			CurrentTime = TmpTime;
		}
		OutFile.close();
	}

	private void GetSatelliteParams() throws Exception {

		String line, token;
		int EpochYear;
		boolean found;
		int i, NumSatellites;
		char satchar;

		NumSatellites = ListSatellites();

		List<String> lines = getLines(new File("kepler.dat"));
		Iterator<String> iter = lines.iterator();
		found = false;
		// Use the number to get the satellite name
		while (!found) {
			if (!NOCONSOLE) {
				try {
					SatName = System.console().readLine("Letter or satellite name :");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Validate number
			if (SatName.length() == 1) { /* use single character label */
				satchar = SatName.charAt(0);

				if (LetterNum(satchar) > NumSatellites) {
					System.out.println("'" + satchar + "' is out of range");

					continue;
				}
				// Convert number to satellite name
				i = 0;
				while (iter.hasNext()) {
					line = iter.next();
					if (line.startsWith("Satellite: ")) {
						i++;
						if (++i == LetterNum(satchar)) {
							found = true;
							SatName = line.substring(11);
							break;
						}
					}
				}
			} else {
				while (!found) { /* use satellite name */
					while (iter.hasNext()) {
						line = iter.next();
						if (line.startsWith("Satellite: ") && line.endsWith(SatName)) {
							found = true;
							break;
						}
					}
				}
			}

			if (!found) {
				System.out.println("Satellite not found:" + SatName);
				// TODO: InFile.close();?
			}
		}

		BeaconFreq = 146.0; /* Default value */

		line = iter.next(); // skip catalog

		token = "Epoch time:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochDay = Double.parseDouble(token.substring(token.length()));
		EpochYear = (int) (EpochDay / 1000.0);
		EpochDay -= EpochYear * 1000.0;
		EpochDay += GetDayNum(EpochYear, 1, 0);

		// TODO? if (sscanf(str,"Element set: %ld",&ElementSet) == 0)
		// { /* Old style kepler.dat */
		line = iter.next();
		token = "Element set:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		ElementSet = Integer.parseInt(token.substring(token.length()));

		line = iter.next();
		token = "Inclination:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		Inclination = Double.parseDouble(token.substring(token.length()));
		Inclination *= RadiansPerDegree;

		line = iter.next();
		token = "RA of node:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochRAAN = Double.parseDouble(token.substring(token.length()));
		EpochRAAN *= RadiansPerDegree;

		line = iter.next();
		token = "Eccentricity:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		Eccentricity = Double.parseDouble(token.substring(token.length()));

		line = iter.next();
		token = "Arg of perigee:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochArgPerigee = Double.parseDouble(token.substring(token.length()));
		EpochArgPerigee *= RadiansPerDegree;

		line = iter.next();
		token = "Mean anomaly:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochMeanAnomaly = Double.parseDouble(token.substring(token.length()));
		EpochMeanAnomaly *= RadiansPerDegree;

		line = iter.next();
		token = "Mean motion:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		epochMeanMotion = Double.parseDouble(token.substring(token.length()));

		line = iter.next();
		token = "Decay rate:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		OrbitalDecay = Double.parseDouble(token.substring(token.length()));

		line = iter.next();
		token = "Epoch rev:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochOrbitNum = Integer.parseInt(token.substring(token.length()));

		while (iter.hasNext() && (line = iter.next()).length() > 2) {
			token = "Beacon:";
			if (!line.startsWith(token)) {
				throw new Exception();
			}
			BeaconFreq = Double.parseDouble(token.substring(token.length()));
		}

		PrintApogee = (Eccentricity >= 0.3);
		perigeePhase = 0;
		MaxPhase = 256; /* Default values */
		NumModes = 0;

		lines = getLines(new File("mode.dat"));

		found = true;
		while (iter.hasNext()) {
			token = "Satellite: ";
			line = iter.next();
			if (line.startsWith(token) && line.endsWith(SatName)) {
				found = false;
			}
		}

		if (found) {
			while (iter.hasNext() && (line = iter.next()).length() > 2) {
				line = iter.next();
				token = "Beacon: ";
				if (!line.startsWith(token)) {
					throw new Exception();
				}
				BeaconFreq = Double.parseDouble(token.substring(token.length()));

				line = iter.next();
				token = "Perigee phase: ";
				if (!line.startsWith(token)) {
					throw new Exception();
				}
				perigeePhase = Double.parseDouble(token.substring(token.length()));

				line = iter.next();
				token = "Max phase: ";
				if (!line.startsWith(token)) {
					throw new Exception();
				}
				MaxPhase = Double.parseDouble(token.substring(token.length()));

				line = iter.next();
				Scanner scanner = new Scanner(line);
				token = "Mode: ";

				if (!line.startsWith(token)) {
					scanner.close();
					throw new Exception();
				}
				Modes[NumModes].ModeStr = line.substring(token.length(), token.length() + 20);
				Modes[NumModes].MinPhase = scanner.nextInt();
				Modes[NumModes].MaxPhase = scanner.nextInt();
				scanner.close();
				if (NumModes >= MaxModes) {
					throw new Exception();
				}
				NumModes++;
			}
		}
	}

	private List<String> getLines(File file) throws FileNotFoundException {
		List<String> list = new ArrayList<>();
		Scanner sc = new Scanner(file);
		while (sc.hasNext()) {
			list.add(sc.nextLine());
		}
		sc.close();
		return list;
	}

	int LetterNum(char c) throws Exception {
		if (c >= 'a' && c <= 'z') {
			return c - 'a' + 1;
		} else if (c >= 'A' && c <= 'Z') {
			return c - 'A' + 27;
		} else if (c >= '0' && c <= '9') {
			return c - '0' + 53;
		}
		throw new Exception();
	}

	/* List the satellites in kepler.dat, and return the number found */
	int ListSatellites() throws IOException {
		char satchar;
		int NumSatellites;

		System.out.println("Available satellites:");

		satchar = 'a';
		NumSatellites = 0;
		List<String> lines = getLines(new File("kepler.dat"));
		for (String line : (Iterable<String>) lines::iterator) {
			if (line.startsWith("Satellite: ")) {
				System.out.println("	" + satchar + ") " + line.substring(11));
				if (satchar == 'z') {
					satchar = 'A';
				} else if (satchar == 'Z') {
					satchar = '0';
				} else {
					satchar++;
				}
				NumSatellites++;
			}
		}

		return NumSatellites;
	}

	void GetSiteParams() throws IOException {
		String line;
		String name = "zerobouy.sit";

		if (!NOCONSOLE) {
			name = System.console().readLine("Site name :").trim() + ".sit";
		}

		List<String> lines = getLines(new File(name));
		SiteName = lines.iterator().next();
		line = lines.iterator().next();
		SiteLat = Double.parseDouble(line);
		SiteLat *= RadiansPerDegree;

		line = lines.iterator().next();
		SiteLong = Double.parseDouble(line);
		SiteLong *= RadiansPerDegree;

		line = lines.iterator().next();
		SiteAltitude = Double.parseDouble(line);
		SiteAltitude /= 1000; // meters to km

		line = lines.iterator().next();
		SiteMinElev = Double.parseDouble(line);
		SiteMinElev *= RadiansPerDegree;

		Flip = PrintEclipses = false;
		while (lines.iterator().hasNext()) {
			line = lines.iterator().next();
			if (line.startsWith("Flip")) {
				Flip = true;
			} else if (line.startsWith("Eclipse")) {
				PrintEclipses = true;
			} else
				System.err.println(name + " unknown option: " + line);
		}

	}

	void GetSimulationParams() {
		// double hour, duration;
		int Month, Day, Year;

		String line = "6 21 2017";
		if (!NOCONSOLE) {
			line = System.console().readLine("Start date (UTC) (Month Day Year) :");
		}
		String s[] = line.split(" ");
		Month = Integer.parseInt(s[0]);
		Day = Integer.parseInt(s[1]);
		Year = Integer.parseInt(s[2]);

		StartTime = GetDayNum(Year, Month, Day);
		line = "0";
		if (!NOCONSOLE) {
			line = System.console().readLine("Starting Hour (UTC) :");
		}
		StartTime += Double.parseDouble(line) / 24.0;

		line = "1";
		if (!NOCONSOLE) {
			line = System.console().readLine("Duration (Days) :");
		}
		EndTime = StartTime + Double.parseDouble(line);

		line = "1";
		if (!NOCONSOLE) {
			line = System.console().readLine("Time Step (Minutes) :");
		}
		StepTime = Double.parseDouble(line) / MinutesPerDay;
	}

	long[] GetDate(long DayNum) {
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

	long GetDayNum(int Year, int Month, int Day) {
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

	void PrintMode(PrintStream OutFile, double Phase) {
		int CurMode;

		for (CurMode = 0; CurMode < NumModes; CurMode++) {
			if ((Phase >= Modes[CurMode].MinPhase && Phase < Modes[CurMode].MaxPhase)
					|| ((Modes[CurMode].MinPhase > Modes[CurMode].MaxPhase)
							&& (Phase >= Modes[CurMode].MinPhase || Phase < Modes[CurMode].MaxPhase))) {
				OutFile.print(Modes[CurMode].ModeStr + " ");
			}
		}
	}

	boolean Eclipsed(double SatX, double SatY, double SatZ, double SatRadius, double CurrentTime) {
		double MeanAnomaly, TrueAnomaly;
		// double SunX,SunY,SunZ,SunRad;
		// double vx,vy,vz;
		double CosTheta;

		MeanAnomaly = SunMeanAnomaly + (CurrentTime - SunEpochTime) * SunMeanMotion * PI2;
		TrueAnomaly = Kepler(MeanAnomaly, SunEccentricity);

		double[] sun = GetSatPosition(SunEpochTime, SunRAAN, SunArgPerigee, SunSemiMajorAxis, SunInclination,
				SunEccentricity, 0.0, 0.0, CurrentTime, TrueAnomaly); // sun = {SunX,SunY,SunZ,SunRad,vx,vy,vz}

		CosTheta = (sun[0] * SatX + sun[1] * SatY + sun[3] * SatZ) / (sun[4] * SatRadius) * CosPenumbra
				+ (SatRadius / EarthRadius) * SinPenumbra;

		if (CosTheta < 0)
			if (CosTheta < -Math.sqrt((SatRadius * SatRadius) - (EarthRadius * EarthRadius)) / SatRadius * CosPenumbra
					+ (SatRadius / EarthRadius) * SinPenumbra)

				return true;
		return false;
	}

	/*
	 * Compute the satellite postion and velocity in the RA based coordinate system,
	 * returns array: double[7] {X,Y,Z,Radius,VX,VY,VZ;}
	 */

	double[] GetSatPosition(double EpochTime, double EpochRAAN, double EpochArgPerigee, double SemiMajorAxis,
			double Inclination, double Eccentricity, double RAANPrecession, double PerigeePrecession, double Time,
			double TrueAnomaly) {

		double RAAN, ArgPerigee;

		double Xw, Yw, VXw, VYw; /* In orbital plane */
		double Tmp;
		double Px, Qx, Py, Qy, Pz, Qz; /* Escobal transformation 31 */
		double CosArgPerigee, SinArgPerigee;
		double CosRAAN, SinRAAN, CoSinclination, SinInclination;
		double[] retval = new double[7];

		retval[3] = SemiMajorAxis * (1 - (Eccentricity * Eccentricity)) / (1 + Eccentricity * Math.cos(TrueAnomaly));

		Xw = retval[3] * Math.cos(TrueAnomaly);
		Yw = retval[3] * Math.sin(TrueAnomaly);

		Tmp = Math.sqrt(GM / (SemiMajorAxis * (1 - (Eccentricity * Eccentricity))));

		VXw = -Tmp * Math.sin(TrueAnomaly);
		VYw = Tmp * (Math.cos(TrueAnomaly) + Eccentricity);

		ArgPerigee = EpochArgPerigee + (Time - EpochTime) * PerigeePrecession;
		RAAN = EpochRAAN - (Time - EpochTime) * RAANPrecession;

		CosRAAN = Math.cos(RAAN);
		SinRAAN = Math.sin(RAAN);
		CosArgPerigee = Math.cos(ArgPerigee);
		SinArgPerigee = Math.sin(ArgPerigee);
		CoSinclination = Math.cos(Inclination);
		SinInclination = Math.sin(Inclination);

		Px = CosArgPerigee * CosRAAN - SinArgPerigee * SinRAAN * CoSinclination;
		Py = CosArgPerigee * SinRAAN + SinArgPerigee * CosRAAN * CoSinclination;
		Pz = SinArgPerigee * SinInclination;
		Qx = -SinArgPerigee * CosRAAN - CosArgPerigee * SinRAAN * CoSinclination;
		Qy = -SinArgPerigee * SinRAAN + CosArgPerigee * CosRAAN * CoSinclination;
		Qz = CosArgPerigee * SinInclination;

		retval[0] = Px * Xw + Qx * Yw; /* Escobal, transformation #31 */
		retval[1] = Py * Xw + Qy * Yw;
		retval[2] = Pz * Xw + Qz * Yw;

		retval[4] = Px * VXw + Qx * VYw;
		retval[5] = Py * VXw + Qy * VYw;
		retval[6] = Pz * VXw + Qz * VYw;

		return retval;
	}

	/*
	 * Initialize the Sun's keplerian elements for a given epoch. Formulas are from
	 * "Explanatory Supplement to the Astronomical Ephemeris". Also init the
	 * sidereal reference
	 */

	void InitOrbitRoutines(double EpochDay) {
		double T, T2, T3, Omega;
		int n;
		double SunTrueAnomaly, SunDistance;

		T = (Math.floor(EpochDay) - 0.5) / 36525;
		T2 = T * T;
		T3 = T2 * T;

		SidDay = Math.floor(EpochDay);

		SidReference = (6.6460656 + 2400.051262 * T + 0.00002581 * T2) / 24;
		SidReference -= Math.floor(SidReference);

		/* Omega is used to correct for the nutation and the abberation */
		Omega = (259.18 - 1934.142 * T) * RadiansPerDegree;
		n = (int) (Omega / PI2); // JTAL
		Omega -= n * PI2;

		SunEpochTime = EpochDay;
		SunRAAN = 0;

		SunInclination = (23.452294 - 0.0130125 * T - 0.00000164 * T2 + 0.000000503 * T3 + 0.00256 * Math.cos(Omega))
				* RadiansPerDegree;
		SunEccentricity = (0.01675104 - 0.00004180 * T - 0.000000126 * T2);
		SunArgPerigee = (281.220833 + 1.719175 * T + 0.0004527 * T2 + 0.0000033 * T3) * RadiansPerDegree;
		SunMeanAnomaly = (358.475845 + 35999.04975 * T - 0.00015 * T2 - 0.00000333333 * T3) * RadiansPerDegree;
		n = (int) (SunMeanAnomaly / PI2);
		SunMeanAnomaly -= n * PI2;

		SunMeanMotion = 1 / (365.24219879 - 0.00000614 * T);

		SunTrueAnomaly = Kepler(SunMeanAnomaly, SunEccentricity);
		SunDistance = SunSemiMajorAxis * (1 - (SunEccentricity * SunEccentricity))
				/ (1 + SunEccentricity * Math.cos(SunTrueAnomaly));

		SinPenumbra = (SunRadius - EarthRadius) / SunDistance;
		CosPenumbra = Math.sqrt(1 - (SinPenumbra * SinPenumbra));
	}

	long calls = 0;
	long iters = 0;

	/* Solve Kepler's equation */
	/* Inputs: */
	/* MeanAnomaly Time Since last perigee, in radians. */
	/* PI2 = one complete orbit. */
	/* Eccentricity Eccentricity of orbit's ellipse. */
	/* Output: */
	/* TrueAnomaly Angle between perigee, geocenter, and */
	/* current position. */
	double Kepler(double MeanAnomaly, double Eccentricity) {
		double E; /* Eccentric Anomaly */
		double Error;
		double TrueAnomaly;

		calls++;

		E = MeanAnomaly;/* + Eccentricity*sin(MeanAnomaly); /* Initial guess */
		do {
			Error = (E - Eccentricity * Math.sin(E) - MeanAnomaly) / (1 - Eccentricity * Math.cos(E));
			E -= Error;
			iters++;
		} while (Math.abs(Error) >= Epsilon);

		if (Math.abs(E - Math.PI) < Epsilon) {
			TrueAnomaly = Math.PI;
		} else {
			TrueAnomaly = 2 * Math.atan(Math.sqrt((1 + Eccentricity) / (1 - Eccentricity)) * Math.tan(E / 2));
		}
		if (TrueAnomaly < 0) {
			TrueAnomaly += PI2;
		}

		return TrueAnomaly;
	}

	// returns double[2]={RAANPrecession,PerigeePrecession}
	double[] GetPrecession(double SemiMajorAxis, double Eccentricity, double Inclination) {
		double[] retval = new double[2]; // RAANPrecession,PerigeePrecession
		retval[0] = 9.95 * Math.pow(EarthRadius / SemiMajorAxis, 3.5) * Math.cos(Inclination)
				/ ((1 - (Eccentricity * Eccentricity)) * (1 - (Eccentricity * Eccentricity))) * RadiansPerDegree;

		retval[1] = 4.97 * Math.pow(EarthRadius / SemiMajorAxis, 3.5)
				* (5 * ((Math.cos(Inclination)) * (Math.cos(Inclination))) - 1)
				/ ((1 - (Eccentricity * Eccentricity)) * (1 - (Eccentricity * Eccentricity))) * RadiansPerDegree;
		return retval;
	}

	/*
	 * Compute the site postion and velocity in the RA based coordinate system.
	 * SiteMatrix is set to a matrix which is used by GetTopoCentric to convert
	 * geocentric coordinates to topocentric (observer-centered) coordinates.
	 * Returns double[]={SiteX,SiteY,SiteZ,SiteVX,SiteVY,} and modifies SiteMatrix
	 */

	double[] GetSitPosition(double SiteLat, double SiteLong, double SiteElevation, double CurrentTime,
			double[][] SiteMatrix) {

		double G1, G2; /* Used to correct for flattening of the Earth */
		double CosLat, SinLat;
		// double OldSiteLat = -100000; /* Used to avoid unneccesary recomputation */
		// double OldSiteElevation = -100000;
		double Lat;
		double SiteRA; /* Right Ascension of site */
		double CosRA, SinRA;
		double[] retval = new double[5]; // SiteX,SiteY,SiteZ,SiteVX,SiteVY,

		// if ((SiteLat != OldSiteLat) || (SiteElevation != OldSiteElevation)) {
		// OldSiteLat = SiteLat;
		// OldSiteElevation = SiteElevation;
		Lat = Math.atan(1 / (1 - (EarthFlat * EarthFlat)) * Math.tan(SiteLat));

		CosLat = Math.cos(Lat);
		SinLat = Math.sin(Lat);

		G1 = EarthRadius / (Math.sqrt(1 - (2 * EarthFlat - (EarthFlat * EarthFlat)) * (SinLat * SinLat)));
		G2 = G1 * ((1 - EarthFlat) * (1 - EarthFlat));
		G1 += SiteElevation;
		G2 += SiteElevation;
		// }

		SiteRA = PI2 * ((CurrentTime - SidDay) * SiderealSolar + SidReference) - SiteLong;
		CosRA = Math.cos(SiteRA);
		SinRA = Math.sin(SiteRA);

		retval[0] = G1 * CosLat * CosRA;
		retval[1] = G1 * CosLat * SinRA;
		retval[2] = G2 * SinLat;
		retval[3] = -SidRate * retval[1];
		retval[4] = SidRate * retval[0];

		SiteMatrix[0][0] = SinLat * CosRA;
		SiteMatrix[0][1] = SinLat * SinRA;
		SiteMatrix[0][2] = -CosLat;
		SiteMatrix[1][0] = -SinRA;
		SiteMatrix[1][1] = CosRA;
		SiteMatrix[1][2] = 0.0;
		SiteMatrix[2][0] = CosRA * CosLat;
		SiteMatrix[2][1] = SinRA * CosLat;
		SiteMatrix[2][2] = SinLat;

		return retval;
	}

	// return double[]={Azimuth,Elevation}
	double[] GetBearings(double SatX, double SatY, double SatZ, double SiteX, double SiteY, double SiteZ,
			double[][] SiteMatrix) {
		double[] xyz = new double[3];
		double[] retval = new double[2];

		xyz = GetTopocentric(SatX, SatY, SatZ, SiteX, SiteY, SiteZ, SiteMatrix);

		retval[0] = Math.atan(xyz[2] / Math.sqrt((xyz[0] * xyz[0]) + (xyz[1] * xyz[1])));

		retval[1] = Math.PI - Math.atan2(xyz[1], xyz[0]);

		if (retval[0] < 0) {
			retval[0] += Math.PI;
		}
		return retval;
	}

	/*
	 * Convert from geocentric RA based coordinates to topocentric (observer
	 * centered) coordinates
	 */
	double[] GetTopocentric(double SatX, double SatY, double SatZ, double SiteX, double SiteY, double SiteZ,
			double[][] SiteMatrix) {

		double[] retval = new double[3];
		SatX -= SiteX;
		SatY -= SiteY;
		SatZ -= SiteZ;

		retval[0] = SiteMatrix[0][0] * SatX + SiteMatrix[0][1] * SatY + SiteMatrix[0][2] * SatZ;
		retval[1] = SiteMatrix[1][0] * SatX + SiteMatrix[1][1] * SatY + SiteMatrix[1][2] * SatZ;
		retval[3] = SiteMatrix[2][0] * SatX + SiteMatrix[2][1] * SatY + SiteMatrix[2][2] * SatZ;
		return retval;
	}

	void PrintTime(PrintStream OutFile, double Time) {
		int day, hours, minutes, seconds;

		day = (int) Time;
		Time -= day;
		if (Time < 0)
			Time += 1.0; /* Correct for truncation problems with negatives */

		hours = (int) (Time * 24);
		Time -= hours / 24.0;

		minutes = (int) (Time * MinutesPerDay);
		Time -= minutes / MinutesPerDay;

		seconds = (int) (Time * SecondsPerDay);
		seconds -= seconds / SecondsPerDay;

		OutFile.print("" + hours + ":" + minutes + ":" + seconds);
	}

	// returns double[] = {Range,RangeRate}
	double[] GetRange(double SiteX, double SiteY, double SiteZ, double SiteVX, double SiteVY, double SatX, double SatY,
			double SatZ, double SatVX, double SatVY, double SatVZ) {
		double DX, DY, DZ;
		double[] retval = new double[2];

		DX = SatX - SiteX;
		DY = SatY - SiteY;
		DZ = SatZ - SiteZ;

		retval[0] = Math.sqrt((DX * DX) + (DY * DY) + (DZ * DZ));

		retval[1] = ((SatVX - SiteVX) * DX + (SatVY - SiteVY) * DY + SatVZ * DZ) / retval[0];

		return retval;
	}

	// returns double[]={Latitude,Longitude,Height}
	double[] GetSubSatPoint(double SatX, double SatY, double SatZ, double Time) {
		double r;
		long i;
		double[] retval = new double[3];
		r = Math.sqrt((SatX * SatX) + (SatY * SatY) + (SatZ * SatZ));

		retval[1] = PI2 * ((Time - SidDay) * SiderealSolar + SidReference) - Math.atan2(SatY, SatX);

		/* i = floor(Longitude/2*pi) */
		i = (long) (retval[1] / PI2);
		if (i < 0)
			i--;

		retval[1] -= i * PI2;

		retval[0] = Math.atan(SatZ / Math.sqrt((SatX * SatX) + (SatY * SatY)));

		if (SSPELLIPSE) {
		} else
			retval[2] = r - EarthRadius;
		return retval;
	}

}
