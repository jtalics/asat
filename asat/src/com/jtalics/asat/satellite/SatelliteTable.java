package com.jtalics.asat.satellite;

import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.jtalics.n3mo.Satellite;

public class SatelliteTable extends JTable {

	private List<Satellite> satellites;

	public SatelliteTable(List<Satellite> satellites) {
		this.satellites = satellites;
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
}
