package asat;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jtalics.n3mo.Satellite;

final class SatelliteTableListSelectionListener implements ListSelectionListener {

    private final SatelliteTable satTable;

	public void valueChanged(ListSelectionEvent e) {
		Satellite sat = ((SatelliteTableModel)this.satTable.getModel()).getSatelliteAt(e.getFirstIndex());
		SatelliteDialog satDialog = new SatelliteDialog(satTable.app, sat);
	}

	public SatelliteTableListSelectionListener(SatelliteTable satelliteTable) {
		super();
		this.satTable = satelliteTable;
	}
}