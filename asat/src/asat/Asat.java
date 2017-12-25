package asat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.jtalics.n3mo.Satellite;

public class Asat {
	
	Logger logger =Logger.getLogger("ASAT");
    public final List<Satellite> satellites = new ArrayList<>();
	
	private static void createAndShowGUI(Asat app) {
		JFrame frame = new JFrame("ASAT v2018.00.00");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(new MainPanel(app));

		// Display the window.
		frame.setSize(500, 500);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		Asat app = new Asat();
		app.logger.setLevel(Level.INFO);
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(app);
			}
		});
	}

	public void calcEphemeris(Satellite sat) {
		
	}
}
