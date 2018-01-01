package com.jtalics.asat.ephemeris;

import javax.swing.table.AbstractTableModel;

import com.jtalics.n3mo.Constants;
import com.jtalics.n3mo.Ephemeris;

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
		return 12;
	}

	// U.T.C. Az El Az' El' Doppler Range Height Lat Long Phase(256.0)
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Ephemeris.Frame frame = getFrameAt(rowIndex);
		switch (columnIndex) {
		case 0:
			return Constants.printTime(frame.currentTime);
		case 1:
			return Math.round(frame.azimuth);
		case 2:
			return Math.round(frame.elevation);
		case 3:
			return Math.round(frame.azimuthFlip);
		case 4:
			return Math.round(frame.elevationFlip);
		case 5:
			return Math.round(frame.doppler);
		case 6:
			return Math.round(frame.range);
		case 7:
			return Math.round(frame.height);
		case 8:
			return Math.round(frame.lat);
		case 9:
			return Math.round(frame.lon);
		case 10:
			return Math.round(frame.phase);
		case 11:
			if (frame.eclipsed)
				return "yes";
			else
				return "";
		default:
			throw new RuntimeException();
		}
	}

	Ephemeris.Frame getFrameAt(int rowIndex) {
		return ephemeris.frames.get(rowIndex);
	}
}
