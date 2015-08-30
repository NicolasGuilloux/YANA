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

	private static final String LOG_TAG = JsonParser.class.getName();

	static JSONObject jObj = null;
	static String json = "";
    private Traitement traitement;

    public JsonParser(Traitement traitement) {
        this.traitement = traitement;
    }

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
			
			Log.d(LOG_TAG," "+json);
		}
		catch (UnsupportedEncodingException e) {
			Log.d("JsonParser","UnsupportedEncoding");
			e.printStackTrace();traitement.retour=false;}
		catch (ClientProtocolException e) {
			Log.d("JsonParser","Client");
			e.printStackTrace();traitement.retour=false;}
		catch (IOException e) {
			Log.d("JsonParser","IOException");
			e.printStackTrace();traitement.retour=false;}
		catch (Exception e) {
			Log.d("JsonParser","Exception");
			e.printStackTrace();traitement.retour=false;}
	
		// convertir le r�sultat qui est sous format d'un String en un JSONObject
		try {jObj = new JSONObject(json);} 
		catch (JSONException e) {Log.e("JSON Parser", "Error parsing data " + e.toString());traitement.retour=false;}
		
		// retourner un JSONObject
		return jObj;}
	
	protected void onPostExecute(JSONObject json) {}

} 