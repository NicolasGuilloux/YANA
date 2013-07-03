/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana.passerelles;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Traitement {
	
	static String Reponse="";
	// Logger tag
 	private static final String TAG="";
 	public static double Voice_Sens;
 	
 	public static ArrayList<String> Commandes = new ArrayList<String>();
 	public static ArrayList<String> Liens = new ArrayList<String>();
 	static boolean retour;
	
	public static String HTTP_Send(String Enregistrement, String IPadress){
		Log.d(TAG,"Echange avec le serveur");
		
			// Début de l'envoie au serveur
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://"+IPadress);

	    try {
	        	// Prépare les variables et les envoient
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("message", Enregistrement.toString()));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

	        	// Reçoit la réponse
	        HttpResponse responsePOST = httpclient.execute(httppost);
	        HttpEntity httpreponse = responsePOST.getEntity();
	        Reponse = EntityUtils.toString(httpreponse).trim();}
        
        catch(Exception e){
            Log.e("log_tag", "Error converting result "+e.toString());
            Reponse="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}

    	return Reponse;}
	
	public static boolean pick_JSON(String IPadress){
		retour=true;
		JsonParser jParser = new JsonParser ();
		JSONObject json = jParser.getJSONFromUrl(IPadress);
		try{
			JSONArray commands = json.getJSONArray("commands");
			for (int i = 0; i < commands.length(); i++) {
				JSONObject emp = commands.getJSONObject(i);
				Commandes.add(i, emp.getString("cmmand"));
				Liens.add(i, emp.getString("url"));}
		}
		catch(JSONException e){e.printStackTrace();retour=false;}
		return retour;}
	
	public static int Comparaison(String Enregistrement){
		int n=-1;
		double c=0,co;
		for (int i = 0; i < Commandes.size(); i++){
			co=LevenshteinDistance.similarity(Enregistrement, Commandes.get(i));
			if(co>c){
				c=co;
				n=i;}
		}
		if(c<Voice_Sens){n=-1;}
		return n;
	}
	
	public static String HTTP_Contact(String URL){
		Log.d(TAG,"Echange avec le serveur");
		
			// Début de l'envoie au serveur
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(URL);

	    try {
	        	// Reçoit la réponse
	        HttpResponse responsePOST = httpclient.execute(httppost);
	        HttpEntity httpreponse = responsePOST.getEntity();
	        Reponse = EntityUtils.toString(httpreponse).trim();}
        
        catch(Exception e){
            Log.e("log_tag", "Error converting result "+e.toString());
            Reponse="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}

    	return Reponse;}

}