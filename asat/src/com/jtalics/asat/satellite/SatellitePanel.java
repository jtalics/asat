package com.jtalics.asat.satellite;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import com.jtalics.asat.Asat;
import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Satellite;

public class SatellitePanel extends JPanel implements AsatEventListener {

	public final List<Satellite> satellites = new ArrayList<>();

	public SatellitePanel() {
		super();
		setLayout(new BorderLayout());
		/* read one satellite for testing */
		if (false)
			try { // AMSAT format
				Satellite satellite = new Satellite(new File("kepler.dat"), "a");
				satellites.add(satellite);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		else
			try {
				loadTest(); // TLE format
			} catch (IOException e1) {

				e1.printStackTrace();
			}
		SatelliteTable satelliteTable = new SatelliteTable(satellites);
		add(new JScrollPane(satelliteTable), BorderLayout.CENTER);
		satelliteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		satelliteTable.getSelectionModel()
				.addListSelectionListener(new SatelliteTableListSelectionListener(satellites));
		JTableHeader header = satelliteTable.getTableHeader();
		header.setDefaultRenderer(new SatelliteTableCellRenderer());
		SatelliteTableModel satTableModel = new SatelliteTableModel(satellites);
		satelliteTable.setModel(satTableModel);

		JButton kepsButton = new JButton("Get latest keps");
		kepsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					download();
					((SatelliteTableModel) satelliteTable.getModel()).fireTableDataChanged();
				} catch (IOException e) {
					Asat.logger.log(Level.SEVERE, e.getMessage());
					// TODO notify user
				}
			}
		});
		add(kepsButton, BorderLayout.SOUTH);
		AsatEventListener.addListener(this);
	}

	private void loadTest() throws IOException {
		satellites.clear();

		BufferedReader in = new BufferedReader(new FileReader(new File("nasabare.txt")));

		String inputLine;
		StringBuilder sb = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine).append(";");
		}
		in.close();
		inputLine = sb.toString();
		String lines[] = inputLine.split(";");
		for (int i = 0; i < lines.length; i += 3) {
			Satellite satellite = new Satellite(lines[i], lines[i + 1], lines[i + 2]);
			satellites.add(satellite);
		}
	}

	private void download() throws IOException {

		// TODO: confirm clearing of map
		satellites.clear();

		URL oracle = new URL("https://www.amsat.org/amsat/ftp/keps/current/nasabare.txt");
		BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));

		String inputLine;
		StringBuilder sb = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine).append(";");
		}
		in.close();
		inputLine = sb.toString();
		String lines[] = inputLine.split(";");
		for (int i = 0; i < lines.length; i += 3) {
			Satellite satellite = new Satellite(lines[i], lines[i + 1], lines[i + 2]);
			satellites.add(satellite);
		}
	}

	@Override
	public void eventOccurred(AsatEvent ev) {
		// ignore

	}

}
