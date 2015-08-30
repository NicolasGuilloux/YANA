package fr.nover.yana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by sylvain on 29/08/15.
 */
public class Helper {

    private static final String LOG_TAG = Helper.class.getName();

    public static boolean verif_Reseau(Context context){ // Vérifie le réseau local
        try{WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            String SSID_wifi=wifiInfo.getSSID();
            if(SSID_wifi!=null && SSID_wifi.contains("\"")){
                SSID_wifi = SSID_wifi.replaceAll("\"", "");}
            Log.d(LOG_TAG, "SSID actuelle : " + SSID_wifi);

            SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
            String SSID_local=preferences.getString("SSID", "");
            Log.d(LOG_TAG,"SSID enregistr� : "+SSID_local);
            if(wifi.isConnected()){
                if(SSID_wifi.compareTo(SSID_local)==0 || SSID_local.compareTo("")==0){
                    Log.d(LOG_TAG,"local true");
                    return true;}}}
        catch(Exception e){
            Toast toast= Toast.makeText(context,
                    "Echec de la v�rification du r�seau. Mise en local par d�faut", Toast.LENGTH_SHORT);
            toast.show();}
        return false;
    }
}
