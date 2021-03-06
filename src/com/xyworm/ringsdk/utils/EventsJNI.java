/*
 * Android Event Injector 
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */

package com.xyworm.ringsdk.utils;

import java.util.ArrayList;


import android.util.Log;

public class EventsJNI {

	private final static String LT = "Events";

	public class InputDevice {

		private int m_nId;
		private String m_szPath, m_szName;
		private boolean m_bOpen;

		InputDevice(int id, String path) {
			m_nId = id;
			m_szPath = path;
		}

		public int InjectEvent() {
			return 0;
		}

		public int getPollingEvent() {
			return PollDev(m_nId);
		}

		public int getSuccessfulPollingType() {
			return getType();
		}

		public int getSuccessfulPollingCode() {
			return getCode();
		}

		public int getSuccessfulPollingValue() {
			return getValue();
		}
		
		public double getSuccessfulPollingTime() {
			return getTime();
		}

		public boolean getOpen() {
			return m_bOpen;
		}

		public int getId() {
			return m_nId;
		}

		public String getPath() {
			return m_szPath;
		}

		public String getName() {
			return m_szName;
		}

		public void Close() {
			m_bOpen = false;
			RemoveDev(m_nId);
		}

		final int EV_KEY = 0x01, EV_REL = 0x02, EV_ABS = 0x03, REL_X = 0x00,
				REL_Y = 0x01, REL_Z = 0x02, BTN_TOUCH = 0x14a;// 330

		public int SendKey(int key, boolean state) {
			if (state)
				return intSendEvent(m_nId, EV_KEY, key, 1); // key down
			else
				return intSendEvent(m_nId, EV_KEY, key, 0); // key up
		}

		

		/**
		 * function Open : opens an input event node
		 * 
		 * @param forceOpen
		 *            will try to set permissions and then reopen if first open
		 *            attempt fails
		 * @return true if input event node has been opened
		 */
		public boolean Open(boolean forceOpen) {
			int res = OpenDev(m_nId);
			// if opening fails, we might not have the correct permissions, try
			// changing 660 to 666
			if (res != 0) {
				// possible only if we have root
				if (forceOpen && Shell.isSuAvailable()) {
					// set new permissions
					Shell.runCommand("chmod 666 " + m_szPath);
					// reopen
					res = OpenDev(m_nId);
				}
			}
			m_szName = getDevName(m_nId);
			m_bOpen = (res == 0);
			// debug
			Log.d(LT, "Open:" + m_szPath + " Name:" + m_szName + " Result:"
					+ m_bOpen);
			// done, return
			return m_bOpen;
		}
	}

	// top level structures
	public ArrayList<InputDevice> m_Devs = new ArrayList<InputDevice>();

	public int Init() {
		m_Devs.clear();
		int n = ScanFiles(); // return number of devs

		for (int i = 0; i < n; i++)
			m_Devs.add(new InputDevice(i, getDevPath(i)));
		return n;
	}

	public void Release() {
		for (InputDevice idev : m_Devs)
			idev.Close();
	}

	// JNI native code interface
	public native static void intEnableDebug(int enable);

	private native static int ScanFiles(); // return number of devs

	private native static int OpenDev(int devid);

	private native static int RemoveDev(int devid);

	private native static String getDevPath(int devid);

	private native static String getDevName(int devid);

	private native static int PollDev(int devid);

	private native static int getType();

	private native static int getCode();

	private native static int getValue();
	private native static double getTime();
	public native static int system(String s);

	// injector:
	public native static int intSendEvent(int devid, int type, int code,
			int value);

	static {
		System.loadLibrary("EventsJNI");
	}

}
