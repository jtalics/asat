package com.jtalics.asat.ephemeris;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class EphemerisTableCellRenderer implements TableCellRenderer {

	public EphemerisTableCellRenderer() {
	}

	// U.T.C. Az El Az' El' Doppler Range Height Lat Long Phase(256.0)
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		switch (col) {
		case 0:
			return new JLabel("U.T.C.");
		case 1:
			return new JLabel("Az");
		case 2:
			return new JLabel("El");
		case 3:
			return new JLabel("Az'");
		case 4:
			return new JLabel("El'");
		case 5:
			return new JLabel("Doppler");
		case 6:
			return new JLabel("Range");
		case 7:
			return new JLabel("Height");
		case 8:
			return new JLabel("Lat");
		case 9:
			return new JLabel("Long");
		case 10:
			return new JLabel("Phase");
		case 11:
			return new JLabel("Eclipse");
		default:
			throw new RuntimeException();
		}
	}
}