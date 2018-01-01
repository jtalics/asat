package com.jtalics.asat.site;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class SiteTableCellRenderer implements TableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {
		switch (col) {
		case 0:
			return new JLabel("Name");
		case 1:
			return new JLabel("Latitude");
		case 2:
			return new JLabel("Longitude");
		case 3:
			return new JLabel("Altitude");
		default:
			throw new RuntimeException();
		}
	}
}