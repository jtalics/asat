package com.jtalics.asat;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.jtalics.asat.ephemeris.EphemerisPanel;
import com.jtalics.asat.satellite.SatellitePanel;
import com.jtalics.asat.site.SitePanel;

public class MainPanel extends JPanel {

	public final Asat asat;
	public final SatellitePanel satellitePanel;
	public final SitePanel sitePanel;
	public final EphemerisPanel ephemerisPanel;

	public MainPanel(Asat asat) {
		this.asat = asat;
		satellitePanel = new SatellitePanel();
		sitePanel = new SitePanel();
		ephemerisPanel = new EphemerisPanel();
		setLayout(new BorderLayout());
		final JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		add(topSplitPane);
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		topSplitPane.setLeftComponent(splitPane);
		EphemerisPanel ephemerisPanel = getEphemerisPanel();
		topSplitPane.setRightComponent(ephemerisPanel);

		SitePanel sitePanel = getSitePanel();
		splitPane.setLeftComponent(sitePanel);
		SatellitePanel satellitePanel = getSatellitePanel();
		splitPane.setRightComponent(satellitePanel);
		add(new StatusPanel(asat), BorderLayout.SOUTH);	
	};

	private EphemerisPanel getEphemerisPanel() {

		return ephemerisPanel;
	}

	private SatellitePanel getSatellitePanel() {

		return satellitePanel;
	}

	private SitePanel getSitePanel() {
		return sitePanel;
	}
}
