package com.xyworm.ringsdk;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.xyworm.ringsdk.utils.DirectionAnalyst;
import com.xyworm.ringsdk.utils.EventsJNI;
import com.xyworm.ringsdk.utils.RawEvent;
import com.xyworm.ringsdk.utils.EventsJNI.InputDevice;

public class RingManager {
	
	private String ringName;
	private OnRingEventListener l;
	private DirectionAnalyst direAnalyst;
	/** �߳������ж�*/
	private boolean run;
	
	/** �����ָ�������ص���Ϣ*/
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what){
			case RawEvent.ERROR:
				l.onError((String)msg.obj);
				break;
			default:
				RawEvent re = (RawEvent) msg.getData().getSerializable("RawEvent");
				l.onRawEvent(re);
				direAnalyst.onRecvEvent(re);
				// gestureAnalyst.onRecvEvent(re);
				break;
			}
				
		}
	};
	
	public String getRingName() {
		return ringName;
	}
	
	public void setRingName(String ringName) {
		this.ringName = ringName;
	}
	
	public void setRingEventListener(OnRingEventListener l) {
		this.l = l;
		direAnalyst = new DirectionAnalyst(l);
	}
	
	/**
	 * ��ʼ�����豸�������¼�����Ҫ������
	 */
	public void runListener(){
		// δʵ�ֻص��ӿ�
		if(null == l){ 	
			return;
		}
		
		EventsJNI events = new EventsJNI();
		Message errorMsg = new Message();
		errorMsg.what = RawEvent.ERROR;
		int res = events.Init();
		if (res < 1) {    // δ�ҵ��κ��豸
			errorMsg.obj = "Find none device,have you change mode 666 to /dev/input/*";
			mHandler.sendMessage(errorMsg);
			return;
		}
		for (InputDevice idev : events.m_Devs) {
			if (idev.Open(true)) { // force root open it
				if (idev.getName().contains(ringName)) { // find the device
					startMonitor(idev);
					return;
				}

			} else {
				errorMsg.obj = "Device could find failed to open, have you change mode 666 to /dev/input/*";
				mHandler.sendMessage(errorMsg);
				break;
			}
		}
		errorMsg.obj = "Can not find [" + ringName + "], have connect to it?";
		mHandler.sendMessage(errorMsg);
		
	}
	
	/** �رռ�������Ҫ����*/
	public void killListener(){
		run = false;
		System.gc();
	}
	
	/**
	 * ���������߳�
	 * @param idev �������豸
	 */
	private void startMonitor(final InputDevice idev){
		run = true;
		Thread b = new Thread(new Runnable() {
			public void run() {
				while (run) {
					if (idev.getOpen() && (0 == idev.getPollingEvent())) {
						RawEvent raw = new RawEvent(idev);
						Message m = new Message();
						m.what = raw.getType();
						Bundle b = new Bundle();
						b.putSerializable("RawEvent", raw);
						m.setData(b);
						mHandler.sendMessage(m);
					}
				}
			}
		});
		b.start();
	}
	
	
}
	
	
	
