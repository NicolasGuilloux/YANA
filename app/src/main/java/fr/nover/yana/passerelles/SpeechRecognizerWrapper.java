package fr.nover.yana.passerelles;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class SpeechRecognizerWrapper implements RecognitionListener{
    private static boolean             DEBUG = true;
    private static final String        TAG   = "SpeechRecognizerWrapper";

    private Context                    mAppContext;
    private SpeechRecognizer           mSpeechRecognizer;
    private boolean                    mRecognitionReady;
    public static boolean              mIsListening;

    private RecognizerFinishedCallback mCallback;
    private RecognizerState            mRecognitonState;
		// Temps avant arrêt de la reconnaissance
	private Timer 					   speechTimeout = null;
	
 	private BroadcastReceiver post_speech = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			Start();}};

    public enum RecognizerState {
        onReadyForSpeech,
        onEndOfSpeech,
        onBeginingOfSpeech,
        Stop,
        Error,
    }
    
    public void Broad(Context context){
	    LocalBroadcastManager.getInstance(context).registerReceiver(post_speech,
				new IntentFilter("Post_speech"));
    }

    public SpeechRecognizerWrapper(Context mAppContext) {
        super();
        this.mAppContext = mAppContext;
        mRecognitionReady = true;
        mIsListening = false;

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mAppContext
                .getApplicationContext());
        mSpeechRecognizer.setRecognitionListener(this);
    }
    
    public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
        	Looper.prepare();
    		speechTimeout.cancel();
    		Stop();}
	}

    public void Start() {
        mIsListening = true;
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mAppContext.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            if (mRecognitionReady) {
                mSpeechRecognizer.startListening(intent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mAppContext, "ActivityNotFoundException", Toast.LENGTH_SHORT).show();
            mIsListening = false;
        }
    }

    public void Stop() {
        mIsListening = false;
        mRecognitonState = RecognizerState.Stop;
        mSpeechRecognizer.stopListening();
    }

    public void addRecognizerFinishedCallback(RecognizerFinishedCallback callback) {
        mCallback = callback;
    }

    public void onBeginningOfSpeech() {
        if (DEBUG) Log.w(TAG, "onBeginningOfSpeech()");
        mRecognitionReady = false;
    }

    public void onBufferReceived(byte[] buffer) {
        // Log.d(TAG,"onBufferReceived()");
    }

    public void onEndOfSpeech() {
        if (DEBUG) Log.w(TAG, "onEndOfSpeech()");
        mRecognitionReady = true;
    }

    public void onError(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                Log.e(TAG, "ERROR_AUDIO");
                break;

            case SpeechRecognizer.ERROR_CLIENT:
                Log.e(TAG, "ERROR_CLIENT");
                mRecognitionReady = true;
                break;

            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                Log.e(TAG, "ERROR_INSUFFICIENT_PERMISSIONS");
                break;

            case SpeechRecognizer.ERROR_NETWORK:
                Log.e(TAG, "ERROR_NETWORK");
                mRecognitonState = RecognizerState.Error;
                mCallback.onRecognizerStateChanged(mRecognitonState);
                mRecognitionReady = false;
                break;

            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                Log.e(TAG, "ERROR_NETWORK_TIMEOUT");
                mRecognitionReady = true;
                break;

            case SpeechRecognizer.ERROR_NO_MATCH:
                if (DEBUG) Log.d(TAG, "ERROR_NO_MATCH");

                mRecognitionReady = true;
                break;

            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                if (DEBUG) Log.e(TAG, "ERROR_RECOGNIZER_BUSY");

                mRecognitionReady = false;
                break;

            case SpeechRecognizer.ERROR_SERVER:
                Log.e(TAG, "ERROR_SERVER");
                mRecognitionReady = true;
                break;

            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                if (DEBUG) Log.d(TAG, "ERROR_SPEECH_TIMEOUT");

                mRecognitionReady = true;
                break;

            default:
                break;
        }

        if (mIsListening) Start();
    }

    public void onReadyForSpeech(Bundle params) {
        if (DEBUG) Log.w(TAG, "onReadyForSpeech()");
        mRecognitonState = RecognizerState.onReadyForSpeech;

        mRecognitionReady = false;
    }

    public void onResults(Bundle results) {
        if (DEBUG) Log.d(TAG, "onResults()");
        
    	String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
        
    	mRecognitionReady = true;
        mCallback.onRecognizerFinished(Resultat);}

    public void onRmsChanged(float rmsdB) {}

    public RecognizerState getRecognitonState() {
        return mRecognitonState;}

    public interface RecognizerFinishedCallback {
        public void onRecognizerFinished(String string);

        public void onRecognizerStateChanged(RecognizerState state);

        public void onRmsChanged(float rmsdB);
    }

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub
		
	}

}