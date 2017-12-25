package com.jtalics.n3mo;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

class Site {
	/* Site Parameters */
	public String SiteName;
	public double SiteLat, SiteLong, SiteAltitude, SiteMinElev;
	public boolean PrintEclipses;
	public boolean Flip;
	double X;
	double Y;
	double Z;
	double VX;
	double VY;

	Site() throws IOException {

		String line;
		String name = "zerobuoy.sit";

		if (!N3mo.NOCONSOLE) {
			name = System.console().readLine("Site name :").trim() + ".sit";
		}
		List<String> lines = Constants.getLines(new File(name));
		Iterator<String> iter = lines.iterator();
		SiteName = iter.next();
		line = iter.next();
		String latitude = "\t\tLatitude";
		if (line.endsWith(latitude))
			line = line.substring(0, line.length() - latitude.length());
		SiteLat = Double.parseDouble(line);
		SiteLat *= Constants.RadiansPerDegree;

		line = iter.next();
		String longitude = "\t\tLongitude";
		if (line.endsWith(longitude))
			line = line.substring(0, line.length() - longitude.length());
		SiteLong = Double.parseDouble(line);
		SiteLong *= Constants.RadiansPerDegree;

		line = iter.next();
		String heightMeters = "\t\tHeight (Meters)";
		if (line.endsWith(heightMeters))
			line = line.substring(0, line.length() - heightMeters.length());
		SiteAltitude = Double.parseDouble(line);
		SiteAltitude /= 1000; // meters to km

		line = iter.next();
		String minElevationDegrees = "\t\tMin Elevation (Degrees)";
		if (line.endsWith(minElevationDegrees))
			line = line.substring(0, line.length() - minElevationDegrees.length());
		SiteAltitude = Double.parseDouble(line);
		SiteMinElev *= Constants.RadiansPerDegree;

		Flip = PrintEclipses = false;
		while (iter.hasNext()) {
			line = iter.next();
			if (line.startsWith("Flip")) {
				Flip = true;
			} else if (line.startsWith("Eclipse")) {
				PrintEclipses = true;
			} else
				System.err.println(name + " unknown option: '" + line + "'");
		}
	}
	
	/*
	 * Compute the site position and velocity in the RA based coordinate system.
	 * SiteMatrix is set to a matrix which is used by GetTopoCentric to convert
	 * geocentric coordinates to topocentric (observer-centered) coordinates.
	 * Returns double[]={SiteX,SiteY,SiteZ,SiteVX,SiteVY,} and modifies SiteMatrix
	 */

	void calcPosVel(Sun solarKeps, double CurrentTime, double[][] SiteMatrix) {

		double G1, G2; /* Used to correct for flattening of the Earth */
		double CosLat, SinLat;
		double Lat;
		double SiteRA; /* Right Ascension of site */
		double CosRA, SinRA;

		// if ((SiteLat != OldSiteLat) || (SiteElevation != OldSiteElevation)) {
		// OldSiteLat = SiteLat;
		// OldSiteElevation = SiteElevation;
		Lat = Math.atan(1 / (1 - (Constants.EarthFlat * Constants.EarthFlat)) * Math.tan(SiteLat));

		CosLat = Math.cos(Lat);
		SinLat = Math.sin(Lat);

		G1 = Constants.EarthRadius / (Math.sqrt(1 - (2 * Constants.EarthFlat - (Constants.EarthFlat * Constants.EarthFlat)) * (SinLat * SinLat)));
		G2 = G1 * ((1 - Constants.EarthFlat) * (1 - Constants.EarthFlat));
		G1 += SiteAltitude;
		G2 += SiteAltitude;
		// }

		SiteRA = Constants.PI2 * ((CurrentTime - solarKeps.SidDay) * Sun.SiderealSolar + solarKeps.SidReference) - SiteLong;
		CosRA = Math.cos(SiteRA);
		SinRA = Math.sin(SiteRA);

		X = G1 * CosLat * CosRA;
		Y = G1 * CosLat * SinRA;
		Z = G2 * SinLat;
		VX = -Sun.SidRate * Y; // TODO double check, orig could be bad
		VY = Sun.SidRate * X;

		SiteMatrix[0][0] = SinLat * CosRA;
		SiteMatrix[0][1] = SinLat * SinRA;
		SiteMatrix[0][2] = -CosLat;
		SiteMatrix[1][0] = -SinRA;
		SiteMatrix[1][1] = CosRA;
		SiteMatrix[1][2] = 0.0;
		SiteMatrix[2][0] = CosRA * CosLat;
		SiteMatrix[2][1] = SinRA * CosLat;
		SiteMatrix[2][2] = SinLat;
	}


}