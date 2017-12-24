package asat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.jtalics.n3mo.Satellite;


public class MainPanel extends JPanel {

	private AppMain app;

	public MainPanel(AppMain app) {
		
		this.app=app;
		setLayout(new BorderLayout());
		SatelliteTable satTable = new SatelliteTable(app);
		add(new JScrollPane(satTable),BorderLayout.CENTER);
		JTableHeader header = satTable.getTableHeader();
		header.setDefaultRenderer(new TableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int col) {
				switch(col) {
				case 0:
					return new JLabel("Name");
				case 1:
					return new JLabel("EpochTime");
				case 2:
					return new JLabel("Inclination");
				case 3:
					return new JLabel("RAAN");
				case 4:
					return new JLabel("Eccentricity");
				case 5:
					return new JLabel("Arg of Perigee");
				case 6:
					return new JLabel("Mean Motion");
				case 7:
					return new JLabel("mean Anomaly");
				default:
					throw new RuntimeException();
				}
			}
		});
		SatelliteTableModel satTableModel = new SatelliteTableModel(app);
		satTable.setModel(satTableModel);
		JButton button = new JButton("Get latest keps");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					download();
					satTableModel.fireTableDataChanged();
				} catch (IOException e) {
					app.logger.log(Level.SEVERE,e.getMessage());
					// TODO notify user
				}
			}
		});
		add(button,BorderLayout.NORTH);		
	};
	
	private void download() throws IOException {

		// TODO: confirm clearing of map
		app.satellites.clear();
		
		URL oracle = new URL("https://www.amsat.org/amsat/ftp/keps/current/nasabare.txt");
        BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));

        String inputLine;
        StringBuilder sb = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {sb.append(inputLine).append(";");}
        in.close();
        inputLine = sb.toString();
        String lines[] = inputLine.split(";");
        for (int i = 0; i< lines.length; i+=3) {
        	Satellite satellite = new Satellite(lines[i],lines[i+1],lines[i+2]);
        	app.satellites.add(satellite);
        }
	}

	public static void main(String[] args) {
		System.out.println("RUNNING MAIN");
	}

}
