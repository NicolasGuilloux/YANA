package fr.nover.yana;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.nover.yana.model.Category;
import fr.nover.yana.model.Commande;
import fr.nover.yana.model.Parameter;
import fr.nover.yana.model.YanaConfiguration;
import fr.nover.yana.utils.Helper;

/**
 * Created by sylvain on 29/08/15.
 */
public class MemoryProvider {

    private static final String LOG_TAG = MemoryProvider.class.getName();

    private static MemoryProvider ourInstance;

    private  ArrayList<Commande> commandes = new ArrayList<Commande>();

    private ArrayList<Category> categories = new ArrayList<Category>();

    public void setConfiguration(YanaConfiguration configuration) {
        this.configuration = configuration;
    }

    private YanaConfiguration configuration;


    public static MemoryProvider getInstance() {
        if (ourInstance == null) {
            ourInstance = new MemoryProvider();
        }

        return ourInstance;
    }

    public ArrayList<Commande> getCommandes() {
        return commandes;
    }



    public void clear() {
        commandes.clear();
        categories.clear();
    }

    public void initData(boolean full) {
        int i = 0;
        Parameter params = new Parameter("","0.7");
        Commande command;

        command = new Commande("YANA, montre-toi.");
        command.setParameter(params);
        commandes.add(i, command);
        i++;

        command = new Commande("YANA, cache-toi.");
        command.setParameter(params);
        commandes.add(i, command);
        i++;

        command = new Commande("YANA, active les évènements.");
        command.setParameter(params);
        commandes.add(i, command);
        i++;

        command = new Commande("YANA, désactive les évènements.");
        command.setParameter(params);
        commandes.add(i, command);
        i++;

        if (full) {
            Category category =new Category("XBMC","XBMC");
            categories.add(category);

            category =new Category("Sons","vocalinfo_sound");
            categories.add(category);

            category =new Category("Relais radio","radioRelay_change_state");
            categories.add(category);

            category =new Category("Default","default");
            categories.add(category);

        }
    }

    public ArrayList<Category> getCategories() {
        return categories;
    }


    public Commande findCommande(String recrep) {
        for(Commande commande :commandes ){
            if(commande.getName().equals(recrep)){
                return commande;
            }
        }
        return null;
    }

    public YanaConfiguration getConfiguration() {
        return configuration;
    }
}
