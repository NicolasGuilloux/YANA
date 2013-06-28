/**
Cette application a été développée par Nicolas -Nover- Guilloux.
Elle a été créée afin d'interagir avec YURI, lui-même créé par Idleman.
Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.nover.yana.passerelles.Contact_RPi;
import fr.nover.yana.passerelles.ShakeDetector;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class Yana extends Activity implements TextToSpeech.OnInitListener {
	
	static EditText IPadress; // Affiche et stocke l'adresse IP
	static TextView tts_pref_false; // Affichage pour prévenir de l'état du TTS
	ImageButton btnRec; // Bouton pour lancer l'initialisation
	ImageView ip_adress; // Affichage et actions du bouton à côté de ip_adress
	String Recrep="", Rep="";
		
		// Déclare le TTS
    private TextToSpeech mTts;
	    
    	// A propos du Service (Intent pour le lancer et servstate pour savoir l'état du service)
	private Intent ShakeService;
	static boolean servstate=false;
	boolean Box_TTS;
	    
	    // Juste une valeur fixe de référence pour le résultat d'Activités lancées
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	protected static final int TTS = 3;
	
		// Conversation
	public int n=1;
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState){
	    super.onCreate(savedInstanceState);
	    //requestWindowFeature(Window.FEATURE_NO_TITLE);
	    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    setContentView(R.layout.interface_yana);
	    
    	IPadress = (EditText)findViewById(R.id.IPadress);
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	ip_adress = (ImageView) findViewById(R.id.ip_adress);
    	
    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	StrictMode.setThreadPolicy(policy);
    	getConfig();
    	
    	ip_adress.setOnClickListener(new View.OnClickListener() {	
    		@Override
    		public void onClick(View v){
    			startActivityForResult(new Intent(Yana.this, Configuration.class), OPTION);}});
    	
    	btnRec.setOnClickListener(new View.OnClickListener() {	
    		@Override
    		public void onClick(View v){
    			Initialisation();}});}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) // Installe un TTS s'il n'y en a pas, ou créer le mTts
    {
    switch (requestCode) {
	case RESULT_SPEECH: {
		if (resultCode == RESULT_OK && null != data) {

			ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			Recrep= text.get(0);
			conversation(Recrep, true);
			Rep="";
			
			android.os.SystemClock.sleep(1000);
			Rep=Contact_RPi.HTTP_Send(Recrep,false);
			conversation(Rep,false);
			
			while(Recrep==""){android.os.SystemClock.sleep(1000);}
			
			if (Box_TTS==true){
			    	getTTS();}
		    }
			break;}
	
			case OPTION: {getConfig();}
			
			case TTS:{
	            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
	                    // success, create the TTS instance
	                mTts = new TextToSpeech(this, this);}
	        }
	}}

	public void onInit(int i){
    	// S'exécute dès la création du mTts
	    mTts.speak(Rep,TextToSpeech.QUEUE_FLUSH,null);}

	public void onDestroy(){
	    	// Quitte le TTS
	    if (mTts != null){
	        mTts.stop();
	        mTts.shutdown();}
	    super.onDestroy();}
	
    public boolean onCreateOptionsMenu(Menu menu) {
			// Il dit juste que y'a telle ou telle chose dans le menu
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);}   
	
	public boolean onOptionsItemSelected(MenuItem item){
		// Il dit que si on clique sur tel objet, on effectue telle action
		if(item.getItemId() == R.id.configuration){
			startActivityForResult(new Intent(this, Configuration.class), OPTION);}
		return super.onOptionsItemSelected(item);}

    public void getConfig(){
    	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	String V_string=preferences.getString("IPadress", "");
    	if(V_string != ""){
    		IPadress.setText(V_string);}
    	Contact_RPi.IP_adress=V_string;
    		
    	Box_TTS=preferences.getBoolean("tts_pref", true);
    	if(Box_TTS==false){
    		tts_pref_false.setText("Attention ! Votre TTS est désactivé.");}
    	else{
    		tts_pref_false.setText("");}
    		
    	ShakeService=new Intent(Yana.this, ShakeService.class);
    	boolean Box_shake=preferences.getBoolean("shake", true);
    	if((Box_shake==true) && servstate==false){
    		startService(ShakeService);}
    		
    	if((Box_shake==false) && servstate==true){
    		stopService(ShakeService);}
    	
    	float Shake_sens=Float.parseFloat(preferences.getString("shake_sens", "3.0f"));
		if(Shake_sens<=1)   { 		
			Toast t = Toast.makeText(getApplicationContext(),
			"Attention ! Votre sensibilité de Shake est trop basse donc elle a été réhaussée à 3.",
			Toast.LENGTH_SHORT);
        	t.show();}
		ShakeDetector.getConfig(Shake_sens);
         }
    	
    public void getTTS(){
    	Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA); // Va vérifier si il y a un TTS de disponible
        startActivityForResult(checkIntent, TTS);}
        
    public void Initialisation(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");
			
		try {
			startActivityForResult(intent, RESULT_SPEECH);}
			
		catch (ActivityNotFoundException a) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Oh bah zut alors ! Ton Android n'a pas installé le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
					Toast.LENGTH_SHORT);
			t.show();}
        }  
    
    public static void ServiceState(boolean etat){
		servstate=etat;}

    public void conversation(String Texte, boolean Envoi){
    	View Conversation_layout =  findViewById(R.id.conversation);
    	
        TextView valueTV = new TextView(this);
        valueTV.setText(Texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this);
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);
        	
        if(Envoi){
        	fleche.setImageResource(R.drawable.envoi);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);
	        	
        	params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.envoi));}
        
        else{
        	fleche.setImageResource(R.drawable.reponse);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);

        	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.recu));}
        
        n=n+1;

        valueTV.setPadding(10, 10, 10, 10);
        params.setMargins(20, 0, 20, 20);
        params_fleche.setMargins(0, 0, 0, 20);
        
        valueTV.setLayoutParams(params);
        fleche.setLayoutParams(params_fleche);
        ((ViewGroup) Conversation_layout).addView(valueTV);
        ((ViewGroup) Conversation_layout).addView(fleche);}

}