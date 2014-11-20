package com.xyworm.ringsdk;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.xyworm.ringsdk.OnRingEventListener;
import com.xyworm.ringsdk.RingManager;
import com.xyworm.ringsdk.utils.RawEvent;

public class DemoActivity extends Activity implements OnRingEventListener{
	
	RingManager manager = new RingManager();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager.setRingName("bluetooth remote");
		manager.setRingEventListener(this);
		manager.runListener();
	}

	@Override
	protected void onPause() {
		super.onPause();
		manager.killListener();
	}

	@Override
	public void onDirectEvent(int arg0) {
		// TODO ·½Ïò¿ØÖÆ
		
	}

	@Override
	public void onError(String arg0) {
		Toast.makeText(this, arg0, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRawEvent(RawEvent arg0) {
		System.out.println(arg0.toString());
	}
}
