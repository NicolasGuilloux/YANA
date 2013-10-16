/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import fr.nover.yana.model.User;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerFinishedCallback;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerState;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper;

public class ShakeService extends Service implements TextToSpeech.OnInitListener, OnUtteranceCompletedListener, RecognizerFinishedCallback{

	private ShakeDetector mShakeDetector; // Pour la détection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    SpeechRecognizerWrapper mSpeechRecognizerWrapper;
    
    private TextToSpeech mTts; // Déclare le TTS
    boolean TTS_Box; // Permet de savoir si le TTS est autorisé
	boolean speech_continu;
    Random random = new Random(); // Pour un message aléatoire
    User user; // Pour l'identité de l'utilisateur
    
	String a_envoyer, a_dire="", iPadress, token; // Déclare les deux variables de conversation et l'adresse IP
    
    Boolean last_init=false, last_shake=false; // Déclare les variables pour éviter les doublons d'initialisation et Shake
    
    Intent STT;
	Handler myHandler = new Handler();
	Context context;
    
		// Logger tag
 	private static final String TAG="";
 		// Déclare le SpeechRecognizer
 	private static SpeechRecognizer speech = null;
 	
 		// Déclare les contacts avec l'activité Yana
 	Intent newRecrep = new Intent("NewRecrep"); 
 	Intent newRep = new Intent("NewRep");
 	Intent post_speech = new Intent("Post_speech");
 	
 		// Valeur de retour de la Comparaison
 	int n=-1;
 	
 	final BroadcastReceiver mReceiver = new ScreenReceiver();
	
