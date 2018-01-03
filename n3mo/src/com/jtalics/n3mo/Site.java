package com.jtalics.n3mo;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Site {

	public String siteName;
	public double siteLat, siteLong, siteAltitude;
	double X, Y, Z, VX, VY;

	public Site() throws IOException {

		String line;
		String name = "zerobuoy.sit";

		if (!N3mo.NOCONSOLE) {
			name = System.console().readLine("Site name :").trim() + ".sit";
		}
		List<String> lines = Constants.getLines(new File(name));
		Iterator<String> iter = lines.iterator();
		siteName = iter.next();
		line = iter.next();
		String latitude = "\t\tLatitude";
		if (line.endsWith(latitude))
			line = line.substring(0, line.length() - latitude.length());
		siteLat = Double.parseDouble(line);
		siteLat *= Constants.RadiansPerDegree;

		line = iter.next();
		String longitude = "\t\tLongitude";
		if (line.endsWith(longitude))
			line = line.substring(0, line.length() - longitude.length());
		siteLong = Double.parseDouble(line);
		siteLong *= Constants.RadiansPerDegree;

		line = iter.next();
		String heightMeters = "\t\tHeight (Meters)";
		if (line.endsWith(heightMeters))
			line = line.substring(0, line.length() - heightMeters.length());
		siteAltitude = Double.parseDouble(line);
		siteAltitude /= 1000; // meters to km
/*
		line = iter.next();
		String minElevationDegrees = "\t\tMin Elevation (Degrees)";
		if (line.endsWith(minElevationDegrees))
			line = line.substring(0, line.length() - minElevationDegrees.length());
		SiteMinElev = Double.parseDouble(line);
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
		ObjectMapper mapper = new ObjectMapper();

		// Object to JSON in file
		try {
			mapper.writeValue(new File("site.json"), this);
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
		*/
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
		Lat = Math.atan(1 / (1 - (Constants.EarthFlat * Constants.EarthFlat)) * Math.tan(siteLat));

		CosLat = Math.cos(Lat);
		SinLat = Math.sin(Lat);

		G1 = Constants.EarthRadius / (Math
				.sqrt(1 - (2 * Constants.EarthFlat - (Constants.EarthFlat * Constants.EarthFlat)) * (SinLat * SinLat)));
		G2 = G1 * ((1 - Constants.EarthFlat) * (1 - Constants.EarthFlat));
		G1 += siteAltitude;
		G2 += siteAltitude;
		// }

		SiteRA = Constants.PI2
				* ((CurrentTime - solarKeps.siderealDay) * Sun.SiderealSolar + solarKeps.siderealReference) - siteLong;
		CosRA = Math.cos(SiteRA);
		SinRA = Math.sin(SiteRA);

		X = G1 * CosLat * CosRA;
		Y = G1 * CosLat * SinRA;
		Z = G2 * SinLat;
		VX = -Sun.SiderealRate * Y; // TODO double check, orig could be bad x,y swapped accidentally?
		VY = Sun.SiderealRate * X;

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
/*
	// TODO: move these Ephemeris
	private boolean Flip;
	private double SiteMinElev;
	private boolean PrintEclipses;

	public boolean isFlip() {
		return Flip;
	}

	public double getSiteMinElev() {
		return SiteMinElev;
	}

	public boolean isPrintEclipses() {
		return PrintEclipses;
	}
*/
}