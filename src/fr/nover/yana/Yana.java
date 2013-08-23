/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import fr.nover.yana.assistant_installation.Assistant_Installation;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ExpandableListAdapter;

public class Yana extends Activity implements TextToSpeech.OnInitListener{
	
	static EditText IPadress; // Affiche et stocke l'adresse IP
	static TextView tts_pref_false; // Affichage pour prévenir de l'état du TTS
	ImageButton btnRec; // Bouton pour lancer l'initialisation
	ImageView ip_adress; // Affichage et actions du bouton à côté de ip_adress
	String Recrep="", Rep=""; // Déclare les variables correspondant aux divers éléments de la conversation avec le RPi
    String Nom, Prénom, Sexe, Pseudo; // Pour l'identité de l'utilisateur
    boolean bienvenue, changelog=false;
    static boolean bienvenue_fait=false;
    Random random = new Random(); // Pour un message aléatoire
		
    private TextToSpeech mTts;// Déclare le TTS
    
    static boolean testTTS = false, AI, Commande_actu=false;
	    
    	// A propos du Service (Intent pour le lancer et servstate pour savoir l'état du service)
	public static Intent mShakeService,mEventService;
	public static boolean servstate=false, eventstate=false;
	boolean Box_TTS;
	
	String Token=""; 
	public static String version;
	String version_ex;
	
	SharedPreferences preferences;
	
		// Conversation et liste de commandes
	int n=1;
	boolean update;
	Handler myHandler = new Handler();
	
    ExpandableListView expListView;
	
