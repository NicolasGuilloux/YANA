/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;

@SuppressLint({ "NewApi", "ShowToast" })
public class ShakeService extends Service implements TextToSpeech.OnInitListener, RecognitionListener{

	private ShakeDetector mShakeDetector; // Pour la détection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private TextToSpeech mTts; // Déclare le TTS
    Random random = new Random(); // Pour un message aléatoire
    
	String A_envoyer, A_dire="", IPadress, Token; // Déclare les deux variables de conversation et l'adresse IP
    
    Boolean last_init=false, last_shake=false; // Déclare les variables pour éviter les doublons d'initialisation et Shake
    
		// Logger tag
 	private static final String TAG="";
 		// Déclare le SpeechRecognizer
 	private static SpeechRecognizer speech = null;
 		// Temps avant arrêt de la reconnaissance
 	private Timer speechTimeout = null;
 	
 		// Déclare les contacts avec l'activité Yana
 	Intent NewRecrep = new Intent("NewRecrep"); 
 	Intent NewRep = new Intent("NewRep");
 	
 		// Valeur de retour de la Comparaison
 	int n=-1;
     
 		// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
        	Looper.prepare();
        	fin();
    		speechTimeout.cancel();
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);}
	}
	
	public void onCreate(){
    	super.onCreate();
    	
    	Yana.ServiceState(true); // Définit l'état du service pour Yana
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Définit tous les attributs du Shake
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override 
            public void onShake(int count) { // En cas de Shake, si l'écran est allumé et qu'il n'y a pas déjà eu Shake
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		A_dire=Random_String();
            		getTTS();}
        }});
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // Pour le contact avec l'interface de Yana
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        final BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        
        fin();}

    public void onDestroy() { // En cas d'arrêt du service
        super.onDestroy();
        Yana.ServiceState(false); // Définit l'état du service (éteint)
        mShakeDetector.setOnShakeListener(new OnShakeListener(){ // Arrête le Shake
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){ // Arrête de TTS
        	mTts.stop();
            mTts.shutdown();}
	    fin();
    }
    
	public void onInit(int status) { // En cas d'initialisation (après avoir initialisé le TTS)
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Traitement.Verif_Reseau(getApplicationContext())){
			IPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{IPadress=preferences.getString("IPadress_ext", "");}
		
		Token=preferences.getString("token", "");
		
		Toast t = Toast.makeText(getApplicationContext(),A_dire,Toast.LENGTH_SHORT); // Affiche la phrase dites par votre téléphone
		t.show();
		
		if(preferences.getBoolean("tts_pref", true)==true){ // Si on autorise le TTS
			mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
		if(last_init==false){ // Si il n'a pas déjà initialisé le processus de reconnaissance vocale
			android.os.SystemClock.sleep(1000);
			startVoiceRecognitionCycle();} // Il l'effectue au bout d'une seconde
		else{fin();}} // Sinon il remet tout à 0
		}
	
	public void getTTS(){mTts = new TextToSpeech(this, this);} // Initialise le TTS

	public IBinder onBind(Intent arg0) {return null;}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	private SpeechRecognizer getSpeechRevognizer(){ // Configure la reconnaissance vocale
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
			speech.setRecognitionListener(this);}
		return speech;}

	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	public void startVoiceRecognitionCycle(){ // Démarre le cycle de reconnaissance vocale
		last_init=true;
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"fr.nover.yana");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
		getSpeechRevognizer().startListening(intent);}

	public void stopVoiceRecognition(){ // Arrête le cicle de reconnaissance vocale
		speechTimeout.cancel();
		fin();}

	public void onReadyForSpeech(Bundle params) { // Dès que la reconnaissance vocale est prête
		Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		speechTimeout = new Timer();
		speechTimeout.schedule(new SilenceTimer(), 5000);
		}

	public void onBeginningOfSpeech() { // Dès qu'on commence à parler
		Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		speechTimeout.cancel();}

	public void onBufferReceived(byte[] buffer) {}

	public void onEndOfSpeech() {Log.d(TAG,"onEndOfSpeech");} // Dès qu'on arrête de parler

	public void onError(int error) { // Si erreur, il affiche celle correspondante
		String message;
		switch (error){
			case SpeechRecognizer.ERROR_AUDIO:
				message = "Erreur d'enregistrement audio";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				message = "Client side error";
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				message = "Erreur de permission";
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				message = "Erreur de réseau";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				message = "Erreur de réseau (trop long)";
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				message = "Il y a eu une erreur en contactant la reconnaissance vocale. Essayez de quitter l'application totalement ou de redémarrer le service.";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				message = "La reconnaissance vocale est déjà utilisée";
				break;
			case SpeechRecognizer.ERROR_SERVER:
				message = "Erreur du serveur";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				message = "Pas d'ordre donné";
				break;
			default:
				message = "Erreur inconnue";
				break;}

		fin();
		Log.d(TAG,"onError code:" + error + " message: " + message);
		Toast t = Toast.makeText(getApplicationContext(),
				"Annulé : " + message.toString().toString(),
				Toast.LENGTH_SHORT);
		t.show();}

	public void onResults(Bundle results) { // Dès la réception du résultat
		String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
		Log.d(TAG,"Ordre : " + Resultat);
		A_dire="";
		if (speech != null) {
			speech.destroy();
			speech = null;}
		
		String Ordre="", URL="";
		n = Traitement.Comparaison(Resultat); // Compare les deux chaines de caractères
		Log.d(TAG,"Numéro sortant : " + n);
		if(n<0){ // Si échec, Ordre=Resultat
			Ordre=Resultat;
			A_dire="Aucun ordre ne semble être identifié au votre.";} 
		else{ // Sinon...
			Ordre = Traitement.Commandes.get(n); // Ordre prend la valeur de la commande choisie
			if(n==0){ // Si le repère est égal à 0
				SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
	    		geted.putBoolean("shake", false); // Il va mettre la box dans les options décochée
	    		geted.commit();
	    		if(Yana.servstate==true){ // Il va arrêter le service s'il est lancé.
	    			A_dire="Le ShakeService est maintenant désactivé.";}
	    		else{A_dire="Votre service est déjà désactivé.";}}}
	 	
		NewRecrep.putExtra("contenu", Ordre); // Envoie l'ordre à l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRecrep);
	 	
	 	if(n>0){ // Si l'ordre est valable
	    	URL = Traitement.Liens.get(n);
	    	
	    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            
	    	if(activeNetwork!=null){ // Vérifie le réseau
	    		A_dire = Traitement.HTTP_Contact("http://"+IPadress+"?"+URL+"&token="+Token);} // Envoie au RPi et enregistre sa réponse
        	else{
        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
    			    	"Vous n'avez pas de connexion internet !", 4000);  
    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    					toast.show();}}
		
		while(A_dire==""){android.os.SystemClock.sleep(1000);} // Attend que A_dire soit bien remplie
		
		NewRep.putExtra("contenu", A_dire); // Envoie de la réponse à l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRep);

		getTTS();} // Dicte la réponse

	public void onRmsChanged(float rmsdB) {}

	public void onPartialResults(Bundle arg0) {}
    		
	@SuppressLint("NewApi")
	public String Random_String(){ // Choisit une chaine de caractères au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Que voulez-vous, maître ?");
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc à dire, ma poule ?!");
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){ // Finalise le processus
		if (speech != null) {
			speech.cancel();
			speech.destroy();
			speech = null;}
		if(n==0){ // Si "Yana, cache-toi"
			android.os.SystemClock.sleep(7000); // Attend bien la fin de l'énonciation avant de quitter l'appli
			last_shake=last_init=false;
			this.stopSelf();}
		else{last_shake=last_init=false;} // Sinon...
		}
	
}