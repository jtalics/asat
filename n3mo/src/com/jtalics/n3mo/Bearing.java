package com.jtalics.n3mo;

class Bearing {
	double elevation, azimuth;

	void calc(Satellite sat, Site site, double[][] SiteMatrix) {
		double[] xyz = new double[3];

		xyz = GetTopocentric(sat.X, sat.Y, sat.Z, site.X, site.Y, site.Z, SiteMatrix);

		elevation = Math.atan(xyz[2] / Math.sqrt((xyz[0] * xyz[0]) + (xyz[1] * xyz[1]))); // Elevation

		azimuth = Math.PI - Math.atan2(xyz[1], xyz[0]); // Azimuth

		if (azimuth < 0) {
			azimuth += Math.PI;
		}
	}

	/*
	 * Convert from geocentric RA based coordinates to topocentric (observer centered) coordinates
	 */
	private double[] GetTopocentric(double SatX, double SatY, double SatZ, double SiteX, double SiteY, double SiteZ,
			double[][] SiteMatrix) {

		double[] retval = new double[3];
		SatX -= SiteX;
		SatY -= SiteY;
		SatZ -= SiteZ;

		retval[0] = SiteMatrix[0][0] * SatX + SiteMatrix[0][1] * SatY + SiteMatrix[0][2] * SatZ;
		retval[1] = SiteMatrix[1][0] * SatX + SiteMatrix[1][1] * SatY + SiteMatrix[1][2] * SatZ;
		retval[2] = SiteMatrix[2][0] * SatX + SiteMatrix[2][1] * SatY + SiteMatrix[2][2] * SatZ;
		return retval;
	}
}