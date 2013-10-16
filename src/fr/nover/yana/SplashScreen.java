package fr.nover.yana;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashScreen extends Activity {

	private static final int STOPSPLASH = 0;
	private static final long SPLASHTIME = 1000;

	private Handler splashHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case STOPSPLASH:
				Intent main = new Intent(SplashScreen.this, Yana.class);
				startActivity(main);
				finish();
				break;
			}
			finish();
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen);
		Message msg = new Message();
		msg.what = STOPSPLASH;
		splashHandler.sendMessageDelayed(msg, SPLASHTIME);
	}
}
