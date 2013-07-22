/**
*Cette application a été développée par Nicolas -Nover- Guilloux.
*Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Configuration extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.option_configuration);
	}

}