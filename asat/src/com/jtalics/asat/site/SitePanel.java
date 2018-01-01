package com.jtalics.asat.site;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import com.jtalics.asat.AsatEvent;
import com.jtalics.asat.AsatEventListener;
import com.jtalics.n3mo.Site;

public class SitePanel extends JPanel implements AsatEventListener {

	public final List<Site> sites = new ArrayList<>();

	public SitePanel() {
		super();

		try {
			sites.add(new Site());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setLayout(new BorderLayout());
		SiteTable siteTable = new SiteTable(sites);
		add(new JScrollPane(siteTable), BorderLayout.CENTER);
		siteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		siteTable.getSelectionModel().addListSelectionListener(new SiteTableListSelectionListener(sites));
		JTableHeader header = siteTable.getTableHeader();
		header.setDefaultRenderer(new SiteTableCellRenderer());
		SiteTableModel siteTableModel = new SiteTableModel(sites);
		siteTable.setModel(siteTableModel);
		AsatEventListener.addListener(this);
	}

	@Override
	public void eventOccurred(AsatEvent ev) {
		// ignore

	}

}
