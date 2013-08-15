/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana.passerelles;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.EventService;
import fr.nover.yana.ShakeService;
import fr.nover.yana.Yana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class Traitement {
	
	static String Reponse=""; // Déclare la variable de réponse
	
 	private static final String TAG=""; // Logger tag
 	public static double Voice_Sens; // Sensibilité de la comparaison entre ordres et commandes
 	static public String URL="", XBMC, Rep;
 	static public boolean reco_invalide, Sons;
 	
 		// Déclare les ArraList utilisées pour stocker les éléments de commandes
 	public static ArrayList<String> Commandes = new ArrayList<String>();
 	public static ArrayList<String> Liens = new ArrayList<String>();
 	public static ArrayList<String> Confidences = new ArrayList<String>();
 	
 	public static ArrayList<String> Reco_spec = new ArrayList<String>();
 	public static ArrayList<Boolean> Reco_bool = new ArrayList<Boolean>();
 	public static ArrayList<Boolean> Reco_add = new ArrayList<Boolean>(); 	
 	
 	static boolean retour; // Déclare le retour pour la passerelles JsonParser
 	
 	static JSONObject json;
	
	public static int Comparaison(String Enregistrement){ // Processus de comparaison pour faire une reconnaissance par pertinence
		int n=-1;
		double c=0,co;
		reco_invalide=false;
		try{
			Log.d("Reco_spec",""+Reco_spec.get(0));
			Log.d("Reco_bool",""+Reco_bool.get(0));
			for (int i = 0; i < Commandes.size(); i++){
				co=LevenshteinDistance.similarity(Enregistrement, Commandes.get(i));
				if(co>c && co>Double.parseDouble(Confidences.get(i))-Voice_Sens){
					c=co;
					n=i;}
		}}
		catch(Exception e){Log.e("log_tag", "Erreur pour la comparaison : "+e.toString());}
		if(c<Voice_Sens){n=-1;} // Compare en fonction de la sensibilité (cf option)
		for (int y = 0; y < Reco_spec.size(); y++){
			Log.d("",""+Reco_spec.get(y));
			if(n>0){
			if(Liens.get(n).contains(Reco_spec.get(y).toLowerCase())){
				if(!Reco_bool.get(y)){
					Log.d("Non choisi !","Il n'y a pas d'activer le reco "+Reco_spec.get(y));
					reco_invalide=true;}}}}
		return n;} // Retourne le résultat
	
	public static String HTTP_Contact(String URL, Context context){
		Log.d("Echange avec le serveur",""+URL);
		boolean Action_done=false;
		
		for (int y = 0; y < Reco_spec.size(); y++){
			if(URL.contains(Reco_spec.get(y))){
				Action_done=true;
				if(URL.contains("disable")){
					Reco_bool.set(y, false);
					Reponse="La reconnaissance "+Reco_spec.get(y)+" est maintenant désactivée.";}
				else{
					Reco_bool.set(y, true);
					Reponse="La reconnaissance "+Reco_spec.get(y)+" est maintenant activée.";}
			}}
		
	    if(!Action_done){
		    JsonParser jParser = new JsonParser ();
		    try{json = jParser.getJSONFromUrl(URL);}
		    catch(Exception e){Log.d("Echec du contact","Le server n'a pas pu être contacté");}
		    	
		    try{JSONArray commands = json.getJSONArray("responses");
				JSONObject Rep = commands.getJSONObject(0); // Importe la première valeur
				String type = Rep.getString("type");
				
				if(type.compareTo("talk")==0){
					Reponse=Rep.getString("sentence");}
				
				else if (Rep.getString("type").compareTo("sound")==0){
					String Son = Rep.getString("file");
					Son = Son.replace(".wav","");
					Reponse="*"+Son+"*";
					Media_Player(Son, context);}
				
				for (int i = 1; i < commands.length(); i++) { // Importe les autres valeurs s'il y en a
					JSONObject emp = commands.getJSONObject(i);
					if("talk".compareTo(emp.getString("type"))==0){
						Reponse=Reponse+" \n"+emp.getString("sentence");}
					else if (Rep.getString("type").compareTo("sound")==0){
						String Son = emp.getString("file");
						Son = Son.replace(".wav","");
						Reponse=Reponse+" \n"+"*"+Son+"*";
						Media_Player(Son, context);}	
				}
			}
			catch(JSONException e){e.printStackTrace();}
			catch(Exception e){
				try{
					JSONArray commands = json.getJSONArray("error");
					JSONObject Rep = commands.getJSONObject(0); // Importe la première valeur
					Reponse=Rep.getString("error");}
				catch(JSONException x){e.printStackTrace();}
				catch(Exception x){
					Reponse="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}
				}}
	    	
    	return Reponse;}
	
	public static boolean pick_JSON(String IPadress, String Token){
		retour=true;

		Commandes.clear();
		Liens.clear();
		Confidences.clear();
		
		Reco_spec.clear();
		Reco_bool.clear();
		Reco_add.clear();
		
		Reco_spec.add("XBMC");
		Reco_bool.add(false);
		Reco_add.add(false);

	 	JsonParser jParser = new JsonParser ();
	 	try{json = jParser.getJSONFromUrl("http://"+IPadress+"?action=GET_SPEECH_COMMAND&token="+Token);}
	 	catch(Exception e){}
	 	
	 	if(retour){
	 		Log.d("","Début du traitement du JSON");
		 	try{JSONArray commands = json.getJSONArray("commands");
				for(int i = 0; i < commands.length(); i++) {
					JSONObject emp = commands.getJSONObject(i);
					String Command=emp.getString("command");
					Command = Command.replace("&#039;", "'");
					Command = Command.replace("eteint", "éteint");
					String URL = emp.getString("url");
					URL = URL.replace("{", "%7B");
        			URL = URL.replace("}", "%7D");
					StringTokenizer tokens = new StringTokenizer(URL, "?");
					tokens.nextToken();
					URL = tokens.nextToken();
					
					for (int y = 0; y < Reco_spec.size(); y++){
						if(URL.contains(Reco_spec.get(y).toLowerCase()) && !Reco_add.get(y)){						
							Commandes.add("YANA, active la reco "+Reco_spec.get(y));
							Liens.add(Reco_spec.get(y)+"_able");
							Confidences.add("0.7");
							
							Commandes.add("YANA, désactive la reco "+Reco_spec.get(y));
							Liens.add(Reco_spec.get(y)+"_disable");
							Confidences.add("0.7");
							
							Reco_add.set(y, true);}}
					
						Commandes.add(Command);
						Liens.add(URL);
						Confidences.add(emp.getString("confidence"));}
			}
		
		 	catch(JSONException e){
				Verification_erreur();
				retour=false;}
		 	
			catch(Exception e){
				Verification_erreur();
				retour=false;}}
	 	else{
	 		Liens.add("");
			Confidences.add("");
	 		Commandes.add("Echec du contact avec le serveur. Veuillez vérifier votre système et l'adresse entrée.");}
		
	 	Add_Commandes();
		return retour;}
	
	static void Verification_erreur(){
		Log.d(TAG, "Regarde s'il n'y a pas une erreur disponible.");
		
		Liens.add("");
		Confidences.add("");
				
		try{
			if(json.getString("error").compareTo("insufficient permissions")==0 || json==null){
				Commandes.add("Il y a une erreur d'identification ou vous n'avez pas les permissions nécéssaires. Vérifiez votre Token.");}
		}
		catch(JSONException x){
			Log.d(TAG, "Echec de toute compréhension.");
			Commandes.add("Echec de compréhension par rapport à la réponse du Raspberry Pi. Veuillez présenter le problème à Nover.");}

		catch(Exception x){
			Log.d(TAG, "Echec de toute compréhension.");
			Commandes.add("Echec de compréhension par rapport à la réponse du Raspberry Pi. Veuillez présenter le problème à Nover.");}
	}

	public static boolean Verif_Reseau(Context context){ // Vérifie le réseau local
		try{WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		    ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    String SSID_wifi=wifiInfo.getSSID();
		    if(SSID_wifi!=null && SSID_wifi.contains("\"")){
			   SSID_wifi = SSID_wifi.replaceAll("\"", "");}
		    Log.d("SSID","SSID actuelle : "+SSID_wifi);
		   
		    SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
		    String SSID_local=preferences.getString("SSID", "");
		    Log.d("SSID demandée","SSID enregistré : "+SSID_local);
		    if(wifi.isConnected()){
		 	   if(SSID_wifi.compareTo(SSID_local)==0 || SSID_local.compareTo("")==0){
				   Log.d("Local","true");
				   return true;}}}
		catch(Exception e){
			Toast toast= Toast.makeText(context,
    			"Echec de la vérification du réseau. Mise en local par défaut", Toast.LENGTH_SHORT);  
    			toast.show();}
		return false;
	}
	
	public static boolean Verif_aux(String Commande, Context context){
		SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		if(Commande.contains("cache-toi")){
			geted.putBoolean("shake", false);
			geted.commit();
			if(Yana.servstate==true){
				ShakeService.Finish();
				Rep="Le ShakeService est maintenant désactivé.";}
			else{Rep="Votre service est déjà désactivé.";}
			return true;}
		
		else if(Commande.contains("montre-toi")){
			geted.putBoolean("shake", true);
			geted.commit();
			if(Yana.servstate==false){
				context.startService(Yana.mShakeService);
				Rep="Le ShakeService est maintenant activé.";}
			else{Rep="Votre service est déjà activé.";}
			return true;}
		
		else if(Commande.contains("désactive les événements")){
			geted.putBoolean("event", false);
			geted.commit();
			if(Yana.eventstate==true){
				EventService.Finish();
				Rep="Les événements sont maintenant désactivés.";}
			else{Rep="Les événements sont déjà désactivés.";}
			return true;}
		
		else if(Commande.contains("active les événements")){
			geted.putBoolean("event", true);
			geted.commit();
			if(Yana.eventstate==false){
				context.startService(Yana.mEventService);
				Rep="Les événements sont maintenant activés.";}
			else{Rep="Les événements sont déjà activés.";}
			return true;}
		
		
		return false;
	}
	
	public static void Add_Commandes(){
		int i=0;
		
		Commandes.add(i, "YANA, montre-toi.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, cache-toi.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, active les événements.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, désactive les événements.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Reco_spec.add("     ");
		Reco_bool.add(false);
		Reco_add.add(false);
	}
	
	public static void Media_Player(String Son, Context context){
		Sons = true;
		int ID = context.getResources().getIdentifier(Son, "raw", "fr.nover.yana");
		
		MediaPlayer mp = MediaPlayer.create(context, ID); 
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mp.start();}
	
}