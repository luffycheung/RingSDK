package com.xyworm.ringsdk;

import com.xyworm.ringsdk.utils.RawEvent;

/**
 * 戒指回调接口，在Activity或Service中实现改接口，配合RingManager实现戒指的监听
 * @author linwei
 *
 */
public interface OnRingEventListener {

	/**
	 * 戒指移动方向回调接口
	 * @param type 上下左右
	 */
	public void onDirectEvent(int type);
	
	/**
	 * 原始戒指事件回调接口，后面可用于使用者扩展
	 * @param event
	 */
	public void onRawEvent(RawEvent event);
	
	/**
	 * 错误回调接口
	 * @param msg 错误信息字符串
	 */
	public void onError(String msg);
}
