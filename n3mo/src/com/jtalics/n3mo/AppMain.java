package com.jtalics.n3mo;

import java.io.File;
import java.io.PrintStream;

public class AppMain {
	static final int MaxModes = 10;

	class ModeRec {
		int MinPhase, MaxPhase;
		String ModeStr;
	}

	static boolean NOCONSOLE = false;

	/* Simulation Parameters */

	double StartTime, EndTime, StepTime; /* In Days, 1 = New Year of reference year */

	public static void main(String[] args) throws Exception {

		if (System.console() == null) {
			NOCONSOLE = true; // probably running in IDE or debugger
			// throw new Exception("Cannot open a console");
		}
		System.out.println("Current directory is " + System.getProperty("user.dir"));
		AppMain appMain = new AppMain();
		appMain.mainC();
	}

	private void mainC() throws Exception {

		double ReferenceOrbit; /* Floating point orbit # at epoch */
		double CurrentTime, TmpTime/*, PrevTime*/; /* In Days */
		double CurrentOrbit;
		double AverageMotion, /* Corrected for drag */
				CurrentMotion;
		double MeanAnomaly, TrueAnomaly;
		final double SiteMatrix[][] = new double[3][3];
		long OrbitNum, PrevOrbitNum;
		long Day, PrevDay;
		double Doppler;
		int Phase;
		String FileName = null;
		PrintStream OutFile;
		boolean DidApogee;
		boolean PrevVisible = false;

		System.out.println(Constants.VersionStr);

		Satellite sat2 = new Satellite(this);
		Site site2 = new Site();
		Bearing bearing = new Bearing();
		GetSimulationParams();

		SolarKeps solarKeps = new SolarKeps((StartTime + EndTime) / 2);

		if (!NOCONSOLE) {
			FileName = System.console().readLine("Output file (RETURN for TTY): ");
		}
		if (FileName == null || FileName.length() > 0) {
			File file = new File(FileName, sat2.SatName + ".eph");
			OutFile = new PrintStream(file);
		} else
			OutFile = System.out;

		OutFile.println(sat2.SatName + " Element Set " + sat2.ElementSet);

		OutFile.println(site2.SiteName);

		OutFile.println("Doppler calculated for freq = " + sat2.BeaconFreq);

		double[] prec = sat2.GetPrecession(sat2.Eccentricity, sat2.Inclination);

		ReferenceOrbit = sat2.EpochMeanAnomaly / (Math.PI * 2) + sat2.EpochOrbitNum;

		PrevDay = -10000;
		PrevOrbitNum = -10000;
		/* PrevTime = StartTime - 2 * StepTime; */

		sat2.BeaconFreq *= 1E6; /* Convert to Hz */

		DidApogee = false;

		for (CurrentTime = StartTime; CurrentTime <= EndTime; CurrentTime += StepTime) {

			AverageMotion = sat2.epochMeanMotion + (CurrentTime - sat2.EpochDay) * sat2.OrbitalDecay / 2;
			CurrentMotion = sat2.epochMeanMotion + (CurrentTime - sat2.EpochDay) * sat2.OrbitalDecay;

			CurrentOrbit = ReferenceOrbit + (CurrentTime - sat2.EpochDay) * AverageMotion;
			OrbitNum = (long) CurrentOrbit;

			MeanAnomaly = (CurrentOrbit - OrbitNum) * Constants.PI2;

			TmpTime = CurrentTime;
			if (MeanAnomaly < Math.PI) {
				DidApogee = false;
			}
			if (sat2.PrintApogee && !DidApogee && MeanAnomaly > Math.PI) {
				/* Calculate Apogee */
				TmpTime -= StepTime; /* So we pick up later where we left off */
				MeanAnomaly = Math.PI;
				CurrentTime = sat2.EpochDay + (OrbitNum - ReferenceOrbit + 0.5) / AverageMotion;
			}

			TrueAnomaly = solarKeps.Kepler(MeanAnomaly, sat2.Eccentricity);
			// double Radius; /* From geocenter */
			// double SatX, SatY, SatZ; /* In Right Ascension based system */
			// double SatVX, SatVY, SatVZ; /* Kilometers/second */

			double SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(Constants.MinutesPerDay / CurrentMotion) / 3);
			double[] sat = Satellite.GetSatPosition(sat2.EpochDay, sat2.EpochRAAN, sat2.EpochArgPerigee, SemiMajorAxis, sat2.Inclination,
					sat2.Eccentricity, prec[0], prec[1], CurrentTime, TrueAnomaly); // ,&SatX,&SatY,&SatZ,&Radius,
																				// &SatVX,&SatVY,&SatVZ

			double[] site = site2.GetSitPosition(solarKeps, site2.SiteLat, site2.SiteLong, site2.SiteAltitude, CurrentTime, SiteMatrix); // &SiteX,&SiteY,&SiteZ,&SiteVX,&SiteVY,

			
			bearing.calcBearings(sat[0], sat[1], sat[2], site[0], site[1], site[2], SiteMatrix); // &Elevation,&Azimuth

			if (bearing.elevation >= site2.SiteMinElev && CurrentTime >= StartTime) {

				Day = (long) (CurrentTime + Constants.HalfSecond);
				if (((double) Day) > CurrentTime + Constants.HalfSecond)
					Day -= 1; /* Correct for truncation of negative values */

				if (OrbitNum == PrevOrbitNum && Day == PrevDay && !PrevVisible) { // TODO? is prevVisible init'd ?
					OutFile.println(); /* Dipped out of sight; print blank */
				}
				if (OrbitNum != PrevOrbitNum || Day != PrevDay) {
					/* Print Header */
					OutFile.print(Constants.DayNames[(int) (Day % 7)]);
					OutFile.print(" ");
					long[] date = Constants.GetDate(Day);
					OutFile.print(date[2] + " " + date[1] + " " + date[0]);
					OutFile.println("  ----Orbit # " + OrbitNum + "-----");
					OutFile.print(" U.T.C.   Az  El  ");
					if (site2.Flip) {
						OutFile.print(" Az'  El' ");
					}
					OutFile.print("Doppler Range Height  Lat  Long  Phase(" + sat2.MaxPhase + ")");
					OutFile.println();
				}

				PrevOrbitNum = OrbitNum;
				PrevDay = Day;
				Constants.PrintTime(OutFile, CurrentTime + Constants.HalfSecond);

				OutFile.print("  " + Math.round(bearing.azimuth * Constants.DegreesPerRadian) + " " + Math.round(bearing.elevation * Constants.DegreesPerRadian));
				if (site2.Flip) {
					bearing.azimuth += Math.PI;
					if (bearing.azimuth >= Math.PI * 2) {
						bearing.azimuth -= Math.PI * 2;
					}
					bearing.elevation = Math.PI - bearing.elevation;
					OutFile.print("  " + Math.round(bearing.azimuth * Constants.DegreesPerRadian) + "  " + Math.round(bearing.elevation * Constants.DegreesPerRadian));
				}

				double[] range = GetRange(site[0], site[1], site[2], site[3], site[4], sat[0], sat[1], sat[2], /* sat[3] is Radius*/sat[4],
						sat[5], sat[6]); // Range,RangeRate

				Doppler = -sat2.BeaconFreq * range[1] / Constants.C;
				OutFile.print("  " + Math.round(Doppler) + " " + Math.round(range[0]));

				double[] ssp = GetSubSatPoint(solarKeps, sat[0], sat[1], sat[2], CurrentTime); // ,&SSPLat,&SSPLong,&Height
				OutFile.print(" " + Math.round(ssp[2]) + "  " + Math.round(ssp[0] * Constants.DegreesPerRadian) + "  " + Math.round(ssp[1] * Constants.DegreesPerRadian));

				Phase = (int) (MeanAnomaly / (Math.PI * 2) * sat2.MaxPhase + sat2.perigeePhase);
				while (Phase < 0) {
					Phase += sat2.MaxPhase;
				}
				while (Phase >= sat2.MaxPhase) {
					Phase -= sat2.MaxPhase;
				}

				OutFile.print(" " + Math.round(Phase) + "  ");
				sat2.PrintMode(OutFile, Phase);

				if (sat2.PrintApogee && (MeanAnomaly == Math.PI)) {
					OutFile.print("    Apogee");
				}
				if (site2.PrintEclipses && solarKeps.Eclipsed(sat[0], sat[1], sat[2], sat[3], CurrentTime)) {
					OutFile.print("  Eclipse");
				}
				OutFile.println();
				PrevVisible = true;
			} else {
				PrevVisible = false;
			}
			if (sat2.PrintApogee && (MeanAnomaly == Math.PI)) {
				DidApogee = true;
			}

			/* PrevTime = CurrentTime;*/
			CurrentTime = TmpTime;
		}
		OutFile.close();
	}

	void GetSimulationParams() {

		int Month, Day, Year;
		String line = "11 30 2017";
		if (!NOCONSOLE) {
			line = System.console().readLine("Start date (UTC) (Month Day Year) :");
		}
		String s[] = line.split(" ");
		Month = Integer.parseInt(s[0]);
		Day = Integer.parseInt(s[1]);
		Year = Integer.parseInt(s[2]);

		StartTime = Constants.GetDayNum(Year, Month, Day);
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
		StepTime = Double.parseDouble(line) / Constants.MinutesPerDay;
	}

	// returns double[] = {Range,RangeRate}
	static double[] GetRange(double SiteX, double SiteY, double SiteZ, double SiteVX, double SiteVY, double SatX, double SatY,
			double SatZ, double SatVX, double SatVY, double SatVZ) {
		double DX, DY, DZ;
		double[] retval = new double[2];

		DX = SatX - SiteX;
		DY = SatY - SiteY;
		DZ = SatZ - SiteZ;

		retval[0] = Math.sqrt((DX * DX) + (DY * DY) + (DZ * DZ)); // Range

		retval[1] = ((SatVX - SiteVX) * DX + (SatVY - SiteVY) * DY + SatVZ /*why not "-SiteVX"?*/ * DZ) / retval[0]; // RangeRate

		return retval;
	}

	// returns double[]={Latitude,Longitude,Height}
	static double[] GetSubSatPoint(SolarKeps solarKeps, double SatX, double SatY, double SatZ, double Time) {
		double r;
		long i;
		double[] retval = new double[3];
		r = Math.sqrt((SatX * SatX) + (SatY * SatY) + (SatZ * SatZ));

		retval[1] = Constants.PI2 * ((Time - solarKeps.SidDay) * SolarKeps.SiderealSolar + solarKeps.SidReference) - Math.atan2(SatY, SatX);

		/* i = floor(Longitude/2*pi) */
		i = (long) (retval[1] / Constants.PI2);
		if (i < 0)
			i--;

		retval[1] -= i * Constants.PI2;

		retval[0] = Math.atan(SatZ / Math.sqrt((SatX * SatX) + (SatY * SatY)));

		if (Constants.SSPELLIPSE) {
		} else
			retval[2] = r - Constants.EarthRadius;
		return retval;
	}
}
