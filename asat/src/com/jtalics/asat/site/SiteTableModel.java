package com.jtalics.asat.site;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.jtalics.n3mo.Site;

public class SiteTableModel extends AbstractTableModel {

	private List<Site> sites;

	public SiteTableModel(List<Site> sites) {
		this.sites = sites;
	}

	@Override
	public int getRowCount() {
		return sites.size();
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Site site = getSiteAt(rowIndex);
		switch (columnIndex) {
		case 0:
			return site.siteName;
		case 1:
			return site.siteLat;
		case 2:
			return site.siteLong;
		case 3:
			return site.siteAltitude;
		default:
			throw new RuntimeException();
		}
	}

	Site getSiteAt(int rowIndex) {
		return sites.get(rowIndex);
	}
}
