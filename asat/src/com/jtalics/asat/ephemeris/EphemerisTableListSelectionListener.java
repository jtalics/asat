package com.jtalics.asat.ephemeris;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Ephemeris;

public class EphemerisTableListSelectionListener implements ListSelectionListener {

	private Ephemeris ephemeris;

	public EphemerisTableListSelectionListener(Ephemeris ephemeris) {
		super();
		this.ephemeris = ephemeris;
	}

	@Override
	public void valueChanged(ListSelectionEvent ev) {
		if (ev.getValueIsAdjusting()) {
			return;
		}
		for (int row=ev.getFirstIndex(); row<=ev.getLastIndex(); row++) {
			if (((DefaultListSelectionModel)ev.getSource()).isSelectedIndex(row)) {
				AsatEventListener.fireEvent(
						new AsatEvent(ephemeris, AsatEvent.EPHEMERIS_FRAME_SELECTED, ephemeris.frames.get((row))));
				}			
		}
	}
}