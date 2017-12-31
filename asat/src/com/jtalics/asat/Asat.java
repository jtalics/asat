package com.jtalics.asat;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.jtalics.n3mo.N3mo;
import com.jtalics.n3mo.Satellite;
import com.jtalics.n3mo.Site;

public class Asat {
	
	public static Logger logger =Logger.getLogger("ASAT");
	public JFrame frame;
	public MainPanel mainPanel;

	public Asat() {
		mainPanel=new MainPanel(this);
	}
	
	private void createAndShowGUI(Asat asat) {
		JFrame frame = new JFrame("ASAT v2018.00.00");
		asat.frame = frame;
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(mainPanel);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new JMenu("File"));
		frame.getRootPane().setJMenuBar(menuBar);
		// Display the window.
		frame.setSize(1500, 500);
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException {
		Asat asat = new Asat();
		asat.logger.setLevel(Level.INFO);
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				asat.createAndShowGUI(asat);
			}
		});
	}

	public void calcEphemeris(Site site, Satellite satellite) {
		N3mo.calcEphemeris(site, satellite);
	}

}
