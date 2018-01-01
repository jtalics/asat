package com.jtalics.asat;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class StatusPanel extends JPanel {

	private Asat asat;

	public StatusPanel(Asat asat) {
		this.asat = asat;
		this.setLayout(new BorderLayout());
		add(new JScrollPane(new JTextArea(), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
	}
}
