package com.jtalics.asat.ephemeris;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

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
		add(startTimeLabel);
		String d = "";
		if (ephemeris != null) {
			double t = ephemeris.startTime;
			d=Constants.makeStandardDateTimeFormattedString(Constants.getDateTime(t));
		}
		final JPopupMenu popup = buildPopupMenu();
		MouseListener popupMouseListener = new MouseAdapter() {
 
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
 
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
 
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(),
                            e.getX(), e.getY());
                }
            }
        };
  
		ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				update(ev);
				
			}
		};
		startTextField.setText(d);
		startTextField.addActionListener(actionListener);
		startTextField.addMouseListener(popupMouseListener);
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
			String text = startTextField.getText();
			ephemeris.startTime = Constants.getDurationInDaysSinceEpoch1900(Constants.parseStandardDateTimeFormattedString(text));
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
	
	private JPopupMenu buildPopupMenu() {

        final JPopupMenu popup = new JPopupMenu();
        // New project menu item
        JMenuItem menuItem = new JMenuItem("Current time",
                new ImageIcon("images/x.png"));
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Current Time");
        menuItem.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e) {
            	Component invoker = popup.getInvoker();
            	if (invoker == startTextField) {
            		ZonedDateTime current = ZonedDateTime.now(ZoneOffset.UTC);
            		int[] now = new int[7];
            		now[0] = current.getYear();
            		now[1] = current.getMonthValue();
            		now[2] = current.getDayOfMonth();
            		now[3] = current.getHour();
            		now[4] = current.getMinute();
            		now[5] = current.getSecond();
            		now[6] = current.getNano();
            		//ephemeris.startTime = Constants.getDurationInDaysSinceEpoch1900(now);
            		startTextField.setText(Constants.makeStandardDateTimeFormattedString(now));
            	}
            	//JOptionPane.showMessageDialog(null, "Current time to: "+popup.getInvoker());
            }
        });
        popup.add(menuItem);
        return popup;
	}
}
