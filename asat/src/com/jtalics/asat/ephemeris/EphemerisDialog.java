package com.jtalics.asat.ephemeris;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jtalics.asat.Asat;
import com.jtalics.n3mo.Satellite;
import com.jtalics.n3mo.Site;

public class EphemerisDialog extends JDialog {

	private final Asat app;
	private final Satellite satellite;
	private final Site site;

	public EphemerisDialog(Asat app, Site site, Satellite sat) {
		this.app = app;
		this.site = site;
		this.satellite = sat;
		setModal(false);
		add(getTopPanel());
		setTitle(sat.satName);
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
				app.calcEphemeris(site, satellite);
			}

		});
		return button;
	}
}
