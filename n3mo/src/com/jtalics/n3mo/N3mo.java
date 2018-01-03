package com.jtalics.n3mo;

import java.io.File;
import java.io.PrintStream;

public class N3mo {
	static boolean NOCONSOLE = true;
	static PrintStream outPrintStream = System.out;

	// RUN FROM CONSOLE
	public static void main(String[] args) throws Exception {

		if (System.console() == null) {
			NOCONSOLE = true; // probably running in IDE or debugger
			// throw new Exception("Cannot open a console");
		}
		System.out.println("Current directory is " + System.getProperty("user.dir"));
		String FileName = null;

		System.out.println(Constants.VersionStr);

		Satellite satellite = new Satellite(new File("kepler.dat"), "k");
		Site site = new Site();
		Ephemeris ephemeris = new Ephemeris(site, satellite);

		if (!NOCONSOLE) {
			FileName = System.console().readLine("Output file (RETURN for TTY): ");
		}
		if (FileName == null || FileName.length() > 0) {
			File file = new File(FileName, satellite.satName + ".eph");
			outPrintStream = new PrintStream(file);
		}

		outPrintStream.println(satellite.satName + " Element Set " + satellite.elementSet);

		outPrintStream.println(site.siteName);

		outPrintStream.println("Doppler calculated for freq = " + satellite.beaconFreq);

		ephemeris.calc();

		outPrintStream.close();
	}

	public static Ephemeris calcEphemeris(Site site, Satellite satellite) {

		Ephemeris ephemeris = new Ephemeris(site, satellite);
		ephemeris.calc();
		return ephemeris;
	}
}
