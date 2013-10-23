package fr.nover.yana.passerelles;

import fr.nover.yana.Yana;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean wasScreenOn = true;

    public void onReceive(final Context context, final Intent intent) {
    	if(intent!=Yana.Fermeture){
	        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            // do whatever you need to do here
	            wasScreenOn = false;
	        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
	            // and do whatever you need to do here
	            wasScreenOn = true;
	        }
	        Log.d("Screen","On ? - "+wasScreenOn);
    	}
    }
}