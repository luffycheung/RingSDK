package com.xyworm.ringsdk.utils;

import java.io.Serializable;

import com.xyworm.ringsdk.utils.EventsJNI.InputDevice;


/**
 * @author linwei
 * 
 *         从设备结点获取的原始数据类型
 */

public class RawEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int ERROR = 0xff;

	// type
	/** 相对事件 */
	public static final int EV_REL = 0x02;
	/** 按键事件 */
	public static final int EV_KEY = 0x01; // key event

	// code
	public static final int REL_X = 0x00;
	public static final int REL_Y = 0x01;
	public static final int BTN_LEFT = 0x110;
	public static final int BTN_RIGHT = 0x111;

	// value
	public static final int DOWN = 0x01;
	public static final int UP = 0x00;

	private int type;
	private int code;
	private int value;
	private double timestamp;

	public RawEvent() {
	};

	public RawEvent(InputDevice idev) {
		this.type = idev.getSuccessfulPollingType();
		this.code = idev.getSuccessfulPollingCode();
		this.value = idev.getSuccessfulPollingValue();
		this.timestamp = idev.getSuccessfulPollingTime();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "RawEvent [type=" + type + ", code=" + code + ", value=" + value
				+ ", timestamp=" + timestamp + "]";
	}

}
