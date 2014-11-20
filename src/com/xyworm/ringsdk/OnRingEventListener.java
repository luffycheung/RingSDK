package com.xyworm.ringsdk;

import com.xyworm.ringsdk.utils.RawEvent;

/**
 * ��ָ�ص��ӿڣ���Activity��Service��ʵ�ָĽӿڣ����RingManagerʵ�ֽ�ָ�ļ���
 * @author linwei
 *
 */
public interface OnRingEventListener {

	/**
	 * ��ָ�ƶ�����ص��ӿ�
	 * @param type ��������
	 */
	public void onDirectEvent(int type);
	
	/**
	 * ԭʼ��ָ�¼��ص��ӿڣ����������ʹ������չ
	 * @param event
	 */
	public void onRawEvent(RawEvent event);
	
	/**
	 * ����ص��ӿ�
	 * @param msg ������Ϣ�ַ���
	 */
	public void onError(String msg);
}
