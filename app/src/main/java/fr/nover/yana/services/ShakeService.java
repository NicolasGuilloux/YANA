/**
 * Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
 * Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
 * Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
 * Vous pouvez me contacter � cette adresse : Etsu@live.fr
 */

package fr.nover.yana.services;

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

import fr.nover.yana.MemoryProvider;
import fr.nover.yana.gui.Yana;
import fr.nover.yana.model.Commande;
import fr.nover.yana.model.Parameter;
import fr.nover.yana.model.YanaConfiguration;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerFinishedCallback;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerState;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper;

public class ShakeService extends Service implements TextToSpeech.OnInitListener, OnUtteranceCompletedListener, RecognizerFinishedCallback {

    private static final String LOG_TAG = ShakeService.class.getName();

    private ShakeDetector mShakeDetector; // Pour la d�tection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    SpeechRecognizerWrapper mSpeechRecognizerWrapper;

    private TextToSpeech mTts; // D�clare le TTS


    YanaConfiguration configuration;


    String a_envoyer, a_dire = ""; // D�clare les deux variables de conversation et l'adresse IP

    Boolean last_init = false, last_shake = false; // D�clare les variables pour �viter les doublons d'initialisation et Shake

    Intent STT;
    Handler myHandler = new Handler();
    Context context;
    // D�clare le SpeechRecognizer
    private static SpeechRecognizer speech = null;

    // D�clare les contacts avec l'activit� Yana
    Intent newRecrep = new Intent("NewRecrep");
    Intent newRep = new Intent("NewRep");
    Intent post_speech = new Intent("Post_speech");

    private Traitement traitement = new Traitement();

    // Valeur de retour de la Comparaison
    int n = -1;

    final BroadcastReceiver mReceiver = new ScreenReceiver();

