package com.jtalics.asat.satellite;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.jtalics.asat.Asat;
import com.jtalics.n3mo.Satellite;

public class SatelliteTableModel extends AbstractTableModel {


	private List<Satellite> satellites;

	public SatelliteTableModel(List<Satellite> satellites) {
		this.satellites = satellites;
	}
	
	@Override
	public int getRowCount() {
		
		return satellites.size();
	}

	@Override
	public int getColumnCount() {
		return 8;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Satellite sat = getSatelliteAt(rowIndex);
		switch(columnIndex) {
		case 0:
			return sat.SatName;
		case 1:
			return sat.EpochTime;
		case 2:
			return sat.Inclination; // i
		case 3:
			return sat.EpochRAAN; // TODO: big omega
		case 4:
			return sat.Eccentricity;
		case 5:
			return sat.EpochArgPerigee; // little omega
		case 6:
			return sat.epochMeanMotion;
		case 7:
			return sat.EpochMeanAnomaly;
		default:
			throw new RuntimeException();
		}
	}

	Satellite getSatelliteAt(int rowIndex) {
		return satellites.get(rowIndex);
	}
}
