// Sources : http://preprod-orange-tunisie.makina-corpus.net/developper-ensemble/tutoriel/parsing-des-fichiers-json-dans-android

package fr.nover.yana.passerelles;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonParser extends DefaultHttpClient {

	static JSONObject jObj = null;
	static String json = "";
	
	// Constructeur de notre classe
	public JsonParser() {}
	
	public JSONObject getJSONFromUrl(String URL) {
	
	// début de la requête http
		try {
			jObj=null;
			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
			HttpConnectionParams.setSoTimeout(httpParameters, 3000);

			DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			
			HttpGet httpGet = new HttpGet(URL);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			json = EntityUtils.toString(httpEntity).trim();;
			Log.d("Résultat sous forme de JSON",json);} 
		
		catch (UnsupportedEncodingException e) {
			Log.d("","UnsupportedEncoding");
			e.printStackTrace();Traitement.retour=false;} 
		catch (ClientProtocolException e) {
			Log.d("","Client");
			e.printStackTrace();Traitement.retour=false;} 
		catch (IOException e) {
			Log.d("","IOException");
			e.printStackTrace();Traitement.retour=false;}
		catch (Exception e) {
			Log.d("","IOException");
			e.printStackTrace();Traitement.retour=false;}
	
		// convertir le résultat qui est sous format d'un String en un JSONObject
		try {jObj = new JSONObject(json);} 
		catch (JSONException e) {Log.e("JSON Parser", "Error parsing data " + e.toString());Traitement.retour=false;}
		
		// retourner un JSONObject
		return jObj;}

} 