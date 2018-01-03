package com.jtalics.asat.ephemeris;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Constants;
import com.jtalics.n3mo.Ephemeris;
import com.jtalics.n3mo.N3mo;

public class TimeStepPanel extends JPanel {
	
	private final Ephemeris ephemeris;
	private JTextField startTextField = new JTextField();
	private JTextField durationTextField = new JTextField();
	private JTextField endTextField = new JTextField();
	private JTextField stepTextField = new JTextField();
	private JTextField minTextField = new JTextField();
	private JTextField printEclipsesTextField = new JTextField();
	private JTextField flipTextField = new JTextField();

	public TimeStepPanel(Ephemeris ephemeris) {

		// String line = "11 30 2017"; // Nov 30 2017 is the official test date
		this.ephemeris = ephemeris;
		setLayout(new GridLayout(4, 4));
		JLabel startTimeLabel = new JLabel("Start time (y:M:d:h:m:s):");
		/*
		startTimeLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				// https://sourceforge.net/projects/jdatepicker/
				UtilDateModel model = new UtilDateModel();
				Properties p = new Properties();
				p.put("text.today", "Today");
				p.put("text.month", "Month");
				p.put("text.year", "Year");
				model.setDate(2017, 10, 30); // Java month is zero-based
				model.setSelected(true);
				JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
				JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

				JWindow window = new JWindow();
				int X = ev.getX();
				int Y = ev.getY();
				setLocation(getLocation().x + (ev.getX() - X), getLocation().y + (ev.getY() - Y));
				window.getContentPane().add(datePicker);
				window.pack();
				window.setVisible(true);
			}
		});
		*/
		add(startTimeLabel);
		String d = "";
		if (ephemeris != null) {
			double t = ephemeris.startTime;
			d=Constants.makeStandardDateTimeFormattedString(Constants.getDateTime(t));
		}
		ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				update(ev);
				
			}
		};
		startTextField.setText(d);
		startTextField.addActionListener(actionListener);
		add(startTextField);

		add(new JLabel("Duration (days):"));
		durationTextField.setText(String.format("%.4f", ephemeris.endTime - ephemeris.startTime));
		durationTextField.addActionListener(actionListener);
		add(durationTextField);

		d = "";
		if (ephemeris != null) {
			d=Constants.makeStandardDateTimeFormattedString(Constants.getDateTime(ephemeris.endTime));
		}
		add(new JLabel("End time (y:M:d:h:m:s.ss):"));
		endTextField.setText(d);
		endTextField.addActionListener(actionListener);
		add(endTextField);

		add(new JLabel("Step time (min):"));
		d = "";
		if (ephemeris != null) {
			d = Double.toString(ephemeris.stepTime);
		}
		stepTextField.setText(String.format("%.2f", ephemeris.stepTime*Constants.MinutesPerDay));
		stepTextField.addActionListener(actionListener);
		add(stepTextField);
		
		minTextField.setText(String.format("%.2f", ephemeris.siteMinElev*Constants.DegreesPerRadian));
		minTextField.addActionListener(actionListener);
		add(new JLabel("Min elevation (deg):"));
		add(minTextField);
 
		printEclipsesTextField.setText(String.format("%b", ephemeris.printEclipses));
		printEclipsesTextField.addActionListener(actionListener);
		add(new JLabel("Print eclipses?"));
		add(printEclipsesTextField);

		flipTextField.setText(String.format("%b", ephemeris.flip));
		flipTextField.addActionListener(actionListener);
		add(new JLabel("Flip angles?"));
		add(flipTextField);
/*
		int Month, Day, Year;
		String line = "1 1 2018"; // Nov 30 2017 is the official test date
		
		String s[] = line.split(" ");
		Month = Integer.parseInt(s[0]);
		Day = Integer.parseInt(s[1]);
		Year = Integer.parseInt(s[2]);

		startTime = Constants.getDayNum(Year, Month, Day);
		*/
	}

	public class DateLabelFormatter extends AbstractFormatter {

		private String datePattern = "yyyy-MM-dd";
		private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

		@Override
		public Object stringToValue(String text) throws ParseException {
			return dateFormatter.parseObject(text);
		}

		@Override
		public String valueToString(Object value) throws ParseException {
			if (value != null) {
				Calendar cal = (Calendar) value;
				return dateFormatter.format(cal.getTime());
			}

			return "";
		}

	}
	private void update(ActionEvent ev) {

		// endTime and duration are linked
		if (ev.getSource() == startTextField) {
			// calculate endTime based on duration (as is done originally by N3EMO)
			ephemeris.startTime = Constants.getDurationInDaysSinceEpoch1900(Constants.parseStandardDateTimeFormattedString(startTextField.getText()));
			double duration = Double.parseDouble(durationTextField.getText());			
			ephemeris.endTime = ephemeris.startTime + duration;
			endTextField.setText(Constants.makeStandardDateTimeFormattedString(Constants.getDateTime(ephemeris.endTime)));
		}
		else if (ev.getSource() == durationTextField) {
			double duration = Double.parseDouble(durationTextField.getText());			
			ephemeris.endTime = ephemeris.startTime + duration;
			endTextField.setText(Constants.makeStandardDateTimeFormattedString(Constants.getDateTime(ephemeris.endTime)));			
		}
		else if (ev.getSource() == endTextField) {
			// calculate duration based on endTime
			ephemeris.endTime = Constants.getDurationInDaysSinceEpoch1900(Constants.parseStandardDateTimeFormattedString(endTextField.getText()));
			durationTextField.setText(String.format("%.4f",ephemeris.endTime - ephemeris.startTime));			
		}
		else if (ev.getSource() == stepTextField) {
			ephemeris.stepTime = Double.parseDouble(durationTextField.getText());
		}
		else if (ev.getSource() == minTextField) {
			// User is in Degrees, model is in Radians
			ephemeris.siteMinElev = Double.parseDouble(minTextField.getText())*Constants.RadiansPerDegree;
		}
		else if (ev.getSource() == printEclipsesTextField) {
			ephemeris.printEclipses = Boolean.parseBoolean(printEclipsesTextField.getText());
		}
		else if (ev.getSource() == flipTextField) {
			ephemeris.flip = Boolean.parseBoolean(flipTextField.getText());
		}
				
		AsatEventListener.fireEvent(new AsatEvent(this, AsatEvent.EPHEMERIS_SETTINGS_CHANGE, ephemeris));
		return;
	}
}
