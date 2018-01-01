package com.jtalics.asat.site;

import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Site;

public class SiteTableListSelectionListener implements ListSelectionListener {

	private List<Site> sites;

	public SiteTableListSelectionListener(List<Site> sites) {
		super();
		this.sites = sites;
	}

	@Override
	public void valueChanged(ListSelectionEvent ev) {
		if (ev.getValueIsAdjusting()) {
			return;
		}
		for (int row=ev.getFirstIndex(); row<=ev.getLastIndex(); row++) {
			if (((DefaultListSelectionModel)ev.getSource()).isSelectedIndex(row)) {
				AsatEventListener.fireEvent(
						new AsatEvent(sites, AsatEvent.SITE_SELECTED, sites.get((row))));
				}			
		}
	}

}