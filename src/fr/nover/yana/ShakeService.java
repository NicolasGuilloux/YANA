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
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import fr.nover.yana.passerelles.Contact_RPi;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;

import android.widget.Toast;

@SuppressLint("NewApi")
public class ShakeService extends Service implements TextToSpeech.OnInitListener, RecognitionListener{

	private ShakeDetector mShakeDetector; // Pour la détection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private TextToSpeech mTts;
    Random random = new Random();
    
	String A_envoyer,Resultat="";
    
    String A_dire="";
    Boolean last_init=false, last_shake=false, Yuri=true;
    
		// Logger tag
 	private static final String TAG="";
 		// Speech recognizer instance
 	private SpeechRecognizer speech = null;
 		// Timer used as timeout for the speech recognition
 	private Timer speechTimeout = null;
     
 		// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
			fin();}
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
        registerReceiver(mReceiver, filter);
        
        
     }

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
			mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH,null);}
		if(last_init==false){
			android.os.SystemClock.sleep(1000);
			startVoiceRecognitionCycle();}
		else{fin();}
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

	public void onBufferReceived(byte[] buffer) {Log.d(TAG,"onBufferReceived");}

	public void onEndOfSpeech() {Log.d(TAG,"onEndOfSpeech");}

	public void onError(int error) {
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
		t.show();
		fin();}

	public void onResults(Bundle results) {
		String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
		Log.d(TAG,"Ordre : " + Resultat);
		A_dire="";
		
		if(Resultat.contains("Yana") || Resultat.contains("Iliana") || Resultat.contains("Diana")){A_dire=Contact_RPi.HTTP_Send(Resultat,true);}
		else{A_dire="Vous n'avez pas dit Yuri !";}
		
		while(A_dire==""){android.os.SystemClock.sleep(1000);}
		
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
		
		return Retour;
	}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){
		android.os.SystemClock.sleep(3000);
		last_shake=last_init=false;
		mTts.stop();
        mTts.shutdown();}

}
