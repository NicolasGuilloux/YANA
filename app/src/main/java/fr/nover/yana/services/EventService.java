package fr.nover.yana.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.R;
import fr.nover.yana.gui.Yana;
import fr.nover.yana.passerelles.JsonParser;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.utils.Helper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class EventService extends Service implements OnUtteranceCompletedListener, OnInitListener {

	private static final String LOG_TAG = ShakeService.class.getName();

	private TextToSpeech mTts; // D�clare le TTS
    HashMap<String, String> myHashAlarm;
    
    ArrayList<Boolean> talk = new ArrayList<Boolean>();
    ArrayList<String> contenu = new ArrayList<String>();
    JSONObject json_prec = null;
    
    Handler myHandler = new Handler(),myHandler2 = new Handler();
    Runnable runnable, runnable2;
    Timer t;
	Traitement traitement = new Traitement();
    
    boolean sound,toasts,notifs,toast_ready=true, traitement_ready=true;
    public static boolean first=true;
    int i,j=0;
    String phrase;
    
    Toast toast;
    TextView textView;
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		LayoutInflater layoutInflater = (LayoutInflater)
				   getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = layoutInflater.inflate(R.layout.toast_layout, null);
		
		toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 80);
		textView = (TextView) layout.findViewById(R.id.text);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		
		Yana.eventstate=true;
	
		pick_JSON();}
	
	public void onDestroy(){
		super.onDestroy();
		Yana.eventstate=false;
		myHandler.removeCallbacks(runnable);
		myHandler2.removeCallbacks(runnable2);}
	
	void pick_JSON(){
		
		String IPadress="", token="";
		int time=0;
		boolean entree=false;
		JSONObject json=null;
		
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Helper.verif_Reseau(getApplicationContext())){
			IPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{IPadress=preferences.getString("IPadress_ext", "");}
		token=preferences.getString("token", "");
	try{time=Integer.valueOf(preferences.getString("event_temp", ""))*1000;}
	catch(Exception e){time=20;}
		sound=preferences.getBoolean("event_sound", true);
		toasts=preferences.getBoolean("event_toast", true);
		notifs=preferences.getBoolean("event_notif", true);
		
		talk.clear();
		contenu.clear();
		
		if(!first){
			contenu.add("Service des �v�nements en route.");
			talk.add(true);}
		
		try{json = new JsonParser(traitement).execute("http://"+IPadress+"?action=GET_EVENT&token="+token).get();}
	 	catch(Exception e){
	 		toast("Echec du contact avec le Raspberry Pi.");}
	 	
	 	try{
	 		if((""+json_prec).compareTo(""+json)!=0){
		 		JSONArray commands = json.getJSONArray("responses");
				for(int y = 0; y < commands.length(); y++) {
					JSONObject emp = commands.getJSONObject(y);
					String type=emp.getString("type");
					if(type.compareTo("talk")==0){
						talk.add(true);
						contenu.add(emp.getString("sentence"));
						entree=true;}
					else if(type.compareTo("sound")==0){
						talk.add(false);
						String son = emp.getString("file");
						son = son.replace(".wav", "");
						son = son.replace(".mp3", "");
						contenu.add(son);
						entree=true;}
				}
			}
	 	}
		catch(JSONException e){Log.d(LOG_TAG,"Echec du parsing JSON");}
		catch(Exception e){Log.d(LOG_TAG, "Echec du parsing JSON");}
	 	
	 	json_prec=json;
	 	
	 	i=0;
	 	if(entree || !first) traitement();
	 	
	 	runnable = new Runnable(){
			@Override
			public void run() {
				pick_JSON();
			}};
	 	
		myHandler.postDelayed(runnable, time);}
	
	public void onInit(int status) {
		mTts.setOnUtteranceCompletedListener(this);
		myHashAlarm = new HashMap<String, String>();
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Enonciation termin�e");
		mTts.speak(phrase,TextToSpeech.QUEUE_FLUSH, myHashAlarm);}
	
	@Override
	public void onUtteranceCompleted(String uttid) {traitement();}
	
	boolean launch_son(String son){
		try{int ID = getResources().getIdentifier(son, "raw", "fr.nover.yana");
		
			MediaPlayer mp = MediaPlayer.create(this, ID); 
			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mp.setOnCompletionListener(new  MediaPlayer.OnCompletionListener() { 
	            public  void  onCompletion(MediaPlayer mediaPlayer) { 
	                traitement();
	            } 
	        }); 
			mp.start();
			return true;}
		catch(Exception e){return false;}
	}
	
	void traitement(){
		if(i<talk.size()){
			Log.d("","Phrase : "+contenu.get(i));
			if(talk.get(i)){
				phrase=contenu.get(i);
					if(!first) first=true;
					else if(sound) mTts = new TextToSpeech(this, this);
				}
			else{
				String Son=contenu.get(i);
				if(sound){
					if(launch_son(Son)){phrase="Le son suivant a �t� lanc� : "+Son;}
					else{phrase="Yana a tent� de lancer le son suivant sans succ�s : "+Son;}
				}
				else{phrase="Yana voulait faire un son ("+Son+"), mais vous avez d�sactiv� cette fonctionnalit�.";}
			}
			i++;
			if(toasts) toast(phrase);
			if(notifs) notif(phrase);
	 		if(!sound) traitement();	
		}
	}
	
	@SuppressLint("Wakelock")
	void toast(String affichage){
		PowerManager pm = (PowerManager) getSystemService(EventService.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
		if(!isScreenOn ){final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP, "Toast_Event");
		      wl.acquire();}
		
		if(toast_ready){
			toast_ready=false;
			textView.setText(affichage);
			toast.show();}
		else{
			String Affichage_2 = textView.getText().toString();
			textView.setText(Affichage_2 + " \n" + affichage);
			toast.cancel();
			toast.show();
			//myHandler2.removeCallbacks(Runnable2);
			}

		runnable2 = new Runnable(){
			@Override
			public void run() {
				//Looper.prepare();
		    	toast.cancel();
		        toast_ready=true;
			}};

		if(!toast_ready) myHandler2.postDelayed(runnable2, 4000);}
	
	private final void notif(String Affichage){ 
	       final NotificationManager notificationManager = (NotificationManager)getSystemService(EventService.NOTIFICATION_SERVICE);
	       final String notificationTitle = "�v�nement - Yana"; 
	       final Notification notification = new Notification(R.drawable.ic_launcher, notificationTitle, System.currentTimeMillis());
	       final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, Yana.class), 0);       
	       notification.setLatestEventInfo(this, notificationTitle, Affichage, pendingIntent); 
	       notification.vibrate = new long[] {0,200,100,200,100,200}; 
	 
	       notificationManager.notify(j, notification); 
	       j++;} 
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}