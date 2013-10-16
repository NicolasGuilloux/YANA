package fr.nover.yana.assistant_installation;

import fr.nover.yana.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class get_config extends PreferenceActivity {

	String [] Values = {"Mademoiselle" , "Madame", "Monsieur"};
    public static String nom, prenom, pseudonyme, IPAdress, IPadress_ext, SSID;
    public static boolean TTS, shakeService, update_Com, externe, eventService;
    public static int sexe;
    
    SharedPreferences.Editor geted;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.option);

		geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
		geted.putBoolean("AI", false);
		geted.putString("name", nom);
		geted.putString("surname", prenom);
		geted.putString("nickname", pseudonyme);
		
		geted.putString("IPadress", IPAdress);
		if(externe){
			geted.putBoolean("externe", true);
			geted.putString("IPadress_ext", IPadress_ext);
			geted.putString("SSID", SSID);}
		else{geted.putBoolean("externe", false);}
		geted.putBoolean("shake", shakeService);
		geted.putBoolean("event", eventService);
		geted.putBoolean("tts_pref", TTS);
		geted.putBoolean("update", update_Com);
    	geted.commit();
	
		ListPreference lp = (ListPreference) findPreference("sexe");
		lp.setEntries(Values);
		lp.setEntryValues(Values);
		if(sexe<3) lp.setValue(Values[sexe]);
		
		finish();}
}		