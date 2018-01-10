package com.jtalics.n3mo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.jtalics.n3mo.Ephemeris.Frame;

// LOADS & STORES THE KEPS, ETC.
public class Satellite {

	public String satName; // ISS for example
	// Keplerian Elements:  https://marine.rutgers.edu/cool/education/class/paul/orbits.html
	// http://www.amsat.org/amsat-new/tools/keps_tutorial.php
	// UPDATE KEPS BIWEEKLY for LEO, MONTHLY for HEO

	// Time of epoch: The first thing you need to define an orbit is the time at
	// which the Keplerian Elements were defined. You need a snapshot of where and
	// how fast the satellite was going.
	public double epochTime;  // In units of Days since 1/1/1900.  Noon on 1/2/1900 = 1.500
	
	// This element tells you what the angle is between the equator and the orbit
	// when looking from the center of the Earth. If the orbit went exactly around
	// the equator from left to right, then the inclination would be 0. The
	// inclination ranges from 0 to 180 degrees.
	public double inclination; 

	// RAAN at epoch: This is probably one of the most difficult of the elements to
	// describe. The ascending node is the place where the satellite
	// crosses the equator while going from the Southern Hemisphere
	// to the Northern Hemisphere. Now since the Earth rotates, you
	// need to specify a fixed object in space. We use Aries (this
	// is also the same location as the vernal equinox). The angle,
	// from the center of the Earth, between Aries and the ascending
	// node is called the right ascension of ascending node.
	public double epochRAAN; 

	// The eccentricity tells you how round the orbit is. If the orbit is a perfect
	// circle, then the eccentricity is 0. When the eccentricity is close to 1, then
	// the orbit is very flat.
	public double eccentricity; 

	// Argument of perigee at epoch: Since an orbit usually has an ellipticalshape,
	// the satellite will be closer to the Earth at one point than at another. The
	// point where the satellite is the closest to the Earth is called the perigee.
	// The point where the satellite is the furthest from the Earth is called the
	// apogee. The argument of perigee is the angle formed between the perigee and
	// the ascending node. If the perigee would occur at the ascending node, the
	// argument of perigee would be 0.
	public double epochArgPerigee; 

	// Revolutions/day. The mean motion tells you how fast the satellite is going.
	// According to Kepler's Law: v=(G*M)/r so as the satellite gets closer to the
	// Earth, its velocity increases. If we know how fast the satellite is going, we
	// also know the altitude of the satellite.
	public double epochMeanMotion; 

	// Mean Anomaly at epoch. The mean anomaly tells you where the satellite is in
	// its orbital path. The mean anomaly ranges from 0 to 360 degrees. The mean
	// anomaly is referenced to the perigee. If the satellite were at the perigee,
	// the mean anomaly would be 0.
	public double epochMeanAnomaly; 

	// Misc. data for the satellite:
	public long epochOrbitNum; /* Integer orbit # of epoch */
	public double orbitalDecay; /* Revolutions/day^2 */
	public int elementSet;
	public double beaconFreq = 146.0; /* Mhz, used for doppler calc */
	public double maxPhase = 256; /* Phase units in 1 orbit */
	public double perigeePhase;
	public int numModes;
	final int maxModes = 10;

	public class ModeRec {
		int minPhase, maxPhase;
		String modeStr;
	}

	public ModeRec[] modes = new ModeRec[maxModes];
	public boolean printApogee = false;
	double radius; /* From geocenter */
	double X, Y, Z; /* In Right Ascension based system */
	double VX, VY, VZ; /* Kilometers/second */
	private double raanPrecession;
	private double perigeePrecession;

	public Satellite() {
		// JSON
	}
	