	public void onCreate(){
    	super.onCreate();
    	
    	Yana.servstate=true; // Définit l'état du service pour Yana
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Définit tous les attributs du Shake
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override 
            public void onShake(int count) { // En cas de Shake, si l'écran est allumé et qu'il n'y a pas déjà eu Shake
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		getConfig();
            		a_dire=random_String();
            		getTTS();}
        }});
        
        mSpeechRecognizerWrapper = new SpeechRecognizerWrapper(getApplicationContext());
        mSpeechRecognizerWrapper.addRecognizerFinishedCallback(this);
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // Pour le contact avec l'interface de Yana
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        
        context=this;
        mSpeechRecognizerWrapper.Broad(this);
        
        fin();

        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		speech_continu=preferences.getBoolean("continu", false);
		Log.d("Speech_continu","Speech_continu : "+speech_continu);}

    public void onDestroy() { // En cas d'arrêt du service
        super.onDestroy();
        Yana.servstate=false; // Définit l'état du service (éteint)
        mSpeechRecognizerWrapper.Stop();
        unregisterReceiver(mReceiver);
        mShakeDetector.setOnShakeListener(new OnShakeListener(){ // Arrête le Shake
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){ // Arrête de TTS
        	mTts.stop();
            mTts.shutdown();}
	    fin();}
    
	public void onInit(int status) { // En cas d'initialisation (après avoir initialisé le TTS)
		
		Toast t = Toast.makeText(getApplicationContext(),a_dire,Toast.LENGTH_SHORT); // Affiche la phrase dites par votre téléphone
		t.show();
		
		if(TTS_Box && Yana.servstate){ // Si on autorise le TTS
			mTts.setOnUtteranceCompletedListener(this);
			HashMap<String, String> myHashAlarm = new HashMap<String, String>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Enonciation terminée");
            
            //AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            //int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
            //am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);
            
			mTts.speak(a_dire,TextToSpeech.QUEUE_FLUSH, myHashAlarm);} // Il dicte sa phrase
		}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		
		if(!Yana.servstate){
			fin();
			mSpeechRecognizerWrapper.Stop();
        	last_init=true;}
		
		else if(speech_continu){ // Si il n'a pas déjà initialisé le processus de reconnaissance vocale
			myHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					LocalBroadcastManager.getInstance(context).sendBroadcast(post_speech);
				}}, 250);
			}
		else{fin();} // Sinon il remet tout à 0
	}

	public void getTTS(){mTts = new TextToSpeech(this, this);} // Initialise le TTS

	public IBinder onBind(Intent arg0) {return null;}
    		
	public String random_String(){ // Choisit une chaine de caractères au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc à dire, ma poule ?!");
		
		if(user.prenom!= ""){
			list.add("Oui, "+user.prenom+" ?");}
		
		if(user.nom!= ""){
			list.add("Que voulez-vous, maître "+user.nom+" ?");}
		
		if(user.sexe !=""){
			list.add("Puis-je faire quelque chose pour vous, "+ user.sexe+" ?");}
		
		if(user.nom.compareTo("")!=0 && user.sexe.compareTo("")!=0){
			list.add(user.sexe+" "+user.nom+", je suis tout à vous !");}
		
		if(user.pseudo!=""){
			list.add("Que veux-tu, mon petit "+user.pseudo+" ?");}
		
		int randomInt = random.nextInt(list.size());
        String retour = list.get(randomInt).toString();
		
		return retour;}
	
	public void getConfig(){ // Importe les options
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Traitement.verif_Reseau(getApplicationContext())){
			iPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{iPadress=preferences.getString("IPadress_ext", "");}
		
		token=preferences.getString("token", "");
		TTS_Box=preferences.getBoolean("tts_pref", true);
		speech_continu=preferences.getBoolean("continu", true);
		
		user = new User();
		user.nom=preferences.getString("name", ""); // Importe l'identité de la personne
		user.prenom=preferences.getString("surname", "");
		user.sexe=preferences.getString("sexe", "");
		user.pseudo=preferences.getString("nickname", "");}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){ // Finalise le processus
		if (speech != null) {
			speech.cancel();
			speech.destroy();
			speech = null;}

		last_shake=last_init=false;
		if(n==0){ // Si "Yana, cache-toi"
			this.stopSelf();}
		
		else if(speech_continu){
			last_shake=true;
			last_init=true;}
		}	

    public void onRecognizerFinished(String Resultat) {
        Log.d(TAG,"Ordre : " + Resultat);
        
        if(!speech_continu){
        	mSpeechRecognizerWrapper.Stop();
        	last_init=true;}

		if (speech != null) {
			speech.destroy();
			speech = null;}
        
		a_dire="";
		
		String ordre="", URL="";
		n = Traitement.comparaison(Resultat); // Compare les deux chaines de caractères
		Log.d(TAG,"Numéro sortant : " + n);
		if(n<0){ // Si échec, Ordre=Resultat
			ordre=Resultat;
			a_dire="Aucun ordre ne semble être identifié au votre.";}
		else{ // Sinon...
			ordre = Traitement.commandes.get(n);} // Ordre prend la valeur de la commande choisie
	 	
		newRecrep.putExtra("contenu", ordre); // Envoie l'ordre à l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(newRecrep);
	 	
	 	if(n>0){ // Si l'ordre est valable
	 		if(Traitement.verif_aux(ordre,context)) a_dire=Traitement.rep;
	 		else{
	 			
	 			ArrayList<String> Params = Traitement.parameter.get(ordre);
	 	    	
	 	    	if(Params.size()>2){
	 	        	String Reponse="";
	 	    		String type = Params.get(2);
	 	    		if(type.compareTo("talk")==0){
	 	    			Reponse = Params.get(3);
	 		    		if(!Traitement.sons) getTTS();}
	 	    		else if(type.compareTo("sound")==0){
	 	    			String Son = Params.get(3);
	 	    			Reponse = "*"+Son+"*";
	 	    			try{int ID = getResources().getIdentifier(Son, "raw", "fr.nover.yana");
	 	    			
	 		    			MediaPlayer mp = MediaPlayer.create(this, ID); 
	 		    			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
	 		    			mp.start();}
	 	    			catch(Exception e){}
	 	    		}

	 	        if(Reponse.compareTo("")==0){
	 	        	newRep.putExtra("contenu", Reponse); // Envoie de la réponse à l'interface
	 			 	LocalBroadcastManager.getInstance(this).sendBroadcast(newRep);}
	 	        }
	 	    	
	 			
				URL = Traitement.parameter.get(ordre).get(0);
		 
		    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
	                    .getSystemService(Context.CONNECTIVITY_SERVICE);
	     
	            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	            
		    	if(activeNetwork!=null){ // Vérifie le réseau
		    		a_dire = Traitement.HTTP_Contact("http://"+iPadress+"?"+URL+"&token="+token, getApplicationContext());} // Envoie au RPi et enregistre sa réponse
	        	else{
	        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
	    			    	"Vous n'avez pas de connexion internet !", 4000);  
	    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
	    					toast.show();}}}
		
		if(a_dire.compareTo("")!=0){
			newRep.putExtra("contenu", a_dire); // Envoie de la réponse à l'interface
		 	LocalBroadcastManager.getInstance(this).sendBroadcast(newRep);}
		else{a_dire=" ";}
		if(!Traitement.sons) getTTS();
		else Traitement.sons=false;}
    																													
    @Override
    public void onRecognizerStateChanged(RecognizerState state) {
        Log.d(TAG, state.toString());
        
        if (state == RecognizerState.Error) {
            Toast.makeText(this, state.toString(), Toast.LENGTH_SHORT).show();
            fin();}
    }

    public void onRmsChanged(float rmsdB) {;}

}