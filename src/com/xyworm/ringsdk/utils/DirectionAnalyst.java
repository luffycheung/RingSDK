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
		// 加入判断逻辑，判断移动方向,回调接口
		if(...){
			l.onDirectEvent(UP);
		}else if(...){
			l.onDirectEvent(DOWN);
		}
	}

}
