package fr.nover.yana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.passerelles.JsonParser;
import fr.nover.yana.passerelles.Traitement;
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
	
	private TextToSpeech mTts; // Déclare le TTS
    HashMap<String, String> myHashAlarm;
    
    ArrayList<Boolean> Talk = new ArrayList<Boolean>();
    ArrayList<String> Contenu = new ArrayList<String>();
    JSONObject json_prec = null;
    
    Handler myHandler = new Handler(),myHandler2 = new Handler();
    Runnable Runnable, Runnable2;
    Timer t;
    
    boolean Sound,Toasts,Notifs,first=false,Toast_ready=true, Traitement_ready=true;
    int i,j=0;
    String Phrase;
    
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
		myHandler.removeCallbacks(Runnable);
		myHandler2.removeCallbacks(Runnable2);}
	
	void pick_JSON(){
		
		String IPadress="", Token="";
		int Time=0;
		boolean entree=false;
		JSONObject json=null;
		
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Traitement.Verif_Reseau(getApplicationContext())){
			IPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{IPadress=preferences.getString("IPadress_ext", "");}
		Token=preferences.getString("token", "");
	try{Time=Integer.valueOf(preferences.getString("event_temp", ""))*1000;}
	catch(Exception e){Time=20;}
		Sound=preferences.getBoolean("event_sound", true);
		Toasts=preferences.getBoolean("event_toast", true);
		Notifs=preferences.getBoolean("event_notif", true);
		
		Talk.clear();
		Contenu.clear();
		
		if(!first){
			Contenu.add("Service des événements en route.");
			Talk.add(true);}
		
		try{json = new JsonParser().execute("http://"+IPadress+"?action=GET_EVENT&token="+Token).get();}
	 	catch(Exception e){
	 		Toast("Echec du contact avec le Raspberry Pi.");}
	 	
	 	try{
	 		if((""+json_prec).compareTo(""+json)!=0){
		 		JSONArray commands = json.getJSONArray("responses");
				for(int y = 0; y < commands.length(); y++) {
					JSONObject emp = commands.getJSONObject(y);
					String type=emp.getString("type");
					if(type.compareTo("talk")==0){
						Talk.add(true);
						Contenu.add(emp.getString("sentence"));
						entree=true;}
					else if(type.compareTo("sound")==0){
						Talk.add(false);
						String Son = emp.getString("file");
						Son = Son.replace(".wav", "");
						Son = Son.replace(".mp3", "");
						Contenu.add(Son);
						entree=true;}
				}
			}
	 	}
		catch(JSONException e){Log.d("Echec du JSON (Get_Event)","Echec du parsing JSON");}
		catch(Exception e){Log.d("Echec du JSON (Get_Event)","Echec du parsing JSON");}
	 	
	 	json_prec=json;
	 	
	 	i=0;
	 	if(entree || !first) Traitement();
	 	
	 	Runnable = new Runnable(){
			@Override
			public void run() {
				pick_JSON();
			}};
	 	
		myHandler.postDelayed(Runnable, Time);}
	
	public void onInit(int status) {
		mTts.setOnUtteranceCompletedListener(this);
		myHashAlarm = new HashMap<String, String>();
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Enonciation terminée");
		mTts.speak(Phrase,TextToSpeech.QUEUE_FLUSH, myHashAlarm);}
	
	@Override
	public void onUtteranceCompleted(String uttid) {Traitement();}
	
	boolean Launch_son(String Son){
		try{int ID = getResources().getIdentifier(Son, "raw", "fr.nover.yana");
		
			MediaPlayer mp = MediaPlayer.create(this, ID); 
			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mp.setOnCompletionListener(new  MediaPlayer.OnCompletionListener() { 
	            public  void  onCompletion(MediaPlayer mediaPlayer) { 
	                Traitement();
	            } 
	        }); 
			mp.start();
			return true;}
		catch(Exception e){return false;}
	}
	
	void Traitement(){
		if(i<Talk.size()){
			Log.d("","Phrase : "+Contenu.get(i));
			if(Talk.get(i)){
				Phrase=Contenu.get(i);
					if(!first) first=true;
					else if(Sound) mTts = new TextToSpeech(this, this);
				}
			else{
				String Son=Contenu.get(i);
				if(Sound){
					if(Launch_son(Son)){Phrase="Le son suivant a été lancé : "+Son;}
					else{Phrase="Yana a tenté de lancer le son suivant sans succès : "+Son;}
				}
				else{Phrase="Yana voulait faire un son ("+Son+"), mais vous avez désactivé cette fonctionnalité.";}
			}
			i++;
			if(Toasts) Toast(Phrase);
			if(Notifs) Notif(Phrase);
	 		if(!Sound) Traitement();	
		}
	}
	
	@SuppressLint("Wakelock")
	void Toast(String Affichage){
		PowerManager pm = (PowerManager) getSystemService(EventService.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
		if(!isScreenOn ){final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP, "Toast_Event");
		      wl.acquire();}
		
		if(Toast_ready){
			Toast_ready=false;
			textView.setText(Affichage);
			toast.show();}
		else{
			String Affichage_2 = textView.getText().toString();
			textView.setText(Affichage_2 + " \n" + Affichage);
			toast.cancel();
			toast.show();
			//myHandler2.removeCallbacks(Runnable2);
			}

		Runnable2 = new Runnable(){
			@Override
			public void run() {
				//Looper.prepare();
		    	toast.cancel();
		        Toast_ready=true;
			}};

		if(!Toast_ready) myHandler2.postDelayed(Runnable2, 4000);}
	
	private final void Notif(String Affichage){ 
	       final NotificationManager notificationManager = (NotificationManager)getSystemService(EventService.NOTIFICATION_SERVICE);
	       final String notificationTitle = "Événement - Yana"; 
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