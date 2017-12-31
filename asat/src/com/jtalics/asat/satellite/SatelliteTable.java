package com.jtalics.asat.satellite;

import java.util.List;

import javax.swing.JTable;

import com.jtalics.asat.Asat;
import com.jtalics.n3mo.Satellite;

public class SatelliteTable extends JTable {

	private List<Satellite> satellites;

	public SatelliteTable(List<Satellite> satellites) {
		this.satellites = satellites;
	}
}
