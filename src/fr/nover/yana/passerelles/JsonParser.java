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

import android.os.AsyncTask;
import android.util.Log;

public class JsonParser extends AsyncTask<String, Void, JSONObject> {

	static JSONObject jObj = null;
	static String json = "";

	protected JSONObject doInBackground(String... URL) {
		try {
			jObj=null;
			
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);

			DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			
			HttpGet httpGet = new HttpGet(URL[0]);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			json = EntityUtils.toString(httpEntity).trim();
			if(json.compareTo("")==0){ // Met un emplacement vide en esquivant le JSONException
				json="{\"responses\":[{\"type\":\"talk\",\"sentence\":\"\"}]}";}
			
			json = json.replace("&#039;", "'");
			json = json.replace("eteint", "éteint");
			
			Log.d("Résultat sous forme de JSON"," "+json);
		}
		catch (UnsupportedEncodingException e) {
			Log.d("JsonParser","UnsupportedEncoding");
			e.printStackTrace();Traitement.retour=false;} 
		catch (ClientProtocolException e) {
			Log.d("JsonParser","Client");
			e.printStackTrace();Traitement.retour=false;} 
		catch (IOException e) {
			Log.d("JsonParser","IOException");
			e.printStackTrace();Traitement.retour=false;}
		catch (Exception e) {
			Log.d("JsonParser","Exception");
			e.printStackTrace();Traitement.retour=false;}
	
		// convertir le résultat qui est sous format d'un String en un JSONObject
		try {jObj = new JSONObject(json);} 
		catch (JSONException e) {Log.e("JSON Parser", "Error parsing data " + e.toString());Traitement.retour=false;}
		
		// retourner un JSONObject
		return jObj;}
	
	protected void onPostExecute(JSONObject json) {}

} 