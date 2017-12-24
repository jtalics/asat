package asat;

import javax.swing.table.AbstractTableModel;

import com.jtalics.n3mo.Satellite;

public class SatelliteTableModel extends AbstractTableModel {

	private AppMain app;

	SatelliteTableModel(AppMain app) {
		this.app = app;
	}
	
	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return app.satellites.size();
	}

	@Override
	public int getColumnCount() {
		return 8;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Satellite sat = app.satellites.get(rowIndex);
		switch(columnIndex) {
		case 0:
			return sat.SatName;
		case 1:
			return sat.EpochDay;
		case 2:
			return sat.Inclination;
		case 3:
			return sat.EpochRAAN;
		case 4:
			return sat.Eccentricity;
		case 5:
			return sat.EpochArgPerigee;
		case 6:
			return sat.epochMeanMotion;
		case 7:
			return sat.EpochMeanAnomaly;
		default:
			throw new RuntimeException();
		}
	}
}
