package com.jtalics.n3mo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Ephemeris {

	public double StartTime, EndTime, StepTime; /* In Days, 1 = New Year of reference year */
	public Site site;
	public Satellite satellite;

	public class Frame { // like one frame in a movie

		Frame(int numModes) {
			modes = new String[numModes];
		}

		public double currentTime;
		public double azimuth;
		public double elevation;
		public double azimuthFlip;
		public double elevationFlip;
		public double doppler;
		public double range;
		public long height;
		public long lat;
		public long lon;
		public int phase;
		public String[] modes;
		public boolean apogee;
		public boolean eclipsed;
	}

	public List<Frame> frames = new ArrayList<>();

	Ephemeris(Site site, Satellite satellite) {
		this.site = site;
		this.satellite = satellite;
		GetSimulationParams();
	}
	void GetSimulationParams() {

		int Month, Day, Year;
		String line = "11 30 2017";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Start date (UTC) (Month Day Year) :");
		}
		String s[] = line.split(" ");
		Month = Integer.parseInt(s[0]);
		Day = Integer.parseInt(s[1]);
		Year = Integer.parseInt(s[2]);

		StartTime = Constants.GetDayNum(Year, Month, Day);
		line = "0";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Starting Hour (UTC) :");
		}
		StartTime += Double.parseDouble(line) / 24.0;

		line = "1";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Duration (Days) :");
		}
		EndTime = StartTime + Double.parseDouble(line);

		line = "1";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Time Step (Minutes) :");
		}
		StepTime = Double.parseDouble(line) / Constants.MinutesPerDay;
	}

	void calc() {

		Bearing bearing = new Bearing();
		Range range = new Range();
		Sun sun = new Sun((StartTime + EndTime) / 2);

		double ReferenceOrbit; /* Floating point orbit # at epoch */
		double CurrentTime/*, TmpTime, PrevTime*/; /* In Days */
		double CurrentOrbit;
		double AverageMotion, /* Corrected for drag */
				CurrentMotion;
		double MeanAnomaly, TrueAnomaly;
		final double SiteMatrix[][] = new double[3][3];
		long OrbitNum, PrevOrbitNum;
		long Day, PrevDay;
		double Doppler;
		int Phase;
		boolean DidApogee;
		boolean PrevVisible = false;

		
		ReferenceOrbit = satellite.EpochMeanAnomaly / (Math.PI * 2) + satellite.EpochOrbitNum;

		PrevDay = -10000;
		PrevOrbitNum = -10000;

		satellite.BeaconFreq *= 1E6; /* Convert to Hz */

		DidApogee = false;

		for (CurrentTime = StartTime; CurrentTime <= EndTime; CurrentTime += StepTime) {

			Frame frame = new Frame(satellite.NumModes);
			frame.currentTime = CurrentTime;
			AverageMotion = satellite.epochMeanMotion + (CurrentTime - satellite.EpochTime) * satellite.OrbitalDecay / 2;
			CurrentMotion = satellite.epochMeanMotion + (CurrentTime - satellite.EpochTime) * satellite.OrbitalDecay;

			CurrentOrbit = ReferenceOrbit + (CurrentTime - satellite.EpochTime) * AverageMotion;
			OrbitNum = (long) CurrentOrbit;

			MeanAnomaly = (CurrentOrbit - OrbitNum) * Constants.PI2;

			//TmpTime = CurrentTime;
			if (MeanAnomaly < Math.PI) {
				DidApogee = false;
			}
			if (satellite.PrintApogee && !DidApogee && MeanAnomaly > Math.PI) {
				/* Calculate Apogee */
				//TmpTime -= StepTime; /* So we pick up later where we left off */
				MeanAnomaly = Math.PI;
				CurrentTime = satellite.EpochTime + (OrbitNum - ReferenceOrbit + 0.5) / AverageMotion;
			}

			TrueAnomaly = sun.Kepler(MeanAnomaly, satellite.Eccentricity);
			// double Radius; /* From geocenter */
			// double SatX, SatY, SatZ; /* In Right Ascension based system */
			// double SatVX, SatVY, SatVZ; /* Kilometers/second */

			double SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(Constants.MinutesPerDay / CurrentMotion) / 3);
			satellite.calcPosVel(SemiMajorAxis, CurrentTime, TrueAnomaly); // ,&SatX,&SatY,&SatZ,&Radius,
																				// &SatVX,&SatVY,&SatVZ
			site.calcPosVel(sun, CurrentTime, SiteMatrix); // &SiteX,&SiteY,&SiteZ,&SiteVX,&SiteVY,

			bearing.calc(satellite, site, SiteMatrix); // &Elevation,&Azimuth

			if (bearing.elevation >= site.SiteMinElev && CurrentTime >= StartTime) {

				Day = (long) (CurrentTime + Constants.HalfSecond);
				if (((double) Day) > CurrentTime + Constants.HalfSecond)
					Day -= 1; /* Correct for truncation of negative values */

				if (OrbitNum == PrevOrbitNum && Day == PrevDay && !PrevVisible) {
					N3mo.outPrintStream.println(); /* Dipped out of sight; print blank */
				}
				if (OrbitNum != PrevOrbitNum || Day != PrevDay) {
					printHeader(Day,OrbitNum,site.Flip,satellite.MaxPhase);
				}
				PrevOrbitNum = OrbitNum;
				PrevDay = Day;
				frame.currentTime = CurrentTime + Constants.HalfSecond;
				Constants.PrintTime(N3mo.outPrintStream, frame.currentTime);

				frame.azimuth = bearing.azimuth * Constants.DegreesPerRadian;
				frame.elevation = bearing.elevation * Constants.DegreesPerRadian;
				N3mo.outPrintStream.print("  " + Math.round(frame.azimuth) + " " + Math.round(frame.elevation));
				// calc flip
				bearing.azimuth += Math.PI;
				if (bearing.azimuth >= Math.PI * 2) {
					bearing.azimuth -= Math.PI * 2;
				}
				bearing.elevation = Math.PI - bearing.elevation;
				frame.azimuthFlip = bearing.azimuth * Constants.DegreesPerRadian;
				frame.elevationFlip = bearing.elevation * Constants.DegreesPerRadian; 
				if (site.Flip) {
					N3mo.outPrintStream.print("  " + Math.round(frame.azimuthFlip) + "  " + Math.round(frame.elevationFlip));
				}

				range.GetRange(site,satellite);

				Doppler = -satellite.BeaconFreq * range.rangeRate / Constants.C;
				frame.doppler = Doppler;
				frame.range = range.range;
				N3mo.outPrintStream.print("  " + Math.round(frame.doppler) + " " + Math.round(frame.range));

				double[] ssp = GetSubSatPoint(sun, satellite, CurrentTime); // ,&SSPLat,&SSPLong,&Height
				frame.height = Math.round(ssp[2]);
				frame.lat = Math.round(ssp[0] * Constants.DegreesPerRadian);
				frame.lon = Math.round(ssp[1] * Constants.DegreesPerRadian);
				N3mo.outPrintStream.print(" " + frame.height + "  " + frame.lat + "  " + frame.lon);

				Phase = (int) (MeanAnomaly / (Math.PI * 2) * satellite.MaxPhase + satellite.perigeePhase);
				while (Phase < 0) {
					Phase += satellite.MaxPhase;
				}
				while (Phase >= satellite.MaxPhase) {
					Phase -= satellite.MaxPhase;
				}

				frame.phase = Phase;
				N3mo.outPrintStream.print(" " + Math.round(frame.phase) + "  ");
				satellite.PrintMode(N3mo.outPrintStream, Phase, frame);

				frame.apogee = (MeanAnomaly == Math.PI);
				if (satellite.PrintApogee && frame.apogee) {
					N3mo.outPrintStream.print("    Apogee");
				}
				frame.eclipsed = sun.Eclipsed(satellite, CurrentTime);
				if (site.PrintEclipses && frame.eclipsed) {
					N3mo.outPrintStream.print("  Eclipse");
				}
				N3mo.outPrintStream.println();
				PrevVisible = true;
				frames.add(frame);
			} else {
				PrevVisible = false;
			}
			if (satellite.PrintApogee && (MeanAnomaly == Math.PI)) {
				DidApogee = true;
			}
		}
		ObjectMapper mapper = new ObjectMapper();

		//Object to JSON in file
		try {
			mapper.writeValue(new File("frames.json"), this);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Object to JSON in String
		//String jsonInString = mapper.writeValueAsString(frames);
	}

	private void printHeader(long Day,long OrbitNum,boolean flip,double maxPhase) {
		/* Print Header */
		N3mo.outPrintStream.print(Constants.DayNames[(int) (Day % 7)]);
		N3mo.outPrintStream.print(" ");
		long[] date = Constants.GetDate(Day);
		N3mo.outPrintStream.print(date[2] + " " + date[1] + " " + date[0]);
		N3mo.outPrintStream.println("  ----Orbit # " + OrbitNum + "-----");
		N3mo.outPrintStream.print(" U.T.C.   Az  El  ");
		if (flip) {
			N3mo.outPrintStream.print(" Az'  El' ");
		}
		N3mo.outPrintStream.print("Doppler Range Height  Lat  Long  Phase(" + maxPhase + ")");
		N3mo.outPrintStream.println();
	}
	// SSP: Point where a straight line drawn from a satellite to the center of the Earth intersects the Earth's surface.
	static double[] GetSubSatPoint(Sun solarKeps, Satellite sat, double Time) {
		double r;
		long i;
		double[] retval = new double[3];
		r = Math.sqrt((sat.X * sat.X) + (sat.Y * sat.Y) + (sat.Z * sat.Z));

		retval[1] = Constants.PI2 * ((Time - solarKeps.SidDay) * Sun.SiderealSolar + solarKeps.SidReference) - Math.atan2(sat.Y, sat.X);

		/* i = floor(Longitude/2*pi) */
		i = (long) (retval[1] / Constants.PI2);
		if (i < 0) {
			i--;
		}
		retval[1] -= i * Constants.PI2;

		retval[0] = Math.atan(sat.Z / Math.sqrt((sat.X * sat.X) + (sat.Y * sat.Y)));

		if (Constants.SSPELLIPSE) {
		} else
			retval[2] = r - Constants.EarthRadius;
		return retval;
	}
}
