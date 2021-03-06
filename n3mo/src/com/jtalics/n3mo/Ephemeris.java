package com.jtalics.n3mo;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Ephemeris {

	public double startTime, endTime, stepTime; /* Units of days since 1900, e.g. 1.5 = noon on 1-2-1900 */
	public double siteMinElev=0.0; // ignore satellites below this angular elevation in the sky, 0.0 = horizon
	public boolean printEclipses=true; // is satellite eclipsing sun?
	public boolean flip=true; // print flipped angles for special hardware

	public static class Frame { // like one frame in a movie

		public Frame() {
			// JSON only
		}
		
		public Frame(int numModes) {
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

	public Site site;
	public Satellite satellite;

	private final static String JSON_FILE_NAME = "ephemeris.json";
	
	public Ephemeris() {
		// use for JSON
	}
	
	// READ FROM CONSOLE
	Ephemeris(Site site, Satellite satellite) {
		this.site = site;
		this.satellite = satellite;
/*
		// TODO: remove next three lines, store them directly in this class
		this.siteMinElev = site.getSiteMinElev();
		this.flip = site.isFlip();
		this.printEclipses = site.isPrintEclipses();
*/
		GetSimulationParams();
	}

	void GetSimulationParams() {

		int Month, Day, Year;
		//String line = "11 30 2017"; // Nov 30 2017 is the official test date
		String line = "1 10 2018";
		
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Start date (UTC) (Month Day Year) :");
		}
		String s[] = line.split(" ");
		Month = Integer.parseInt(s[0]);
		Day = Integer.parseInt(s[1]);
		Year = Integer.parseInt(s[2]); 

		startTime = Constants.getDayNumberSince1900(Year, Month, Day);
		line = "0";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Starting Hour (UTC) :");
		}
		startTime += Double.parseDouble(line) / 24.0;

		line = "1";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Duration (Days) :");
		}
		endTime = startTime + Double.parseDouble(line);

		line = "1";
		if (!N3mo.NOCONSOLE) {
			line = System.console().readLine("Time Step (Minutes) :");
		}
		stepTime = Double.parseDouble(line) / Constants.MinutesPerDay;
	}

	public boolean calc() {

		frames.clear();
		if (satellite == null || site == null || stepTime==0) {
			return false;
		}
		System.out.println("SITE:"+site.siteName);
		System.out.println("SATELLITE:"+satellite.satName);
		Bearing bearing = new Bearing();
		Range range = new Range();
		Sun sun = new Sun((startTime + endTime) / 2);

		double ReferenceOrbit; /* Floating point orbit # at epoch */
		double currentTime; /* A real number in units of days */
		double CurrentOrbit;
		double AverageMotion, /* Corrected for drag */
				CurrentMotion;
		double MeanAnomaly, TrueAnomaly;
		final double SiteMatrix[][] = new double[3][3];
		long OrbitNum, PrevOrbitNum;
		int Day, PrevDay;
		double Doppler;
		int Phase;
		boolean DidApogee;
		boolean PrevVisible = false;

		ReferenceOrbit = satellite.epochMeanAnomaly / (Math.PI * 2) + satellite.epochOrbitNum;

		PrevDay = -10000;
		PrevOrbitNum = -10000;

		// Here is the main loop of the calculation
		DidApogee = false;
		for (currentTime = startTime; currentTime <= endTime; currentTime += stepTime) {

			Frame frame = new Frame(satellite.numModes);
			frame.currentTime = currentTime;
			
			AverageMotion = satellite.epochMeanMotion + (currentTime - satellite.epochTime) * satellite.orbitalDecay / 2;
			CurrentMotion = satellite.epochMeanMotion + (currentTime - satellite.epochTime) * satellite.orbitalDecay;

			CurrentOrbit = ReferenceOrbit + (currentTime - satellite.epochTime) * AverageMotion;
			OrbitNum = (long) CurrentOrbit;

			MeanAnomaly = (CurrentOrbit - OrbitNum) * Constants.PI2;

			if (MeanAnomaly < Math.PI) {
				DidApogee = false;
			}
			if (satellite.printApogee && !DidApogee && MeanAnomaly > Math.PI) {
				/* Calculate Apogee */
				MeanAnomaly = Math.PI;
				currentTime = satellite.epochTime + (OrbitNum - ReferenceOrbit + 0.5) / AverageMotion;
			}

			TrueAnomaly = sun.Kepler(MeanAnomaly, satellite.eccentricity);

			double SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(Constants.MinutesPerDay / CurrentMotion) / 3);
			satellite.calcPosVel(SemiMajorAxis, currentTime, TrueAnomaly);

			site.calcPosVel(sun, currentTime, SiteMatrix);

			bearing.calc(satellite, site, SiteMatrix);

			if (bearing.elevation >= siteMinElev && currentTime >= startTime) {

				Day = (int) (currentTime + Constants.HalfSecond);
				if (((double) Day) > currentTime + Constants.HalfSecond)
					Day -= 1; /* Correct for truncation of negative values */

				if (OrbitNum == PrevOrbitNum && Day == PrevDay && !PrevVisible) {
					N3mo.outPrintStream.println(); /* Dipped out of sight; print blank */
				}
				if (OrbitNum != PrevOrbitNum || Day != PrevDay) {
					printHeader(Day, OrbitNum, flip, satellite.maxPhase);
				}
				PrevOrbitNum = OrbitNum;
				PrevDay = Day;
				frame.currentTime = currentTime + Constants.HalfSecond;
				N3mo.outPrintStream.print(Constants.printTime(frame.currentTime));

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
				if (flip) {
					N3mo.outPrintStream
							.print("  " + Math.round(frame.azimuthFlip) + "  " + Math.round(frame.elevationFlip));
				}

				range.GetRange(site, satellite);

				Doppler = -satellite.beaconFreq * range.rangeRate / Constants.C;
				frame.doppler = Doppler;
				frame.range = range.range;
				N3mo.outPrintStream.print("  " + Math.round(frame.doppler) + " " + Math.round(frame.range));

				double[] ssp = GetSubSatPoint(sun, satellite, currentTime);
				frame.height = Math.round(ssp[2]);
				frame.lat = Math.round(ssp[0] * Constants.DegreesPerRadian);
				frame.lon = Math.round(ssp[1] * Constants.DegreesPerRadian);
				N3mo.outPrintStream.print(" " + frame.height + "  " + frame.lat + "  " + frame.lon);

				Phase = (int) (MeanAnomaly / (Math.PI * 2) * satellite.maxPhase + satellite.perigeePhase);
				while (Phase < 0) {
					Phase += satellite.maxPhase;
				}
				while (Phase >= satellite.maxPhase) {
					Phase -= satellite.maxPhase;
				}

				frame.phase = Phase;
				N3mo.outPrintStream.print(" " + Math.round(frame.phase) + "  ");
				satellite.PrintMode(N3mo.outPrintStream, Phase, frame);

				frame.apogee = (MeanAnomaly == Math.PI);
				if (satellite.printApogee && frame.apogee) {
					N3mo.outPrintStream.print("    Apogee");
				}
				frame.eclipsed = sun.Eclipsed(satellite, currentTime);
				if (printEclipses && frame.eclipsed) {
					N3mo.outPrintStream.print("  Eclipse");
				}
				N3mo.outPrintStream.println();
				PrevVisible = true;
				frames.add(frame);
			} else {
				PrevVisible = false;
			}
			if (satellite.printApogee && (MeanAnomaly == Math.PI)) {
				DidApogee = true;
			}
		}
		ObjectMapper mapper = new ObjectMapper();

		// Object to JSON in file
		try {
			mapper.writeValue(new File(JSON_FILE_NAME), this);
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

		// Object to JSON in String
		// String jsonInString = mapper.writeValueAsString(frames);
		return true;
	}

	private void printHeader(long Day, long OrbitNum, boolean flip, double maxPhase) {
		/* Print Header */
		N3mo.outPrintStream.print(Constants.DayNames[(int) (Day % 7)]);
		N3mo.outPrintStream.print(" ");
		long[] date = Constants.getDate(Day);
		N3mo.outPrintStream.print(date[2] + " " + date[1] + " " + date[0]);
		N3mo.outPrintStream.println("  ----Orbit # " + OrbitNum + "-----");
		N3mo.outPrintStream.print(" U.T.C.   Az  El  ");
		if (flip) {
			N3mo.outPrintStream.print(" Az'  El' ");
		}
		N3mo.outPrintStream.print("Doppler Range Height  Lat  Long  Phase(" + maxPhase + ")");
		N3mo.outPrintStream.println();
	}

	// SSP: Lat,lon where a straight line drawn from a satellite to the center of
	// the Earth intersects the Earth's surface.  It is the place on earth where
	// the satellite is at zenith.  Good for generating world ground trace maps.
	static double[] GetSubSatPoint(Sun solarKeps, Satellite sat, double Time) {
		double r;
		long i;
		double[] retval = new double[3];
		r = Math.sqrt((sat.X * sat.X) + (sat.Y * sat.Y) + (sat.Z * sat.Z));

		retval[1] = Constants.PI2 * ((Time - solarKeps.siderealDay) * Sun.SiderealSolar + solarKeps.siderealReference)
				- Math.atan2(sat.Y, sat.X);

		/* i = floor(Longitude/2*pi) */
		i = (long) (retval[1] / Constants.PI2);
		if (i < 0) {
			i--;
		}
		retval[1] -= i * Constants.PI2;

		retval[0] = Math.atan(sat.Z / Math.sqrt((sat.X * sat.X) + (sat.Y * sat.Y)));

		if (!Constants.SSPELLIPSE) {
			retval[2] = r - Constants.EarthRadius;
		}
		return retval;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public double getStepTime() {
		return stepTime;
	}

	public void setStepTime(double stepTime) {
		this.stepTime = stepTime;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
/*
		// TODO: remove next three lines, store them directly in this class
		this.siteMinElev = site.getSiteMinElev();
		this.flip = site.isFlip();
		this.printEclipses = site.isPrintEclipses();
*/
	}

	public Satellite getSatellite() {
		return satellite;
	}

	public void setSatellite(Satellite satellite) {
		this.satellite = satellite;
	}

	public double getSiteMinElev() {
		return siteMinElev;
	}

	public void setSiteMinElev(double siteMinElev) {
		this.siteMinElev = siteMinElev;
	}

	public boolean isPrintEclipses() {
		return printEclipses;
	}

	public void setPrintEclipses(boolean printEclipses) {
		this.printEclipses = printEclipses;
	}

	public boolean isFlip() {
		return flip;
	}

	public void setFlip(boolean flip) {
		this.flip = flip;
	}

	public List<Frame> getFrames() {
		return frames;
	}

	public static Ephemeris getInstance() {
		
		Ephemeris ephemeris = null;
		if (false) {
		ObjectMapper mapper = new ObjectMapper();

		// Object to JSON in file
		try {
			ephemeris = mapper.readValue(new File(JSON_FILE_NAME), Ephemeris.class);
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
		}
		if (ephemeris == null) {
			ephemeris = new Ephemeris(null,null);
		}
		return ephemeris;
	}
}
