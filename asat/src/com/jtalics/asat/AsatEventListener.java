package com.jtalics.asat;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public interface AsatEventListener extends EventListener {
	public void eventOccurred(AsatEvent ev);

	static List<AsatEventListener> listeners = new ArrayList<>();

	static void addListener(AsatEventListener listener) {
		listeners.add(listener);
	}

	static void fireEvent(AsatEvent ev) {
		for (AsatEventListener listener : listeners) {
			listener.eventOccurred(ev);
		}
	}
}