    public void onCreate() {
        super.onCreate();

        Yana.servstate = true; // D�finit l'�tat du service pour Yana


        getConfig();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // D�finit tous les attributs du Shake
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override
            public void onShake(int count) { // En cas de Shake, si l'�cran est allum� et qu'il n'y a pas d�j� eu Shake
                if (last_shake == false && ScreenReceiver.wasScreenOn) {
                    last_shake = true;
                    getConfig();
                    a_dire = configuration.randomAsk();
                    getTTS();
                }
            }
        });

        mSpeechRecognizerWrapper = new SpeechRecognizerWrapper(getApplicationContext());
        mSpeechRecognizerWrapper.addRecognizerFinishedCallback(this);

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // Pour le contact avec l'interface de Yana
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        context = this;
        mSpeechRecognizerWrapper.Broad(this);

        fin();

        Log.d(LOG_TAG, "Speech_continu : " + configuration.isSpeechContinu());
    }

    public void onDestroy() { // En cas d'arr�t du service
        super.onDestroy();
        Yana.servstate = false; // D�finit l'�tat du service (�teint)
        mSpeechRecognizerWrapper.Stop();
        unregisterReceiver(mReceiver);
        mShakeDetector.setOnShakeListener(new OnShakeListener() { // Arr�te le Shake
            @Override
            public void onShake(int count) {
                //Disable
            }
        });
        if (mTts != null) { // Arr�te de TTS
            mTts.stop();
            mTts.shutdown();
        }
        fin();
    }

    public void onInit(int status) { // En cas d'initialisation (apr�s avoir initialis� le TTS)

        Toast t = Toast.makeText(getApplicationContext(), a_dire, Toast.LENGTH_SHORT); // Affiche la phrase dites par votre t�l�phone
        t.show();

        if (configuration.isTts() && Yana.servstate) { // Si on autorise le TTS
            mTts.setOnUtteranceCompletedListener(this);
            HashMap<String, String> myHashAlarm = new HashMap<String, String>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Enonciation termin�e");

            //AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            //int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
            //am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);

            mTts.speak(a_dire, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
        } // Il dicte sa phrase
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {

        if (!Yana.servstate) {
            fin();
            mSpeechRecognizerWrapper.Stop();
            last_init = true;
        } else if (configuration.isSpeechContinu()) { // Si il n'a pas d�j� initialis� le processus de reconnaissance vocale
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(post_speech);
                }
            }, 250);
        } else {
            fin();
        } // Sinon il remet tout � 0
    }

    public void getTTS() {
        mTts = new TextToSpeech(this, this);
    } // Initialise le TTS

    public IBinder onBind(Intent arg0) {
        return null;
    }


    public void getConfig() { // Importe les options
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        configuration = new YanaConfiguration(preferences, getApplicationContext());
    }

    public void onEvent(int arg0, Bundle arg1) {
    }

    public void fin() { // Finalise le processus
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }

        last_shake = last_init = false;
        if (n == 0) { // Si "Yana, cache-toi"
            this.stopSelf();
        } else if (configuration.isSpeechContinu()) {
            last_shake = true;
            last_init = true;
        }
    }

    public void onRecognizerFinished(String Resultat) {
        Log.d(LOG_TAG, "Ordre : " + Resultat);

        if (!configuration.isSpeechContinu()) {
            mSpeechRecognizerWrapper.Stop();
            last_init = true;
        }

        if (speech != null) {
            speech.destroy();
            speech = null;
        }

        a_dire = "";

        Commande commande = null;
        String  url = "";
        n = traitement.comparaison(Resultat); // Compare les deux chaines de caract�res
        Log.d(LOG_TAG, "Num�ro sortant : " + n);
        if (n < 0) { // Si �chec, Ordre=Resultat
            commande = MemoryProvider.getInstance().findCommande(Resultat);
            a_dire = "Aucun ordre ne semble �tre identifi� au votre.";
        } else { // Sinon...
            commande = MemoryProvider.getInstance().getCommandes().get(n);
        } // Ordre prend la valeur de la commande choisie

        newRecrep.putExtra("contenu", commande.getName()); // Envoie l'ordre � l'interface
        LocalBroadcastManager.getInstance(this).sendBroadcast(newRecrep);

        if (n > 0) { // Si l'ordre est valable
            if (traitement.verif_aux(commande.getName(), context)) a_dire = traitement.rep;
            else {

                Parameter params = commande.getParameter();

                if (params.getType() != null) {
                    String Reponse = "";
                    String type = params.getType();
                    if (type.compareTo("talk") == 0) {
                        Reponse = params.getContenu();
                        if (!traitement.sons) getTTS();
                    } else if (type.compareTo("sound") == 0) {
                        String Son = params.getContenu();
                        Reponse = "*" + Son + "*";
                        try {
                            int ID = getResources().getIdentifier(Son, "raw", "fr.nover.yana");

                            MediaPlayer mp = MediaPlayer.create(this, ID);
                            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                            mp.start();
                        } catch (Exception e) {
                        }
                    }

                    if (Reponse.compareTo("") == 0) {
                        newRep.putExtra("contenu", Reponse); // Envoie de la r�ponse � l'interface
                        LocalBroadcastManager.getInstance(this).sendBroadcast(newRep);
                    }
                }


                url = commande.getParameter().getUrl();

                ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                if (activeNetwork != null) { // V�rifie le r�seau
                    a_dire = traitement.HTTP_Contact("http://" + configuration.getIp() + "?" + url + "&token=" + configuration.getToken(), getApplicationContext());
                } // Envoie au RPi et enregistre sa r�ponse
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
                            "Vous n'avez pas de connexion internet !", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
                    toast.show();
                }
            }
        }

        if (a_dire.compareTo("") != 0) {
            newRep.putExtra("contenu", a_dire); // Envoie de la r�ponse � l'interface
            LocalBroadcastManager.getInstance(this).sendBroadcast(newRep);
        } else {
            a_dire = " ";
        }
        if (!traitement.sons) getTTS();
        else traitement.sons = false;
    }

    @Override
    public void onRecognizerStateChanged(RecognizerState state) {
        Log.d(LOG_TAG, state.toString());

        if (state == RecognizerState.Error) {
            Toast.makeText(this, state.toString(), Toast.LENGTH_SHORT).show();
            fin();
        }
    }

    public void onRmsChanged(float rmsdB) {
        ;
    }

}