	/**
	 * READS AMSAT FORMAT
	 */
	public Satellite(File infile, String selection) throws Exception {

		String line, token;
		boolean found;
		int i;
		char satchar;

		int NumSatellites = ListSatellites();

		List<String> lines = Constants.getLines(infile);
		Iterator<String> iter = lines.iterator();
		found = false;
		// Use the number to get the satellite name
		while (!found) {
			if (!N3mo.NOCONSOLE) { // TODO: move console reading up into AppMain
				try {
					selection = System.console().readLine("Letter or satellite name :");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Validate number
			if (selection.length() == 1) { /* use single character label */
				satchar = selection.charAt(0);

				if (Constants.letterNum(satchar) > NumSatellites) {
					System.out.println("'" + satchar + "' is out of range");

					continue;
				}
				// Convert number to satellite name
				i = 0;
				while (iter.hasNext()) {
					line = iter.next();
					if (line.startsWith("Satellite: ")) {
						if ((++i) == Constants.letterNum(satchar)) {
							found = true;
							satName = line.substring(11);
							break;
						}
					}
				}
			} else {
				while (!found) { /* use satellite name */
					while (iter.hasNext()) {
						line = iter.next();
						if (line.startsWith("Satellite: ") && line.endsWith(selection)) {
							found = true;
							break;
						}
					}
				}
			}

			if (!found) {
				System.out.println("Satellite not found:" + satName);
				// TODO: InFile.close();?
			}
		}

		// BeaconFreq = 146.0; /* Default value */

		line = iter.next(); // skip line "Catalog number;"
		line = iter.next();

		token = "Epoch time:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String s = line.substring(token.length() + 1);
		double d = Double.parseDouble(s); // d is not an actual number and may contain spaces (TODO)
		int epochYear = (int) (d / 1000.0);
		d -= epochYear * 1000.0; // d now is days with decimal
		// only last two digits of year is saved? 
		if (epochYear < 57) {epochYear += 2000;} // Sputnik was in 1957
		else if (epochYear < 100) {epochYear += 1900;}

		epochTime = d + Constants.getDayNumberSince1900(epochYear, 1 , 1);

		line = iter.next();
		token = "Element set:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		elementSet = Integer.parseInt(s);

		line = iter.next();
		token = "Inclination:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String deg = " deg";
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		inclination = Double.parseDouble(s) * Constants.RadiansPerDegree;

		line = iter.next();
		token = "RA of node:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		epochRAAN = Double.parseDouble(s) * Constants.RadiansPerDegree;

		line = iter.next();
		token = "Eccentricity:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		eccentricity = Double.parseDouble(line.substring(token.length() + 1));

		line = iter.next();
		token = "Arg of perigee:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		epochArgPerigee = Double.parseDouble(s) * Constants.RadiansPerDegree;

		line = iter.next();
		token = "Mean anomaly:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		epochMeanAnomaly = Double.parseDouble(s) * Constants.RadiansPerDegree;

		line = iter.next();
		token = "Mean motion:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String revperday = "rev/day";
		s = line.substring(token.length() + 1);
		if (s.endsWith(revperday))
			s = s.substring(0, s.length() - revperday.length());
		epochMeanMotion = Double.parseDouble(s);

		line = iter.next();
		token = "Decay rate:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String revperdaysquared = "rev/day^2";
		s = line.substring(token.length() + 1);
		if (s.endsWith(revperdaysquared))
			s = s.substring(0, s.length() - revperdaysquared.length());
		orbitalDecay = Double.parseDouble(s);

		line = iter.next();
		token = "Epoch rev:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		epochOrbitNum = Integer.parseInt(line.substring(token.length() + 1));

		while (iter.hasNext() && (line = iter.next()).length() > 2) {
			token = "Beacon:";
			if (!line.startsWith(token)) {
				throw new Exception();
			}
			beaconFreq = Double.parseDouble(line.substring(token.length() + 1));
		}
		beaconFreq *= 1E6; /* Convert to Hz */

		printApogee = (eccentricity >= 0.3);
		perigeePhase = 0;
		maxPhase = 256; /* Default values */
		numModes = 0;

		lines = Constants.getLines(new File("mode.dat"));

		found = true;
		while (iter.hasNext()) {
			token = "Satellite: ";
			line = iter.next();
			if (line.startsWith(token) && line.endsWith(satName)) {
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
				beaconFreq = Double.parseDouble(line.substring(token.length() + 1));
				beaconFreq *= 1E6; /* Convert to Hz */

				line = iter.next();
				token = "Perigee phase: ";
				if (!line.startsWith(token)) {
					throw new Exception();
				}
				perigeePhase = Double.parseDouble(line.substring(token.length() + 1));

				line = iter.next();
				token = "Max phase: ";
				if (!line.startsWith(token)) {
					throw new Exception();
				}
				maxPhase = Double.parseDouble(line.substring(token.length() + 1));

				line = iter.next();
				Scanner scanner = new Scanner(line);
				token = "Mode: ";

				if (!line.startsWith(token)) {
					scanner.close();
					throw new Exception();
				}
				modes[numModes].modeStr = line.substring(token.length(), token.length() + 1 + 20);
				modes[numModes].minPhase = scanner.nextInt();
				modes[numModes].maxPhase = scanner.nextInt();
				scanner.close();
				if (numModes >= maxModes) {
					throw new Exception();
				}
				numModes++;
			}
		}
		calcPrecession();
	}

	/* Also see http://celestrak.com/NORAD/documentation/tle-fmt.asp
	 * and: www.amsat.org/amsat-new/tools/keps_detail.php
	 * and: https://spaceflight.nasa.gov/realdata/sightings/SSapplications/Post/JavaSSOP/SSOP_Help/tle_def.html
	 * From AMSAT:
	 * Keplerian Elements For the International Space Station
 	 * 6 May, 2004 
	 *
	 * ISS
	 * 1 25544U 98067A   04127.92349537  .00017095  00000-0  14786-3 0  7232
	 * 2 25544  51.6276 176.0525 0011067 106.0444 249.6038 15.69246258311835
	 *
	 *	2-Line Element Definition (use key to decode)
	 *
	 *	1 07530U 74089B   17348.52292424 -.00000039  00000-0  34057-4 0  9992
	 *	1 AAAAAU YYLLLPPP  BBBBB.BBBBBBBB  .CCCCCCCC  DDDDD-D  EEEEE-E F GGGGZ
	 *   01234567890123456789012345678901234567890123456789012345678901234567890
	 *	2 AAAAA  HHH.HHHH III.IIII JJJJJJJ KKK.KKKK MMM.MMMM NN.NNNNNNNNRRRRRZ
	    
  	 * AUTHORITATIVE: https://www.space-track.org/documentation#/tle
	 *	Key - DIFFERET THAN NASABARE!
	 *
	 *	    [1] - Line #1 label
	 *	    [2] - Line #2 label
	 *	    [AAAAA] - Catalog Number assigned sequentially (5-digit integer from 1 to 99999)
	 *	    [U] - Security Classification (U = Unclassified)
	 *	    [YYLLLPPP] - International Designator (YY = 2-digit Launch Year; LLL = 3-digit Sequential Launch of the Year; PPP = up to 3 letter Sequential Piece ID for that launch)
	 *	    [BBBBB.BBBBBBBB] - Epoch Time -- 2-digit year, followed by 3-digit sequential day of the year, followed by the time represented as the fractional portion of one day
	 *	    [.CCCCCCCC] - ndot/2 Drag Parameter (rev/day2) -- one half the first time derivative of the mean motion. This drag term is used by the SGP orbit propagator.
	 *	    [DDDDD-D] - n double dot/6 Drag Parameter (rev/day3) -- one sixth the second time derivative of the mean motion. The "-D" is the tens exponent (10-D). This drag term is used by the SGP orbit propagator.
	 *	    [EEEEE-E] - Bstar Drag Parameter (1/Earth Radii) -- Pseudo Ballistic Coefficient. The "-E" is the tens exponent (10-E). This drag term is used by the SGP4 orbit propagator.
	 *	    [F] - Ephemeris Type -- 1-digit integer (zero value uses SGP or SGP4 as provided in the Project Spacetrack report.
	 *	    [GGGG] - Element Set Number assigned sequentially (up to a 4-digit integer from 1 to 9999). This number recycles back to "1" on the update following element set number "9999."
	 *	    [HHH.HHHH] - Orbital Inclination (from 0 to 180 degrees).
	 *	    [III.IIII] - Right Ascension of the Ascending Node (from 0 to 360 degrees).
	 *	    [JJJJJJJ] - Orbital Eccentricity -- there is an implied leading decimal point (between 0.0 and 1.0).
	 *	    [KKK.KKKK] - Argument of Perigee (from 0 to 360 degrees).
	 *	    [MMM.MMMM] - Mean Anomaly (from 0 to 360 degrees).
	 *	    [NN.NNNNNNNN] - Mean Motion (revolutions per day).
	 *	    [RRRRR] - Revolution Number (up to a 5-digit integer from 1 to 99999). This number recycles following revolution nymber 99999.
	 *	    [Z] - Check Sum (1-digit integer). Both lines have a check sum that is computed from the sum of all integer characters on that line plus a "1" for each negative (-) sign on that line. The check sum is the modulo-10 (or ones digit) of the sum of the digits and negative signs. 
	 */	
	public Satellite(String line0, String line1, String line2) {
		satName = line0;
		if (line1.charAt(0) != '1' || line2.charAt(0) != '2')
			throw new IllegalArgumentException("Keps incorrectly formatted:\n" + line0 + "\n" + line1 + "\n" + line2);

		String s = line1.substring(2, 7);
		Integer catalogNum = Integer.parseInt(s.trim()); // A
		s = line1.substring(18, 32);
		double d = Double.parseDouble(s.trim()); // B - nasabare=(18,32)
		int EpochYear = (int) (d / 1000.0);
		d -= EpochYear * 1000.0; // d now is days with decimal
		// only last two digits of year is saved? 
		if (EpochYear < 57) {EpochYear += 2000;} // Sputnik was in 1957
		else if (EpochYear < 100) {EpochYear += 1900;}

		epochTime = d + Constants.getDayNumberSince1900(EpochYear, 1 , 0);
		s = line1.substring(33, 43);
		orbitalDecay = Double.parseDouble(s.trim()); // C nasabare=(33,43)
		s = line1.substring(63, 68);
		elementSet = Integer.parseInt(s.trim()); // G nasabare=(63,68)
		s = line2.substring(8, 16);
		inclination = Double.parseDouble(s.trim()) * Constants.RadiansPerDegree; // H nasabare=(8,16)
		s = line2.substring(17, 25);
		epochRAAN = Double.parseDouble(s.trim()) * Constants.RadiansPerDegree; // I nasabare=(17,25)
		s = line2.substring(26, 33);
		eccentricity = Double.parseDouble("." + s.trim()); // J - append a decimal point nasabare=(26,33)
		s = line2.substring(34, 42);
		epochArgPerigee = Double.parseDouble(s.trim()) * Constants.RadiansPerDegree; // K nasabare=(34,42)
		s = line2.substring(43, 51);
		epochMeanAnomaly = Double.parseDouble(s.trim()) * Constants.RadiansPerDegree; // M nasabare=(43,51)
		s = line2.substring(52, 63);
		epochMeanMotion = Double.parseDouble(s.trim()); // N nasabare=(52,63)
		s = line2.substring(63, 68);
		epochOrbitNum = Integer.parseInt(s.trim()); // R nasabare=(63,68)
		printApogee = (eccentricity >= 0.3); // oval orbit - print out when at farthest distance
		perigeePhase = 0;
		maxPhase = 256; /* Default values */
		numModes = 0;
		beaconFreq *= 1E6; /* Convert to Hz */

		calcPrecession();
	}

	private void calcPrecession() {
		double SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(Constants.MinutesPerDay / epochMeanMotion) / 3);

		raanPrecession = 9.95 * Math.pow(Constants.EarthRadius / SemiMajorAxis, 3.5) * Math.cos(inclination)
				/ ((1 - (eccentricity * eccentricity)) * (1 - (eccentricity * eccentricity)))
				* Constants.RadiansPerDegree;

		perigeePrecession = 4.97 * Math.pow(Constants.EarthRadius / SemiMajorAxis, 3.5)
				* (5 * ((Math.cos(inclination)) * (Math.cos(inclination))) - 1)
				/ ((1 - (eccentricity * eccentricity)) * (1 - (eccentricity * eccentricity)))
				* Constants.RadiansPerDegree;
	}

	/*
	 * Compute the satellite position and velocity in the right ascension (RA) based coordinate system,
	 */
	void calcPosVel(double SemiMajorAxis, double Time, double TrueAnomaly) {

		double RAAN, ArgPerigee;
		double Xw, Yw, VXw, VYw; /* In orbital plane */

		radius = SemiMajorAxis * (1 - (eccentricity * eccentricity)) / (1 + eccentricity * Math.cos(TrueAnomaly));
		Xw = radius * Math.cos(TrueAnomaly);
		Yw = radius * Math.sin(TrueAnomaly);

		double Tmp = Math.sqrt(Constants.GM / (SemiMajorAxis * (1 - (eccentricity * eccentricity))));

		VXw = -Tmp * Math.sin(TrueAnomaly);
		VYw = Tmp * (Math.cos(TrueAnomaly) + eccentricity);

		ArgPerigee = epochArgPerigee + (Time - epochTime) * perigeePrecession;
		RAAN = epochRAAN - (Time - epochTime) * raanPrecession;

		double CosArgPerigee, SinArgPerigee;
		double CosRAAN, SinRAAN, CoSinclination, SinInclination;
		CosRAAN = Math.cos(RAAN);
		SinRAAN = Math.sin(RAAN);
		CosArgPerigee = Math.cos(ArgPerigee);
		SinArgPerigee = Math.sin(ArgPerigee);
		CoSinclination = Math.cos(inclination);
		SinInclination = Math.sin(inclination);

		double Px, Qx, Py, Qy, Pz, Qz; /* Escobal transformation 31 */
		Px = CosArgPerigee * CosRAAN - SinArgPerigee * SinRAAN * CoSinclination;
		Py = CosArgPerigee * SinRAAN + SinArgPerigee * CosRAAN * CoSinclination;
		Pz = SinArgPerigee * SinInclination;
		Qx = -SinArgPerigee * CosRAAN - CosArgPerigee * SinRAAN * CoSinclination;
		Qy = -SinArgPerigee * SinRAAN + CosArgPerigee * CosRAAN * CoSinclination;
		Qz = CosArgPerigee * SinInclination;

		X = Px * Xw + Qx * Yw; /* Escobal, transformation #31 */
		Y = Py * Xw + Qy * Yw;
		Z = Pz * Xw + Qz * Yw;

		VX = Px * VXw + Qx * VYw;
		VY = Py * VXw + Qy * VYw;
		VZ = Pz * VXw + Qz * VYw;
	}

	/* List the satellites in kepler.dat, and return the number found */
	private int ListSatellites() throws IOException {
		char satchar;
		int NumSatellites;

		System.out.println("Available satellites:");

		satchar = 'a';
		NumSatellites = 0;
		List<String> lines = Constants.getLines(new File("kepler.dat"));
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

	void PrintMode(PrintStream OutFile, double Phase, Frame frame) {
		int CurMode;

		for (CurMode = 0; CurMode < numModes; CurMode++) {
			if ((Phase >= modes[CurMode].minPhase && Phase < modes[CurMode].maxPhase)
					|| ((modes[CurMode].minPhase > modes[CurMode].maxPhase)
							&& (Phase >= modes[CurMode].minPhase || Phase < modes[CurMode].maxPhase))) {
				frame.modes[CurMode] = modes[CurMode].modeStr;
				OutFile.print(modes[CurMode].modeStr + " ");
			}
		}
	}

	@Override
	public String toString() {
		return "Satellite [SatName=" + satName + ", BeaconFreq=" + beaconFreq + ", EpochDay=" + epochTime
				+ ", EpochMeanAnomaly=" + epochMeanAnomaly + ", EpochOrbitNum=" + epochOrbitNum + ", EpochRAAN="
				+ epochRAAN + ", epochMeanMotion=" + epochMeanMotion + ", OrbitalDecay=" + orbitalDecay
				+ ", EpochArgPerigee=" + epochArgPerigee + ", Eccentricity=" + eccentricity + ", Inclination="
				+ inclination + ", ElementSet=" + elementSet + ", MaxPhase=" + maxPhase + ", perigeePhase="
				+ perigeePhase + ", NumModes=" + numModes + ", Modes=" + Arrays.toString(modes) + ", PrintApogee="
				+ printApogee + "]";
	}

}