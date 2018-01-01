package com.jtalics.n3mo;

// The sun is considered a satellite of the earth -- TODO: should we extend Satellite then?
class Sun {

	/*
	 * Initialize the Sun's keplerian elements for a given epoch. Formulas are from
	 * "Explanatory Supplement to the Astronomical Ephemeris"
	 * (https://archive.org/stream/astronomicalalmanac1961/131221-explanatory-
	 * supplement-1961_djvu.txt). Also init the sidereal reference
	 */
	/* values for shadow geometry */
	private final static double Epsilon = Constants.RadiansPerDegree / 3600; /* 1 arc second */

	private final double sinPenumbra, cosPenumbra;
	final double siderealDay, siderealReference; /* Date and sidereal time */
	/* Keplerian elements for the sun */
	private double sunEpochTime, sunInclination, sunRAAN, sunEccentricity, sunArgPerigee, sunMeanAnomaly, sunMeanMotion;
	private final static double SunSemiMajorAxis = 149598845.0; /* Kilometers */
	private final static double SunRadius = 695000;
	final static double SiderealSolar = 1.0027379093;
	final static double SiderealRate = (Constants.PI2 * SiderealSolar / Constants.SecondsPerDay); /* radians/second */

	Sun(final double EpochDay) {

		double T, T2, T3, Omega;
		int n;
		double SunTrueAnomaly, SunDistance;

		T = (Math.floor(EpochDay) - 0.5) / 36525;
		T2 = T * T;
		T3 = T2 * T;

		siderealDay = Math.floor(EpochDay);

		double d = (6.6460656 + 2400.051262 * T + 0.00002581 * T2) / 24;
		siderealReference = d - Math.floor(d);

		/* Omega is used to correct for the nutation and the abberation */
		Omega = (259.18 - 1934.142 * T) * Constants.RadiansPerDegree;
		n = (int) (Omega / Constants.PI2); // JTAL
		Omega -= n * Constants.PI2;

		sunEpochTime = EpochDay;
		sunRAAN = 0;

		sunInclination = (23.452294 - 0.0130125 * T - 0.00000164 * T2 + 0.000000503 * T3 + 0.00256 * Math.cos(Omega))
				* Constants.RadiansPerDegree;
		sunEccentricity = (0.01675104 - 0.00004180 * T - 0.000000126 * T2);
		sunArgPerigee = (281.220833 + 1.719175 * T + 0.0004527 * T2 + 0.0000033 * T3) * Constants.RadiansPerDegree;
		sunMeanAnomaly = (358.475845 + 35999.04975 * T - 0.00015 * T2 - 0.00000333333 * T3)
				* Constants.RadiansPerDegree;
		n = (int) (sunMeanAnomaly / Constants.PI2);
		sunMeanAnomaly -= n * Constants.PI2;

		sunMeanMotion = 1 / (365.24219879 - 0.00000614 * T);

		SunTrueAnomaly = Kepler(sunMeanAnomaly, sunEccentricity);
		SunDistance = SunSemiMajorAxis * (1 - (sunEccentricity * sunEccentricity))
				/ (1 + sunEccentricity * Math.cos(SunTrueAnomaly));

		sinPenumbra = (SunRadius - Constants.EarthRadius) / SunDistance;
		cosPenumbra = Math.sqrt(1 - (sinPenumbra * sinPenumbra));
	}

	private long calls = 0;
	private double X, Y, Z, VX, VY, VZ, radius;

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
		long iters = 0;

		E = MeanAnomaly;/* + Eccentricity*sin(MeanAnomaly); /* Initial guess */
		do {
			Error = (E - Eccentricity * Math.sin(E) - MeanAnomaly) / (1 - Eccentricity * Math.cos(E));
			E -= Error;
			iters++;
		} while (Math.abs(Error) >= Epsilon && iters < 1000);
		if (iters >= 1000) {
			throw new RuntimeException("Sun.Kepler failed on iters");
		}

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

	boolean Eclipsed(Satellite sat, double CurrentTime) {

		double MeanAnomaly, TrueAnomaly;

		MeanAnomaly = sunMeanAnomaly + (CurrentTime - sunEpochTime) * sunMeanMotion * Constants.PI2;
		TrueAnomaly = Kepler(MeanAnomaly, sunEccentricity);

		calcPosVel(SunSemiMajorAxis, 0.0, 0.0, CurrentTime, TrueAnomaly);

		double CosTheta = (X * sat.X + Y * sat.Y + Z * sat.Z) / (radius * sat.radius) * cosPenumbra
				+ (sat.radius / Constants.EarthRadius) * sinPenumbra;

		if (CosTheta < 0) {
			if (CosTheta < -Math.sqrt((sat.radius * sat.radius) - (Constants.EarthRadius * Constants.EarthRadius))
					/ sat.radius * cosPenumbra + (sat.radius / Constants.EarthRadius) * sinPenumbra) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Compute the satellite postion and velocity in the RA based coordinate system,
	 */
	private void calcPosVel(double SemiMajorAxis, double RAANPrecession, double PerigeePrecession, double Time,
			double TrueAnomaly) {

		double RAAN, ArgPerigee;
		double Xw, Yw, VXw, VYw; /* In orbital plane */

		radius = SemiMajorAxis * (1 - (sunEccentricity * sunEccentricity))
				/ (1 + sunEccentricity * Math.cos(TrueAnomaly));
		Xw = radius * Math.cos(TrueAnomaly);
		Yw = radius * Math.sin(TrueAnomaly);

		double Tmp = Math.sqrt(Constants.GM / (SemiMajorAxis * (1 - (sunEccentricity * sunEccentricity))));

		VXw = -Tmp * Math.sin(TrueAnomaly);
		VYw = Tmp * (Math.cos(TrueAnomaly) + sunEccentricity);

		ArgPerigee = sunArgPerigee + (Time - sunEpochTime) * PerigeePrecession;
		RAAN = sunRAAN - (Time - sunEpochTime) * RAANPrecession;

		double CosArgPerigee, SinArgPerigee;
		double CosRAAN, SinRAAN, CoSinclination, SinInclination;
		CosRAAN = Math.cos(RAAN);
		SinRAAN = Math.sin(RAAN);
		CosArgPerigee = Math.cos(ArgPerigee);
		SinArgPerigee = Math.sin(ArgPerigee);
		CoSinclination = Math.cos(sunInclination);
		SinInclination = Math.sin(sunInclination);

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
}