		// S'il reçoit un signal Broadcast du Service, il réagit en conséquence
	private BroadcastReceiver NewRecrep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "envoi");}};
			
	private BroadcastReceiver NewRep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "reponse");}};
	    
	    // Juste une valeur fixe de référence pour le résultat d'Activités lancées
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	protected static final int TTS = 3;
	
	public void onCreate(Bundle savedInstanceState){
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.interface_yana); // Définit la layout a utiliser
	    
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRecrep, // Déclare les liens Broadcast avec le Service
	 			new IntentFilter("NewRecrep"));
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRep,
				new IntentFilter("NewRep"));

    	IPadress = (EditText)findViewById(R.id.IPadress); // Déclare les éléments visibles
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	ip_adress = (ImageView) findViewById(R.id.ip_adress);
    	expListView = (ExpandableListView) findViewById(R.id.ExpLV);
    	
    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); // Empêche un bug de contact avec le RPi (je ne sais pas pourquoi :))
    	StrictMode.setThreadPolicy(policy);

		preferences= PreferenceManager.getDefaultSharedPreferences(this);
		
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
			Log.d("Version","Version de l'application : "+version);} 
		catch (NameNotFoundException e) {e.printStackTrace();}
    	
		IPadress.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT); // Définit l'EditText comme un champ URL
    	
    	getConfig(); // Actualise la configuration
    	if(!Commande_actu){Commandes_actu();}
    	if(update){Commandes_actu();} // Actualise les commandes si la config correspond

    	ip_adress.setOnClickListener(new View.OnClickListener() { // Lance la configuration si on clique sur l'image à côté de l'adresse IP
    		@Override
    		public void onClick(View v){
    			String IP_Adress=IPadress.getText().toString();
    			if(IP_Adress.contains("action.php")){
    				IP_Adress = IP_Adress.replace("action.php", "");
        			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+IP_Adress));
        			startActivity(browserIntent);}
    			else{
    				Toast toast= Toast.makeText(getApplicationContext(),
    			    "Votre adresse n'est pas bonne. :(", 4000);  
    				toast.show();}
    			}});
    	
    	btnRec.setOnClickListener(new View.OnClickListener() {	 // S'effectue lors d'un appui sur le bouton Rec
    		@Override
    		public void onClick(View v){
    			Initialisation();}});}
	
	public void onStart(){
	    super.onStart();
	    
    	if(AI){    		
    		Intent SetupWizard = new Intent(this, Assistant_Installation.class);
    		startActivityForResult(SetupWizard, OPTION);}
	    
    	else{
		   	if(bienvenue && Box_TTS && !bienvenue_fait){
		   		bienvenue_fait=true;
		   		Rep = Random_String();
		   		mTts = new TextToSpeech(this, this);}
		   	
		   	if(changelog){
				String Changelog="Impossible de charger les changelogs.";
				
				try{Resources res = getResources();
			        InputStream in_s = res.openRawResource(R.raw.changelog);
			        InputStreamReader in_r = new InputStreamReader(in_s, "UTF-8");

			        char[] b = new char[in_s.available()];
			        in_r.read(b);
			        
			        Changelog = new String(b);} 
				catch (Exception e) {Log.d("Changelog",""+e);}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(Changelog)
					   .setTitle("Changelogs")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	    changelog=false;
				        	    Traitement.Verif_aux("Changelog_do", getApplicationContext());}
				       });
				AlertDialog alert = builder.create();
				alert.show();
		   	}
    	}
    }
	
	public void onActivityResult(int requestCode, int resultCode, Intent data){ // S'exécute lors d'un retour d'activité
    switch (requestCode) {
		case RESULT_SPEECH: { // Dès que la reconnaissance vocale est terminée
			if (resultCode == RESULT_OK && null != data) {
				
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				Recrep = text.get(0); // Enregistre le résultat dans RecRep
				
				String Ordre="", URL=""; // Déclare Ordre
				int n = Traitement.Comparaison(Recrep); // Compare les String pour trouver un ordre
				if(n<0){Ordre=Recrep;} // Si la comparaison a échoué
				else{ // Sinon, la commande la plus proche de l'ordre est attribuée à Ordre
					Ordre = Traitement.Commandes.get(n); 
					URL = Traitement.Liens.get(n);}
	
				Prétraitement(Ordre, URL); // Envoie en Prétraitement
				break;}}
		
		case OPTION: // Dès un retour de la configuration, il la recharge
			getConfig();
			break;
	}}

	public void onInit(int i){ // S'exécute dès la création du mTts
		try{
			if(mTts.isLanguageAvailable(Locale.FRENCH)!=TextToSpeech.LANG_AVAILABLE && !testTTS){
				new AlertDialog.Builder(this)
		 	    .setTitle("Le TTS n'est pas en Français.")
		 	    .setMessage("Android détecte que votre dispositif de Synthèse Vocale ne dispose pas du Français dans ses langues. Voulez-vous installer le Français ? Appuyez sur Non pour continuer quand même.")
		 	    .setNegativeButton("Non", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	            testTTS=true;}
		 	     })
		 	    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	        	Intent installIntent = new Intent();
			            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			            startActivity(installIntent);}
		 	     })
		 	    .show();}
			else{
				testTTS=true;
				mTts.setLanguage(Locale.FRENCH);
				
				mTts.speak(Rep,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
			    Rep="";} // Au cas où Rep reste le même à la prochaine déclaration du TTS
		}
		catch(Exception e){
			Toast t = Toast.makeText(getApplicationContext(),
    				"Impossible de vérifier les langues de votre TTS. Yana va tout de même essayer de le lancer.",
    				Toast.LENGTH_SHORT);
    	        	t.show();
    	        	mTts.speak(Rep,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
    			    Rep="";}
	}

	public void onDestroy(){ // Quitte le TTS quand l'application se termine
	    if (mTts != null){
	        mTts.stop();
	        mTts.shutdown();}
	    super.onDestroy();}
	
	public void onResume(){
		getConfig();
		super.onResume();}
	
    public boolean onCreateOptionsMenu(Menu menu) { // Il dit juste que y'a telle ou telle chose dans le menu
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);}   
	
	public boolean onOptionsItemSelected(MenuItem item){ // Il dit que si on clique sur tel objet, on effectue telle action
		if(item.getItemId() == R.id.Btnconfiguration){
			startActivityForResult(new Intent(this, Configuration.class), OPTION);}
		if(item.getItemId() == R.id.updateCom){
			Commandes_actu();}
		return super.onOptionsItemSelected(item);}

    void getConfig(){ // Importe les paramètres
    	String ip_adress;
    	if(Traitement.Verif_Reseau(getApplicationContext())) ip_adress=preferences.getString("IPadress", "");// Importe l'adresse du RPi
    	else ip_adress=preferences.getString("IPadress_ext", "");// Importe l'adresse du RPi
    	
    	if(ip_adress != "") IPadress.setText(ip_adress);
    	
    	AI = preferences.getBoolean("AI", true);
    		
    	Box_TTS=preferences.getBoolean("tts_pref", true); // Importe l'état de la box (autorise ou non le TTS)
    	if(Box_TTS==false) tts_pref_false.setText("Attention ! Votre TTS est désactivé.");
    	else tts_pref_false.setText("");
    	
    	bienvenue=preferences.getBoolean("bienvenue", true);
    	
	    version_ex=preferences.getString("version", "");
		if(version.compareTo(version_ex)!=0) changelog=true;
    	
    	update=preferences.getBoolean("update", false);
    	
    	Token=preferences.getString("token", "");
    	
    	Nom=preferences.getString("name", ""); // Importe l'identité de la personne
		Prénom=preferences.getString("surname", "");
		Sexe=preferences.getString("sexe", "");
		Pseudo=preferences.getString("nickname", "");
    	
    	mShakeService=new Intent(Yana.this, ShakeService.class); // Démarre le service en fonction de l'état de la box
    	boolean Box_shake=preferences.getBoolean("shake", false);
    	if((Box_shake==true) && servstate==false){startService(mShakeService);}
    	else if((Box_shake==false) && servstate==true){stopService(mShakeService);}
    	else if((Box_shake==true) && servstate==true){ // Réactualise les variables au cas où on passe d'une reco en continu à une reco par Shake
    		stopService(mShakeService);
    		startService(mShakeService);}
    	
    	mEventService=new Intent(Yana.this, EventService.class); // Démarre le service en fonction de l'état de la box
    	boolean Box_Event=preferences.getBoolean("event", false);
    	if((Box_Event==true) && eventstate==false){startService(mEventService);}
    	else if((Box_Event==false) && eventstate==true){stopService(mEventService);}
    	
    	Traitement.Voice_Sens = Double.parseDouble(preferences.getString("Voice_sens", "3.0"))* Math.pow(10.0,-2.0); // Importe la sensibilité de la comparaison des chaines de caractères
    	if (Traitement.Voice_Sens>=1){
    		Toast t = Toast.makeText(getApplicationContext(),
    				"Attention ! La sensibilité d'analyse de la voix est trop forte. Votre programme choisira la commande la plus proche de votre ordre. Pour mettre une sensibilité, votre valeur dans les options doit être inférieure à 10. ",
    				Toast.LENGTH_SHORT);
    	        	t.show();}
    	
    	float Shake_sens=Float.parseFloat(preferences.getString("shake_sens", "3.0f")); // Importe la sensibilité du Shake
		ShakeDetector.getConfig(Shake_sens);

		Log.d("End of Config","End Of Config");}
        
    void Initialisation(){ // Initialise le processus
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");
			
		try {
			startActivityForResult(intent, RESULT_SPEECH);} // Lance l'acquisition vocale
			
		catch (ActivityNotFoundException a) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Oh bah zut alors ! Ton Android n'a pas installé le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
					Toast.LENGTH_SHORT);
			t.show();}
        }  

    void conversation(String Texte, String Envoi){ // Ici on inscrit la conversation entre l'utilisateur et le RPi
    	
    	final View Conversation_layout =  findViewById(R.id.conversation);
    	
        TextView valueTV = new TextView(this); // Créé le TextView pour afficher le message
        valueTV.setText(Texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this); // Importe la petite flèche de droite ou de gauche
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);
        	
        if(Envoi=="envoi"){
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
        ((ViewGroup) Conversation_layout).addView(fleche);
        
        ((ScrollView) findViewById(R.id.conversation_scroll)).post(new Runnable(){
            public void run(){((ScrollView) findViewById(R.id.conversation_scroll)).fullScroll(View.FOCUS_DOWN);}}); // Pour ancrer en bas à chaque nouvel ordre
    	}
    
	void Commandes_Layout(){ // Ici, on va inscrire les commandes sur le panel
		
		if(Traitement.Categories.size()>0){
			ExpandableListAdapter listAdapter = new ExpandableListAdapter(this, Traitement.Categories, Traitement.listDataChild);
	        expListView.setAdapter(listAdapter);
	        
	        expListView.setOnChildClickListener(new OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long itemID) {
	    	    	int ID=(int)itemID;
	    	    	ArrayList<String> Reco = Traitement.listDataChild.get(Traitement.Categories.get(groupPosition));
	    	    	int i = Traitement.Comparaison(Reco.get(ID));
					Prétraitement(Traitement.Commandes.get(i), Traitement.Liens.get(i));
	    	    	return false;}
			});
	    }
    	
    	ListView Commandes_List =(ListView) findViewById(R.id.commandes_layout);
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, R.drawable.command_list, Traitement.Commandes_a);
		Commandes_List.setAdapter(modeAdapter);
	        
		Commandes_List.setOnItemClickListener(new OnItemClickListener() {
    	    public void onItemClick(AdapterView<?> arg0, View view, int arg2,long itemID) {
    	    	int ID=(int)itemID;
    	    	int i = Traitement.Comparaison(Traitement.Commandes_a.get(ID));
				Prétraitement(Traitement.Commandes.get(i), Traitement.Liens.get(i));}
			});}

    void Commandes_actu(){ // Ici on va actualiser la liste des commandes
    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
 
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		
    	if(Commande_actu && Token.compareTo("")!=0){
    		if(activeNetwork!=null){
		    	if(Traitement.pick_JSON(IPadress.getText().toString(), Token)){ // Commence le protocole de reception et les enregistre dans une ArrayList
		    		Toast toast= Toast.makeText(getApplicationContext(), 
		    				"Update fait !", 4000);  
		    				toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
		    				toast.show();}
		    	
		    	else{
		    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
		    		Traitement.Commandes.get(Traitement.Commandes.size()-1), 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();
					Traitement.Add_Commandes(false);
					Traitement.Commandes_a = new ArrayList<String>(Traitement.Commandes);}}
		    else{
		    	Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
			    	"Vous n'avez pas de connexion internet !", 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();
					Traitement.Add_Commandes(false);
					Traitement.Commandes_a = new ArrayList<String>(Traitement.Commandes);}
    	}
    	
    	else if (Token.compareTo("")==0 && !AI){ 
    		
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Traitement.Add_Commandes(false);
			
    		Traitement.Commandes.add("Vous n'avez pas entré le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("0.7");
    		
    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
    		Traitement.Commandes.get(Traitement.Commandes.size()-1), 4000);  
    		toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    		toast.show();
    		
    		Traitement.Commandes_a = new ArrayList<String>(Traitement.Commandes);}
    	
    	else{
    		Commande_actu=true;
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Traitement.Add_Commandes(false);
			
    		Traitement.Commandes.add("Vous n'avez pas encore actualisé vos commandes.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("0.7");

		 	Traitement.Commandes_a = new ArrayList<String>(Traitement.Commandes);}
    	
    	Commandes_Layout();}
    
    void Prétraitement(final String Ordre, final String URL){ // Ici, on va analyser la réponse si elle est traitable localement. Sinon, on l'envoie au RPi
    	conversation(Ordre, "envoi");
    	
    	myHandler.postDelayed(new Runnable(){

		@Override
		public void run() {
			Prétraitement2(Ordre,URL);
		}}, 250);
    }  
    
    void Prétraitement2 (String Ordre, String URL){ // Deuxième partie du Prétraitement (MyHandler l'oblige pour afficher l'ordre avant le traitement)
    	Rep="";
    	  
    	if(Traitement.Verif_aux(Ordre,this)) Rep = Traitement.Rep;  // Vérification auxiliaire
    	else if(URL.compareTo("")==0) Rep="";
    	else if(Ordre.compareTo(Recrep)==0) Rep="Aucun ordre ne semble être identifié au votre."; // Si Ordre=Recrep alors c'est que la reconnaissance par pertinence a échoué
    	else{
    		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        	
        	if(activeNetwork!=null){ // Vérifie le réseau
        		Log.d("Ordre",""+Ordre);
        			Rep = Traitement.HTTP_Contact("http://"+IPadress.getText().toString()+"?"+URL+"&token="+Token, getApplicationContext());} // Envoie au RPi et enregistre sa réponse
        	else{
        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'échec, il prévient l'utilisateur
    			    	"Vous n'avez pas de connexion internet !", 4000);  
    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    					toast.show();}
        }
		if(Rep.compareTo("")!=0){
			conversation(Rep, "reponse");
			
			if(Box_TTS==true && Rep.length()<300 && !Traitement.Sons){ // Lance la synthèse vocale si les options l'autorisent et si la réponse n'est pas trop longue
				mTts = new TextToSpeech(this, this);}
			else if(!Traitement.Sons) Traitement.Sons=false;
		}}
    
    public String Random_String(){ // Choisit une chaine de caractères au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Bonjour !");
		
		if(!AI){
			if(Prénom.compareTo("")!=0){
				list.add("Salut "+Prénom+" !");}
			
			if(Nom.compareTo("")!=0){
				list.add("Sincères salutations, maître "+Nom+".");}
			
			if(Sexe.compareTo("")!=0){
				list.add("Bonjour "+Sexe+" "+Nom+". Heureux de vous revoir.");}
			
			if(Pseudo.compareTo("")!=0){
				list.add("Coucou mon petit "+Pseudo+". Heureux de te revoir !");}}
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}

}