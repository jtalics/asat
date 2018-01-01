package com.jtalics.asat.ephemeris;

import javax.swing.JTable;

import com.jtalics.n3mo.Ephemeris;

public class EphemerisTable extends JTable {

	private Ephemeris ephemeris;

	public EphemerisTable(Ephemeris ephemeris) {
		this.ephemeris = ephemeris;
	}
}
