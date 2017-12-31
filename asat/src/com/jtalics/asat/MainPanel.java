package com.jtalics.asat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import com.jtalics.asat.ephemeris.EphemerisPanel;
import com.jtalics.asat.ephemeris.EphemerisTable;
import com.jtalics.asat.ephemeris.EphemerisTableCellRenderer;
import com.jtalics.asat.ephemeris.EphemerisTableListSelectionListener;
import com.jtalics.asat.ephemeris.EphemerisTableModel;
import com.jtalics.asat.satellite.SatellitePanel;
import com.jtalics.asat.satellite.SatelliteTable;
import com.jtalics.asat.satellite.SatelliteTableCellRenderer;
import com.jtalics.asat.satellite.SatelliteTableListSelectionListener;
import com.jtalics.asat.satellite.SatelliteTableModel;
import com.jtalics.asat.site.SitePanel;
import com.jtalics.asat.site.SiteTable;
import com.jtalics.asat.site.SiteTableCellRenderer;
import com.jtalics.asat.site.SiteTableListSelectionListener;
import com.jtalics.asat.site.SiteTableModel;
import com.jtalics.n3mo.Ephemeris;
import com.jtalics.n3mo.Satellite;
import com.jtalics.n3mo.Site;


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
	    add(new StatusPanel(asat),BorderLayout.SOUTH);
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
