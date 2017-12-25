package com.jtalics.n3mo;

public class Range {
	double range;
	double rangeRate;

	// returns double[] = {Range,RangeRate}
	void GetRange(Site site, Satellite sat) {
		double DX, DY, DZ;

		DX = sat.X - site.X;
		DY = sat.Y - site.Y;
		DZ = sat.Z - site.Z;

		range = Math.sqrt((DX * DX) + (DY * DY) + (DZ * DZ)); // Range
		rangeRate = ((sat.VX - site.VX) * DX + (sat.VY - site.VY) * DY + sat.VZ /*why not "-SiteVX"? it is always zero*/ * DZ) / range; // RangeRate
	}
}
