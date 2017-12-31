package com.jtalics.asat.satellite;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public final class SatelliteTableCellRenderer implements TableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int col) {
		switch(col) {
		case 0:
			return new JLabel("Name");
		case 1:
			return new JLabel("EpochTime");
		case 2:
			return new JLabel("Inclination");
		case 3:
			return new JLabel("RAAN");
		case 4:
			return new JLabel("Eccentricity");
		case 5:
			return new JLabel("Arg of Perigee");
		case 6:
			return new JLabel("Mean Motion");
		case 7:
			return new JLabel("mean Anomaly");
		default:
			throw new RuntimeException();
		}
	}
}