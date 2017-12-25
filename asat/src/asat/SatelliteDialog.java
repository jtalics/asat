package asat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jtalics.n3mo.Satellite;

public class SatelliteDialog extends JDialog {
	Asat app;
	Satellite sat;
	public SatelliteDialog(Asat app, Satellite sat) {
		this.app = app;
		this.sat = sat;
		setModal(false);
		add(getTopPanel());
		setTitle(sat.SatName);
		pack();
		setVisible(true);
	}
	
	private JPanel getTopPanel() {
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		add(topPanel);
		topPanel.add(getEphemerisButton());
		return topPanel;
	}

	private JButton getEphemerisButton() {
		JButton button = new JButton("Get Ephemeris");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				app.calcEphemeris(sat);
			}
			
		});
		return button;
	}
}
