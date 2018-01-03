package com.jtalics.asat;

import java.awt.Event;

public class AsatEvent extends Event {

	public static final int EPHEMERIS_FRAME_SELECTED = 101;
	public static final int SITE_SELECTED = 102;
	public static final int SATELLITE_SELECTED = 103;
	public static final int EPHEMERIS_SETTINGS_CHANGE = 104;

	public AsatEvent(Object target, int id, Object arg) {
		super(target, id, arg);
	}
}
