/**
 * Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
 * Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
 * Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
 * Vous pouvez me contacter � cette adresse : Etsu@live.fr
 */

package fr.nover.yana.passerelles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.MemoryProvider;
import fr.nover.yana.model.Category;
import fr.nover.yana.model.Commande;
import fr.nover.yana.model.Parameter;
import fr.nover.yana.model.YanaConfiguration;
import fr.nover.yana.services.EventService;
import fr.nover.yana.gui.Yana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class Traitement {

    private static final String LOG_TAG = Traitement.class.getName();

    private String reponse = ""; // D�clare la variable de r�ponse

    public String rep;
    public boolean sons;
    public boolean retour; // D�clare le retour pour la passerelles JsonParser

    public List<Commande> commandes_a = new ArrayList<Commande>();



    public int comparaison(String enregistrement) { // Processus de comparaison pour faire une reconnaissance par pertinence
        YanaConfiguration configuration = MemoryProvider.getInstance().getConfiguration();
        int n = -1;
        double c = 0, co;
        try {
            for (int i = 0; i < MemoryProvider.getInstance().getCommandes().size(); i++) {
                String confidence = MemoryProvider.getInstance().getCommandes().get(i).getParameter().getConfidence();
                if (confidence.equals("")) confidence = "0.8";
                co = LevenshteinDistance.similarity(enregistrement, MemoryProvider.getInstance().getCommandes().get(i).getName());
                if (co > c && co > Double.parseDouble(confidence) - configuration.getVoiceSens()) {
                    c = co;
                    n = i;
                }
            }
        } catch (Exception e) {
            Log.e("log_tag", "Erreur pour la comparaison : " + e.toString());
        }
        Log.d(LOG_TAG, "c=" +  configuration.getVoiceSens());
        Log.d(LOG_TAG, "n=" + n);
        if (c <  configuration.getVoiceSens()) {
            n = -1;
        } // Compare en fonction de la sensibilit� (cf option)
        return n;
    } // Retourne le r�sultat

    public String HTTP_Contact(String URL, Context context) {
        Log.d("Echange avec le serveur", "" + URL);
        retour = true;
        JSONObject json= null;
        try {
            json = new JsonParser(this).execute(URL).get();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Le server n'a pas pu �tre contact�");
        }

        if (!retour) return "Il y a eu une erreur lors du contact avec Yana-Serveur.";

        try {
            JSONArray commands = json.getJSONArray("responses");
            JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
            String type = Rep.getString("type");

            if (type.compareTo("talk") == 0) reponse = Rep.getString("sentence");

            else if (Rep.getString("type").compareTo("sound") == 0) {
                String Son = Rep.getString("file");
                Son = Son.replace(".wav", "");
                reponse = "*" + Son + "*";
                media_Player(Son, context);
            }

            for (int i = 1; i < commands.length(); i++) { // Importe les autres valeurs s'il y en a
                JSONObject emp = commands.getJSONObject(i);
                if ("talk".compareTo(emp.getString("type")) == 0) {
                    reponse = reponse + " \n" + emp.getString("sentence");
                } else if (Rep.getString("type").compareTo("sound") == 0) {
                    String Son = emp.getString("file");
                    Son = Son.replace(".wav", "");
                    reponse = reponse + " \n" + "*" + Son + "*";
                    media_Player(Son, context);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            try {
                JSONArray commands = json.getJSONArray("error");
                JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
                reponse = Rep.getString("error");
            } catch (JSONException x) {
                e.printStackTrace();
            } catch (Exception x) {
                reponse = "Il y a eu une erreur lors du contact avec le Raspberry Pi.";
            }
        }

        return reponse;
    }

    public boolean pick_JSON(String iPadress, String token) {
        retour = true;

        MemoryProvider.getInstance().clear();
        JSONObject json = null;

        commandes_a.clear();
        String speechUrl ="http://" + iPadress + "?action=GET_SPEECH_COMMAND&token=" + token;
        try {
            json = new JsonParser(this).execute(speechUrl).get();
        } catch (Exception e) {
        }

        ArrayList<String> links = new ArrayList<String>();
        JsonConverter jsonConverter = new JsonConverter();

        if (retour) {
            Log.d(LOG_TAG, "Début du traitement du JSON");
            try {
                JSONArray commands = json.getJSONArray("commands");
                for (int i = 0; i < commands.length(); i++) {
                    JSONObject emp = commands.getJSONObject(i);

                    String command = emp.getString("command");
                    command = command.replace("&#039;", "'");
                    command = command.replace("eteint", "�teint");

                    Parameter params = jsonConverter.convertParameters(emp);

                    if (!params.getUrl().contains("vocalinfo_devmod")) {
                        MemoryProvider.getInstance().getCommandes().add(new Commande(command,params));
                        links.add(params.getUrl());
                    }
                }
            } catch (JSONException e) {
                verification_erreur();
                retour = false;
            } catch (Exception e) {
                verification_erreur();
                retour = false;
            }

            MemoryProvider.getInstance().initData(true);

            commandes_a = new ArrayList<Commande>(MemoryProvider.getInstance().getCommandes());

            int x = 0;
            while (commandes_a.size() != links.size()) {
                links.add(x, "");
                x++;
            }

            for (int y = MemoryProvider.getInstance().getCategories().size() - 1; y >= 0; y--) {
                ArrayList<Commande> reco = new ArrayList<Commande>();
                Category category = MemoryProvider.getInstance().getCategories().get(y);
                for (int i = commandes_a.size() - 1; i >= 0; i--) {
                    if (links.get(i).toLowerCase().contains(category.getIdentifiant().toLowerCase()) || commandes_a.get(i).getName().toLowerCase().contains(category.getIdentifiant().toLowerCase())) {
                        reco.add(commandes_a.get(i));
                        commandes_a.remove(i);
                        links.remove(i);
                    }
                }
                Log.d(LOG_TAG, "Reco : " + reco);
                if (reco.size() > 0) {
                    Collections.reverse(reco);
                    MemoryProvider.getInstance().getCategories().get(y).setData(reco);
                } else {
                    Log.d(LOG_TAG, "Remove : " + MemoryProvider.getInstance().getCategories().get(y));
                    MemoryProvider.getInstance().getCategories().remove(y);
                }
            }
        } else {
            Parameter params = new Parameter("","");
            String command = "Echec du contact avec le serveur. Veuillez v�rifier votre syst�me et l'adresse entr�e. Il est possible aussi que votre connexion est trop lente.";
            MemoryProvider.getInstance().getCommandes().add(new Commande(command, params));

            MemoryProvider.getInstance().initData(false);
        }

        return retour;
    }

    private boolean verification_erreur() {
        Log.d(LOG_TAG, "Regarde s'il n'y a pas une erreur disponible.");
        JSONObject json = null;
        Parameter params = new Parameter("","");
        String command = null;


        try {
            if (json.getString("error").compareTo("insufficient permissions") == 0 || json == null)
                command = "Vous n'avez pas les droits n�c�ssaires pour effectuer cette action. Contactez votre administrateur.";
            if (json.getString("error").compareTo("invalid or missing token") == 0 || json == null)
                command = "Votre Token est invalide. Veuillez le v�rifier.";
        } catch (JSONException x) {
        } catch (Exception x) {
        }

        Log.d(LOG_TAG, "Echec de toute compr�hension.");
        if (command == null)
            command = "Echec de compr�hension par rapport � la r�ponse du Raspberry Pi. Veuillez pr�senter le probl�me � Nover.";

        MemoryProvider.getInstance().getCommandes().add(new Commande(command, params));
        return false;
    }


    public boolean verif_aux(String commande, Context context) {
        SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(context).edit();

        if (commande.contains("cache-toi")) {
            geted.putBoolean("shake", false);
            geted.commit();
            if (Yana.servstate == true) {
                context.stopService(Yana.mShakeService);
                rep = "Le ShakeService est maintenant d�sactiv�.";
            } else {
                rep = "Votre service est d�j� d�sactiv�.";
            }
            return true;
        } else if (commande.contains("montre-toi")) {
            geted.putBoolean("shake", true);
            geted.commit();
            if (Yana.servstate == false) {
                context.startService(Yana.mShakeService);
                rep = "Le ShakeService est maintenant activ�.";
            } else {
                rep = "Votre service est d�j� activ�.";
            }
            return true;
        } else if (commande.contains("d�sactive les �v�nements")) {
            geted.putBoolean("event", false);
            geted.commit();
            if (Yana.eventstate == true) {
                context.stopService(Yana.mEventService);
                rep = "Les �v�nements sont maintenant d�sactiv�s.";
            } else {
                rep = "Les �v�nements sont d�j� d�sactiv�s.";
            }
            return true;
        } else if (commande.contains("active les �v�nements")) {
            geted.putBoolean("event", true);
            geted.commit();
            if (Yana.eventstate == false) {
                context.startService(Yana.mEventService);
                EventService.first = false;
                rep = "Les �v�nements sont maintenant activ�s.";
            } else {
                rep = "Les �v�nements sont d�j� activ�s.";
            }
            return true;
        } else if (commande.contains("Changelog_do")) {
            geted.putString("version", Yana.version);
            geted.commit();
            return true;
        }

        return false;
    }



    public void media_Player(String son, Context context) {
        sons = true;
        int ID = context.getResources().getIdentifier(son, "raw", "fr.nover.yana");

        MediaPlayer mp = MediaPlayer.create(context, ID);
        mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mp.start();
    }

}