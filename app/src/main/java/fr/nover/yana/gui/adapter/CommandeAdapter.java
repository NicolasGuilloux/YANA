package fr.nover.yana.gui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.nover.yana.R;
import fr.nover.yana.gui.Yana;
import fr.nover.yana.model.Commande;

/**
 * Created by sylvain on 29/08/15.
 */
public class CommandeAdapter extends ArrayAdapter<Commande> {

    private List<Commande> commandes;
    private Context context;

    public CommandeAdapter(Context context, List<Commande> commandes) {
        super(context, R.layout.command_list,commandes);
        this.commandes = commandes;
        this.context = context;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate( R.layout.command_list, parent, false);
        Commande commande = null;
        if (commandes != null) {
            commande = commandes.get(position);
            TextView textV = (TextView) rowView.findViewById(android.R.id.text1);
            textV.setText(commande.getName());

        }
        return rowView;
    }


}
