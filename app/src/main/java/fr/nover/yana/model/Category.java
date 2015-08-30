package fr.nover.yana.model;

import java.util.ArrayList;

/**
 * Created by sylvain on 29/08/15.
 */
public class Category {

    private String name;
    private String identifiant;



    private ArrayList<Commande> data= new ArrayList<Commande>();

    public Category(String name, String identifiant) {
        this.name = name;
        this.identifiant = identifiant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public ArrayList<Commande> getData() {
        return data;
    }

    public void setData(ArrayList<Commande> data) {
        this.data = data;
    }
}
