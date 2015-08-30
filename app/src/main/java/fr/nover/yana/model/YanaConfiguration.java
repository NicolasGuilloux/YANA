package fr.nover.yana.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Random;

import fr.nover.yana.utils.Helper;

/**
 * Created by sylvain on 29/08/15.
 */
public class YanaConfiguration {
    private Random random = new Random(); // Pour un message aléatoire

    private User user; // Pour l'identité de l'utilisateur


    private String token="";




    private double voiceSens;
    private String ip;

    private boolean ai;



    private float shakeSens;



    private boolean tts; // Permet de savoir si le TTS est autoris�
    private boolean speechContinu;

    public YanaConfiguration(){

    }

    public YanaConfiguration(SharedPreferences preferences,Context context) {
        initFromPreference(preferences,context);
    }

    private void initFromPreference(SharedPreferences preferences,Context context) {
        user = new User();
        user.nom=preferences.getString("name", ""); // Importe l'identité de la personne
        user.prenom=preferences.getString("surname", "");
        user.sexe=preferences.getString("sexe", "");
        user.pseudo = preferences.getString("nickname", "");

        ai = preferences.getBoolean("AI", true);

        token=preferences.getString("token", "");

        if(Helper.verif_Reseau(context)) ip=preferences.getString("IPadress", "");// Importe l'adresse du RPi
        else ip=preferences.getString("IPadress_ext", "");// Importe l'adresse du RPi

        shakeSens=Float.parseFloat(preferences.getString("shake_sens", "3.0f")); // Importe la sensibilit� du Shake

        tts=preferences.getBoolean("tts_pref", true);
        speechContinu=preferences.getBoolean("continu", true);

        voiceSens = Double.parseDouble(preferences.getString("Voice_sens", "3.0")) * Math.pow(10.0, -2.0); // Importe la sensibilit� de la comparaison des chaines de caract�res
    }

    public double getVoiceSens() {
        return voiceSens;
    }

    public User getUser() {
        return user;
    }

    public float getShakeSens() {
        return shakeSens;
    }

    public String getToken() {
        return token;
    }

    public boolean isAi() {
        return ai;
    }

    public String getIp() {
        return ip;
    }

    public boolean isTts() {
        return tts;
    }

    public boolean isSpeechContinu() {
        return speechContinu;
    }

    public String randomWelcome(){ // Choisit une chaine de caract�res au hasard
        ArrayList<String> list = new ArrayList<String>();
        list.add("Bonjour !");

        if(!isAi()){
            if(getUser().prenom != ""){
                list.add("Salut "+getUser().prenom+" !");}

            if(getUser().nom != ""){
                list.add("Sinc�res salutations, ma�tre "+getUser().nom+".");}

            if(getUser().sexe != ""){
                list.add("Bonjour "+getUser().sexe+" "+getUser().nom+". Heureux de vous revoir.");}

            if(getUser().pseudo!= ""){
                list.add("Coucou mon petit "+getUser().pseudo+". Heureux de te revoir !");}}

        int randomInt = random.nextInt(list.size());
        String retour = list.get(randomInt).toString();

        return retour;}

    public String randomAsk(){ // Choisit une chaine de caract�res au hasard
        ArrayList<String> list = new ArrayList<String>();
        list.add("Comment puis-je vous aider, boss ?");
        list.add("Un truc � dire, ma poule ?!");

        if(user.prenom!= ""){
            list.add("Oui, "+user.prenom+" ?");}

        if(user.nom!= ""){
            list.add("Que voulez-vous, ma�tre "+user.nom+" ?");}

        if(user.sexe !=""){
            list.add("Puis-je faire quelque chose pour vous, "+ user.sexe+" ?");}

        if(user.nom.compareTo("")!=0 && user.sexe.compareTo("")!=0){
            list.add(user.sexe+" "+user.nom+", je suis tout � vous !");}

        if(user.pseudo!=""){
            list.add("Que veux-tu, mon petit "+user.pseudo+" ?");}

        int randomInt = random.nextInt(list.size());
        String retour = list.get(randomInt).toString();

        return retour;}

    public String getCommandUrl(Commande commande) {
        return "http://" + ip + "?" + commande.getParameter().getUrl() + "&token=" + token;
    }
}
