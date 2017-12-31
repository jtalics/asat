package com.jtalics.asat.ephemeris;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Ephemeris;
import com.jtalics.n3mo.Satellite;
import com.jtalics.n3mo.Site;

public class EphemerisPanel extends JPanel implements AsatEventListener {

	public Ephemeris ephemeris = new Ephemeris();
	EphemerisTableModel ephemerisTableModel;

	public EphemerisPanel() {
		super();
		setLayout(new BorderLayout());
		EphemerisTable ephemerisTable = new EphemerisTable(ephemeris);
		add(new JScrollPane(ephemerisTable), BorderLayout.CENTER);
		ephemerisTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ephemerisTable.getSelectionModel().addListSelectionListener(new EphemerisTableListSelectionListener(ephemeris));
		JTableHeader header = ephemerisTable.getTableHeader();
		header.setDefaultRenderer(new EphemerisTableCellRenderer());
		ephemerisTableModel = new EphemerisTableModel(ephemeris);
		ephemerisTable.setModel(ephemerisTableModel);
		AsatEventListener.addListener(this);
		add(new TimeStepPanel(ephemeris),BorderLayout.SOUTH);
	}

	@Override
	public void eventOccurred(AsatEvent ev) {
		
		switch(ev.id) {
		case AsatEvent.SITE_SELECTED:
			ephemeris.setSite((Site)ev.arg);
			if (ephemeris.calc()) ephemerisTableModel.fireTableDataChanged();		
			break;
		case AsatEvent.SATELLITE_SELECTED:
			ephemeris.setSatellite((Satellite)ev.arg);
			if (ephemeris.calc()) ephemerisTableModel.fireTableDataChanged();		
			break;
		case AsatEvent.EPHEMERIS_FRAME_SELECTED:
			ephemeris.calc();
			ephemerisTableModel.fireTableDataChanged();		
			break;
		}
		
	}
}
