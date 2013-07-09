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
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;

@SuppressLint("NewApi")
public class ShakeService extends Service implements TextToSpeech.OnInitListener, RecognitionListener{

	private ShakeDetector mShakeDetector; // Pour la détection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private TextToSpeech mTts;
    Random random = new Random();
    
	String A_envoyer,Resultat="";
    
    String A_dire="";
    Boolean last_init=false, last_shake=false, Yuri=true, TTS_utilise=false;
    
		// Logger tag
 	private static final String TAG="";
 		// Speech recognizer instance
 	private static SpeechRecognizer speech = null;
 		// Timer used as timeout for the speech recognition
 	private Timer speechTimeout = null;
 	
 	Intent NewRecrep = new Intent("NewRecrep");
 	Intent NewRep = new Intent("NewRep");
     
 		// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);}
	}
	
	public void onCreate(){
    	super.onCreate();
    	
    	Yana.ServiceState(true);
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override
            public void onShake(int count) {
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		A_dire=Random_String();
            		getTTS();}
        }});
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        final BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);}

    public void onDestroy() {
        super.onDestroy();
        Yana.ServiceState(false);
        mShakeDetector.setOnShakeListener(new OnShakeListener(){
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){
        	mTts.stop();
            mTts.shutdown();}
    }
    
	public void onInit(int status) {
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		Toast t = Toast.makeText(getApplicationContext(),A_dire,Toast.LENGTH_SHORT);
		t.show();
		
		if(preferences.getBoolean("tts_pref", true)==true){
	            
			HashMap<String, String> myHashAlarm = new HashMap<String, String>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SOME MESSAGE");
			mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH, myHashAlarm);
		if(last_init==false){
			android.os.SystemClock.sleep(1000);
			startVoiceRecognitionCycle();}
		else{fin();}}
		}
	
	public void getTTS(){mTts = new TextToSpeech(this, this);}

	public IBinder onBind(Intent arg0) {return null;}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	private SpeechRecognizer getSpeechRevognizer(){
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
			speech.setRecognitionListener(this);}
		return speech;}

	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	public void startVoiceRecognitionCycle(){
		last_init=true;
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"fr.nover.yana");
		getSpeechRevognizer().startListening(intent);}

	public void stopVoiceRecognition(){
		speechTimeout.cancel();
		fin();
		if (speech != null) {
			speech.destroy();
			speech = null;}
	}

	public void onReadyForSpeech(Bundle params) {
		Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		speechTimeout = new Timer();
		speechTimeout.schedule(new SilenceTimer(), 5000);}

	public void onBeginningOfSpeech() {
		Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		speechTimeout.cancel();}

	public void onBufferReceived(byte[] buffer) {}

	public void onEndOfSpeech() {Log.d(TAG,"onEndOfSpeech");}

	public void onError(int error) {
		String message;
		fin();
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
				message = "Pas de correspondance. Essayez de quitter l'application totalement ou de redémarrer le service.";
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
		
		Log.d(TAG,"onError code:" + error + " message: " + message);
		Toast t = Toast.makeText(getApplicationContext(),
				"Annulé : " + message.toString().toString(),
				Toast.LENGTH_SHORT);
		t.show();}

	public void onResults(Bundle results) {
		String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
		Log.d(TAG,"Ordre : " + Resultat);
		A_dire="";
		
		String Ordre="";
		int n = Traitement.Comparaison(Resultat);
		if(n<0){Ordre=Resultat;}
		else{Ordre = Traitement.Commandes.get(n);}
	 	
		NewRecrep.putExtra("contenu", Ordre);
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRecrep);
	 	
	 	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	String IPAdress=preferences.getString("IPadress", "");
		A_dire=Traitement.HTTP_Send(Ordre,IPAdress);
		
		while(A_dire==""){android.os.SystemClock.sleep(1000);}
		NewRep.putExtra("contenu", A_dire);
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRep);

		getTTS();}

	public void onRmsChanged(float rmsdB) {}

	public void onPartialResults(Bundle arg0) {}
    		
	@SuppressLint("NewApi")
	public String Random_String(){
		ArrayList<String> list = new ArrayList<String>();
		list.add("Que voulez-vous, maître ?");
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc à dire, ma poule ?!");
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){
		last_shake=last_init=false;}
	
}