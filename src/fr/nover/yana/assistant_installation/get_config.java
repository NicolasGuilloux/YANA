package fr.nover.yana.assistant_installation;

import fr.nover.yana.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class get_config extends PreferenceActivity {

	String [] testValues = {"Mademoiselle" , "Madame", "Monsieur"};
    public static String Nom, Prénom, Pseudonyme, IPAdress, IPadress_ext, SSID;
    public static boolean TTS, ShakeService, Update_Com, externe, EventService;
    public static int Sexe;
    
    SharedPreferences.Editor geted;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.option);
		
		geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
		
		geted.putBoolean("AI", false);
		geted.putString("name", Nom);
		geted.putString("surname", Prénom);
		geted.putString("nickname", Pseudonyme);
		
		geted.putString("IPadress", IPAdress);
		if(externe){
			geted.putBoolean("externe", true);
			geted.putString("IPadress_ext", IPadress_ext);
			geted.putString("SSID", SSID);}
		else{geted.putBoolean("externe", false);}
		geted.putBoolean("shake", ShakeService);
		geted.putBoolean("event", EventService);
		geted.putBoolean("tts_pref", TTS);
		geted.putBoolean("update", Update_Com);
    	geted.commit();
	
		ListPreference lp = (ListPreference) findPreference("sexe");
		lp.setEntries(testValues);
		lp.setEntryValues(testValues);
		if(Sexe<3){lp.setValue(testValues[Sexe]);}
		
		finish();}
}		