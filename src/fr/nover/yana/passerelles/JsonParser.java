// Sources : http://preprod-orange-tunisie.makina-corpus.net/developper-ensemble/tutoriel/parsing-des-fichiers-json-dans-android

package fr.nover.yana.passerelles;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonParser extends DefaultHttpClient{

	static JSONObject jObj = null;
	static String json = "";
	
	@SuppressWarnings("unused")
	private DefaultHttpClient createHttpClient() {
		 
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

		DefaultHttpClient client = new DefaultHttpClient();
		
		SchemeRegistry registry = new SchemeRegistry();
		SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		
		registry.register(new Scheme("http", socketFactory, 80));
		registry.register(new Scheme("https", socketFactory, 443));
		
		SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);

		// Set verifier     
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		
	    return new DefaultHttpClient(mgr, client.getParams());
	  }
	
	// Constructeur de notre classe
	public JsonParser() {}
	
	public JSONObject getJSONFromUrl(String URL) {
	
	// début de la requête http
		try {
			jObj=null;
		// faire appel à defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();//createHttpClient();
			HttpPost httpPost = new HttpPost(URL);
			HttpResponse httpResponse = httpClient.execute(httpPost);
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