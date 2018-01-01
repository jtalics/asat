package com.jtalics.asat.ephemeris;

import java.awt.GridLayout;
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

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.jtalics.n3mo.Ephemeris;

public class TimeStepPanel extends JPanel {
	final Ephemeris ephemeris;

	public TimeStepPanel(Ephemeris ephemeris) {

		this.ephemeris = ephemeris;
		setLayout(new GridLayout(2, 4));
		JLabel startTimeLabel = new JLabel("Start time:");
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
		add(startTimeLabel);
		String d = "";
		if (ephemeris != null) {
			d = Double.toString(ephemeris.startTime);
		}
		JTextField startTextField = new JTextField(d);
		startTextField.setText(Double.toString(ephemeris.startTime));
		add(startTextField);

		add(new JLabel("Duration (days):"));
		JTextField durationTextField = new JTextField("");
		add(durationTextField);

		d = "";
		if (ephemeris != null) {
			d = Double.toString(ephemeris.endTime);
		}
		add(new JLabel("End time:"));
		JTextField finishTextField = new JTextField(d);
		finishTextField.setText(Double.toString(ephemeris.endTime));
		add(finishTextField);

		add(new JLabel("Step time:"));
		d = "";
		if (ephemeris != null) {
			d = Double.toString(ephemeris.stepTime);
		}
		JTextField stepTextField = new JTextField(d);
		stepTextField.setText(Double.toString(ephemeris.stepTime));
		add(stepTextField);
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
}
