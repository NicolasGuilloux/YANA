package fr.nover.yana.passerelles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.ShakeService;
import fr.nover.yana.Yana;

import android.util.Log;

public class Contact_RPi {
	
	static String A_dire="";
	public static String IP_adress="";
	// Logger tag
 	private static final String TAG="";
	
	public static String HTTP_Send(String Enregistrement, boolean Service){
		Log.d(TAG,"Echange avec le serveur");
		
			// Début de l'envoie au serveur
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://"+IP_adress);

	    try {
	        	// Prépare les variables et les envoient
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("message", Enregistrement.toString()));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

	        	// Reçoit la réponse
	        HttpResponse responsePOST = httpclient.execute(httppost);
	        HttpEntity httpreponse = responsePOST.getEntity();
	        A_dire = EntityUtils.toString(httpreponse).trim();}
        
        catch(Exception e){
            Log.e("log_tag", "Error converting result "+e.toString());
            A_dire="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}
	    
    	return A_dire;}
	
	public interface HttpEndListener {
        public void onEnd(int count);
    }
	
	public void Etude_Resultat(String Resultat){
		
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(IP_adress+"/hcc/");
	    try {
	    	HttpResponse response = client.execute(httpGet);
	        StatusLine statusLine = response.getStatusLine();
	        int statusCode = statusLine.getStatusCode();
	        if (statusCode == 200) {
	          HttpEntity entity = response.getEntity();
	          InputStream content = entity.getContent();
	          BufferedReader reader = new BufferedReader(new InputStreamReader(content));
	          String line;
	          while ((line = reader.readLine()) != null) {
	            builder.append(line);}
	        String JSON = builder.toString();} 
	        else {
	          Log.e(Contact_RPi.class.toString(), "Failed to download file");}}
	    catch (ClientProtocolException e) {
	        e.printStackTrace();} 
	    catch (IOException e) {
	        e.printStackTrace();}
	    
		JSONObject object = new JSONObject();
		  try {
		    object.put("name", "Jack Hack");
		    object.put("score", new Integer(200));
		    object.put("current", new Double(152.32));
		    object.put("nickname", "Hacker");} 
		  catch (JSONException e) {
		    e.printStackTrace();}
		}
}