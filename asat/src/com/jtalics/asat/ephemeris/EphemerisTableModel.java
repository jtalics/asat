package com.jtalics.asat.ephemeris;

import javax.swing.table.AbstractTableModel;

import com.jtalics.asat.Asat;
import com.jtalics.n3mo.Ephemeris;
import com.jtalics.n3mo.Satellite;

public class EphemerisTableModel extends AbstractTableModel {

	
	private Ephemeris ephemeris;

	public EphemerisTableModel(Ephemeris ephemeris) {
		this.ephemeris = ephemeris;
	}

	@Override
	public int getRowCount() {
		return ephemeris.frames.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}
//U.T.C.   Az  El   Az'  El' Doppler Range Height  Lat  Long  Phase(256.0)
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Ephemeris.Frame frame = getFrameAt(rowIndex);
		switch(columnIndex) {
		case 0:
			return frame.currentTime;
		case 1:
			return frame.azimuth;
		case 2:
			return frame.elevation;
		case 3:
			return frame.azimuthFlip;
		case 4:
			return frame.elevationFlip;
		case 5:
			return frame.doppler;
		case 6:
			return frame.range;
		case 7:
			return frame.height;
		case 8:
			return frame.lat;
		case 9:
			return frame.lon;
		case 10:
			return frame.phase;
		default:
			throw new RuntimeException();
		}
	}

	Ephemeris.Frame getFrameAt(int rowIndex) {
		return ephemeris.frames.get(rowIndex);
	}
}
