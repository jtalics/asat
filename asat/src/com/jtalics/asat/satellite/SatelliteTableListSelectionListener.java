package com.jtalics.asat.satellite;

import java.util.List;
import javax.swing.DefaultListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Satellite;

public final class SatelliteTableListSelectionListener implements ListSelectionListener {

	private List<Satellite> satellites;

	public SatelliteTableListSelectionListener(List<Satellite> satellites) {
		super();
		this.satellites = satellites;
	}

	@Override
	public void valueChanged(ListSelectionEvent ev) {
		if (ev.getValueIsAdjusting()) {
			return;
		}
		
		for (int row=ev.getFirstIndex(); row<=ev.getLastIndex(); row++) {
			if (((DefaultListSelectionModel)ev.getSource()).isSelectedIndex(row)) {
				AsatEventListener.fireEvent(
						new AsatEvent(satellites, AsatEvent.SATELLITE_SELECTED, satellites.get((row))));
				}			
		}
	}
}