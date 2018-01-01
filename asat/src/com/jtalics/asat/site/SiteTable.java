package com.jtalics.asat.site;

import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.jtalics.n3mo.Site;

public class SiteTable extends JTable {

	private List<Site> sites;

	public SiteTable(List<Site> sites) {
		this.sites = sites;
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
}
