package n3mo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import n3mo.AppMain.ModeRec;

class Satellite {

	double BeaconFreq; /* Mhz, used for doppler calc */
	String SatName = "k"; // ISS for example
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
	int ElementSet;
	double MaxPhase; /* Phase units in 1 orbit */
	double perigeePhase;
	int NumModes;
	ModeRec[] Modes = new ModeRec[AppMain.MaxModes];
	boolean PrintApogee = false;
	
	Satellite(AppMain appMain) throws Exception {

		String line, token;
		int EpochYear;
		boolean found;
		int i, NumSatellites;
		char satchar;

		NumSatellites = ListSatellites();

		List<String> lines = Constants.getLines(new File("kepler.dat"));
		Iterator<String> iter = lines.iterator();
		found = false;
		// Use the number to get the satellite name
		while (!found) {
			if (!AppMain.NOCONSOLE) {
				try {
					SatName = System.console().readLine("Letter or satellite name :");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Validate number
			if (SatName.length() == 1) { /* use single character label */
				satchar = SatName.charAt(0);

				if (Constants.LetterNum(satchar) > NumSatellites) {
					System.out.println("'" + satchar + "' is out of range");

					continue;
				}
				// Convert number to satellite name
				i = 0;
				while (iter.hasNext()) {
					line = iter.next();
					if (line.startsWith("Satellite: ")) {
						if ((++i) == Constants.LetterNum(satchar)) {
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

		line = iter.next(); // skip line "Catalog number;"
		line = iter.next();

		token = "Epoch time:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String s = line.substring(token.length() + 1);
		EpochDay = Double.parseDouble(s);
		EpochYear = (int) (EpochDay / 1000.0);
		EpochDay -= EpochYear * 1000.0;
		EpochDay += Constants.GetDayNum(EpochYear, 1, 0);

		// TODO? if (sscanf(str,"Element set: %ld",&ElementSet) == 0)
		// { /* Old style kepler.dat */
		line = iter.next();
		token = "Element set:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		ElementSet = Integer.parseInt(s);

		line = iter.next();
		token = "Inclination:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		String deg = " deg";
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		Inclination = Double.parseDouble(s);
		Inclination *= Constants.RadiansPerDegree;

		line = iter.next();
		token = "RA of node:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		EpochRAAN = Double.parseDouble(s);
		EpochRAAN *= Constants.RadiansPerDegree;

		line = iter.next();
		token = "Eccentricity:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		Eccentricity = Double.parseDouble(line.substring(token.length() + 1));

		line = iter.next();
		token = "Arg of perigee:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		EpochArgPerigee = Double.parseDouble(s);
		EpochArgPerigee *= Constants.RadiansPerDegree;

		line = iter.next();
		token = "Mean anomaly:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		s = line.substring(token.length() + 1);
		if (s.endsWith(deg))
			s = s.substring(0, s.length() - deg.length());
		EpochMeanAnomaly = Double.parseDouble(s);
		EpochMeanAnomaly *= Constants.RadiansPerDegree;

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
		OrbitalDecay = Double.parseDouble(s);

		line = iter.next();
		token = "Epoch rev:";
		if (!line.startsWith(token)) {
			throw new Exception();
		}
		EpochOrbitNum = Integer.parseInt(line.substring(token.length() + 1));

		while (iter.hasNext() && (line = iter.next()).length() > 2) {
			token = "Beacon:";
			if (!line.startsWith(token)) {
				throw new Exception();
			}
			BeaconFreq = Double.parseDouble(line.substring(token.length() + 1));
		}
		PrintApogee = (Eccentricity >= 0.3);
		perigeePhase = 0;
		MaxPhase = 256; /* Default values */
		NumModes = 0;

		lines = Constants.getLines(new File("mode.dat"));

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
				BeaconFreq = Double.parseDouble(line.substring(token.length() + 1));

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
				MaxPhase = Double.parseDouble(line.substring(token.length() + 1));

				line = iter.next();
				Scanner scanner = new Scanner(line);
				token = "Mode: ";

				if (!line.startsWith(token)) {
					scanner.close();
					throw new Exception();
				}
				Modes[NumModes].ModeStr = line.substring(token.length(), token.length() + 1 + 20);
				Modes[NumModes].MinPhase = scanner.nextInt();
				Modes[NumModes].MaxPhase = scanner.nextInt();
				scanner.close();
				if (NumModes >= AppMain.MaxModes) {
					throw new Exception();
				}
				NumModes++;
			}
		}
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

	// returns double[2]={RAANPrecession,PerigeePrecession}
	double[] GetPrecession(double Eccentricity, double Inclination) {
		double SemiMajorAxis = 331.25 * Math.exp(2 * Math.log(Constants.MinutesPerDay / epochMeanMotion) / 3);

		double[] retval = new double[2]; // RAANPrecession,PerigeePrecession
		retval[0] = 9.95 * Math.pow(Constants.EarthRadius / SemiMajorAxis, 3.5) * Math.cos(Inclination)
				/ ((1 - (Eccentricity * Eccentricity)) * (1 - (Eccentricity * Eccentricity))) * Constants.RadiansPerDegree;

		retval[1] = 4.97 * Math.pow(Constants.EarthRadius / SemiMajorAxis, 3.5)
				* (5 * ((Math.cos(Inclination)) * (Math.cos(Inclination))) - 1)
				/ ((1 - (Eccentricity * Eccentricity)) * (1 - (Eccentricity * Eccentricity))) * Constants.RadiansPerDegree;
		return retval;
	}

	/*
	 * Compute the satellite postion and velocity in the RA based coordinate system,
	 * returns array: double[7] {X,Y,Z,Radius,VX,VY,VZ;}
	 */

	static double[] GetSatPosition(double EpochTime, double EpochRAAN, double EpochArgPerigee, double SemiMajorAxis,
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

		Tmp = Math.sqrt(Constants.GM / (SemiMajorAxis * (1 - (Eccentricity * Eccentricity))));

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
}