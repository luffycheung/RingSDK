package com.xyworm.ringsdk.utils;

import com.xyworm.ringsdk.OnRingEventListener;

public class DirectionAnalyst {
	
	private OnRingEventListener l;

	public DirectionAnalyst(OnRingEventListener l) {
		super();
		this.l = l;
	}
	
	public void onRecvEvent(RawEvent ev){
		// TODO
		// �����ж��߼����ж��ƶ�����,�ص��ӿ�
		if(...){
			l.onDirectEvent(UP);
		}else if(...){
			l.onDirectEvent(DOWN);
		}
	}

}
