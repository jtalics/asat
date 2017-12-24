package com.jtalics.n3mo;

class SolarKeps {

	/*
	 * Initialize the Sun's keplerian elements for a given epoch. Formulas are from
	 * "Explanatory Supplement to the Astronomical Ephemeris"
	 * (https://archive.org/stream/astronomicalalmanac1961/131221-explanatory-
	 * supplement-1961_djvu.txt). Also init the sidereal reference
	 */
	/* values for shadow geometry */
	private final static double Epsilon = Constants.RadiansPerDegree / 3600; /* 1 arc second */

	double SinPenumbra, CosPenumbra;
	double SidDay, SidReference; /* Date and sidereal time */
	/* Keplerian elements for the sun */
	double SunEpochTime, SunInclination, SunRAAN, SunEccentricity, SunArgPerigee, SunMeanAnomaly, SunMeanMotion;
	private final static double SunSemiMajorAxis = 149598845.0; /* Kilometers */
	private final static double SunRadius = 695000;
	final static double SiderealSolar = 1.0027379093;
	final static double SidRate = (Constants.PI2 * SiderealSolar / Constants.SecondsPerDay); /* radians/second */

	SolarKeps(double EpochDay) {
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
		Omega = (259.18 - 1934.142 * T) * Constants.RadiansPerDegree;
		n = (int) (Omega / Constants.PI2); // JTAL
		Omega -= n * Constants.PI2;

		SunEpochTime = EpochDay;
		SunRAAN = 0;

		SunInclination = (23.452294 - 0.0130125 * T - 0.00000164 * T2 + 0.000000503 * T3 + 0.00256 * Math.cos(Omega))
				* Constants.RadiansPerDegree;
		SunEccentricity = (0.01675104 - 0.00004180 * T - 0.000000126 * T2);
		SunArgPerigee = (281.220833 + 1.719175 * T + 0.0004527 * T2 + 0.0000033 * T3) * Constants.RadiansPerDegree;
		SunMeanAnomaly = (358.475845 + 35999.04975 * T - 0.00015 * T2 - 0.00000333333 * T3)
				* Constants.RadiansPerDegree;
		n = (int) (SunMeanAnomaly / Constants.PI2);
		SunMeanAnomaly -= n * Constants.PI2;

		SunMeanMotion = 1 / (365.24219879 - 0.00000614 * T);

		SunTrueAnomaly = Kepler(SunMeanAnomaly, SunEccentricity);
		SunDistance = SunSemiMajorAxis * (1 - (SunEccentricity * SunEccentricity))
				/ (1 + SunEccentricity * Math.cos(SunTrueAnomaly));

		SinPenumbra = (SunRadius - Constants.EarthRadius) / SunDistance;
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
			TrueAnomaly += Constants.PI2;
		}

		return TrueAnomaly;
	}

	boolean Eclipsed(double SatX, double SatY, double SatZ, double SatRadius, double CurrentTime) {
		double MeanAnomaly, TrueAnomaly;
		// double SunX,SunY,SunZ,SunRad;
		// double vx,vy,vz;
		double CosTheta;

		MeanAnomaly = SunMeanAnomaly + (CurrentTime - SunEpochTime) * SunMeanMotion * Constants.PI2;
		TrueAnomaly = Kepler(MeanAnomaly, SunEccentricity);

		double[] sun = Satellite.GetSatPosition(SunEpochTime, SunRAAN, SunArgPerigee, SunSemiMajorAxis, SunInclination,
				SunEccentricity, 0.0, 0.0, CurrentTime, TrueAnomaly); // sun = {SunX,SunY,SunZ,SunRad,vx,vy,vz}

		CosTheta = (sun[0] * SatX + sun[1] * SatY + sun[3] * SatZ) / (sun[4] * SatRadius) * CosPenumbra
				+ (SatRadius / Constants.EarthRadius) * SinPenumbra;

		if (CosTheta < 0)
			if (CosTheta < -Math.sqrt((SatRadius * SatRadius) - (Constants.EarthRadius * Constants.EarthRadius))
					/ SatRadius * CosPenumbra + (SatRadius / Constants.EarthRadius) * SinPenumbra)

				return true;
		return false;
	}
}