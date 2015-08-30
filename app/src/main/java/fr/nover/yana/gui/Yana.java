/**
 * Cette application a été développée par Nicolas -Nover- Guilloux.
 * Elle a été créée afin d'interagir avec YANA, lui-même créé par Idleman.
 * Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
 * Vous pouvez me contacter à cette adresse : Etsu@live.fr
 */

package fr.nover.yana.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import fr.nover.yana.MemoryProvider;
import fr.nover.yana.R;
import fr.nover.yana.gui.adapter.CommandeAdapter;
import fr.nover.yana.gui.installation.Assistant_Installation;
import fr.nover.yana.model.Commande;
import fr.nover.yana.model.Parameter;
import fr.nover.yana.model.YanaConfiguration;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.gui.adapter.ExpandableListAdapter;
import fr.nover.yana.services.EventService;
import fr.nover.yana.services.ShakeService;

public class Yana extends Activity implements TextToSpeech.OnInitListener {

    private static final String LOG_TAG = Yana.class.getName();

    @InjectView(R.id.IPadress)
    EditText iPadress; // Affiche et stocke l'adresse IP
    @InjectView(R.id.tts_pref_false)
    TextView tts_pref_false; // Affichage pour prévenir de l'état du TTS
    @InjectView(R.id.btnRec)
    ImageButton btnRec; // Bouton pour lancer l'initialisation
    @InjectView(R.id.ip_adress)
    ImageView ip_adress; // Affichage et actions du bouton à côté de ip_adress
    @InjectView(R.id.ExpLV)
    ExpandableListView expListView;
    @InjectView(R.id.conversation)
    RelativeLayout conversationView;
    @InjectView(R.id.conversation_scroll)
    ScrollView conversation_scroll;
    @InjectView(R.id.commandes_layout)
    ListView commandesListView;



    String recrep = "", rep = ""; // Déclare les variables correspondant aux divers élèments de la conversation avec le RPi

    YanaConfiguration configuration;

    boolean bienvenue, changelog = false;
    static boolean bienvenue_fait = false;

    private TextToSpeech mTts;// Déclare le TTS

    static boolean testTTS = false, commande_actu = false;

    // A propos du Service (Intent pour le lancer et servstate pour savoir l'état du service)
    public static Intent mShakeService, mEventService;
    public static boolean servstate = false, eventstate = false;
    boolean box_TTS;


    public static String version;
    String version_ex;

    SharedPreferences preferences;
    Traitement traitement = new Traitement();

    // Conversation et liste de commandes
    int n = 1;
    boolean update;
    Handler myHandler = new Handler();


    // S'il reçoit un signal Broadcast du Service, il réagit en conséquence
    private BroadcastReceiver newRecrep = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String contenu = intent.getStringExtra("contenu");
            conversation(contenu, "envoi");
        }
    };

    private BroadcastReceiver newRep = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String contenu = intent.getStringExtra("contenu");
            conversation(contenu, "reponse");
        }
    };

    // Juste une valeur fixe de référence pour le résultat d'Activités lancées
    protected static final int RESULT_SPEECH = 1;
    protected static final int OPTION = 2;
    protected static final int TTS = 3;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interface_yana); // Définit la layout a utiliser
        ButterKnife.inject(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(newRecrep, // Déclare les liens Broadcast avec le Service
                new IntentFilter("NewRecrep"));
        LocalBroadcastManager.getInstance(this).registerReceiver(newRep,
                new IntentFilter("NewRep"));

        iPadress.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT); // Définit l'EditText comme un champ URL


        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        getVersion();


        getConfig(); // Actualise la configuration
        if (!commande_actu) {
            commandes_actu();
        }
        if (update) {
            commandes_actu();
        } // Actualise les commandes si la config correspond

        ip_adress.setOnClickListener(new View.OnClickListener() { // Lance la configuration si on clique sur l'image � c�t� de l'adresse IP
            @Override
            public void onClick(View v) {
                String IP_Adress = iPadress.getText().toString();
                if (IP_Adress.contains("action.php")) {
                    IP_Adress = IP_Adress.replace("action.php", "");
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + IP_Adress));
                    startActivity(browserIntent);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Votre adresse n'est pas bonne. :(", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        btnRec.setOnClickListener(new View.OnClickListener() {     // S'effectue lors d'un appui sur le bouton Rec
            @Override
            public void onClick(View v) {
                initialisation();
            }
        });
    }

    private void getVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            Log.d(LOG_TAG, "Version de l'application : " + version);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onStart() {
        super.onStart();

        if (configuration.isAi()) {
            Intent SetupWizard = new Intent(this, Assistant_Installation.class);
            startActivityForResult(SetupWizard, OPTION);
        } else {
            if (bienvenue && box_TTS && !bienvenue_fait) {
                bienvenue_fait = true;
                rep = configuration.randomWelcome();
                mTts = new TextToSpeech(this, this);
            }

            initchangeLog();
        }
    }

    private void initchangeLog() {
        if (changelog) {
            String Changelog = "Impossible de charger les changelogs.";

            try {
                Resources res = getResources();
                InputStream in_s = res.openRawResource(R.raw.changelog);
                InputStreamReader in_r = new InputStreamReader(in_s, "UTF-8");

                char[] b = new char[in_s.available()];
                in_r.read(b);

                Changelog = new String(b);
            } catch (Exception e) {
                Log.d(LOG_TAG, "" + e);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(Changelog)
                    .setTitle("Changelogs")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            changelog = false;
                            traitement.verif_aux("Changelog_do", getApplicationContext());
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) { // S'exécute lors d'un retour d'activité
        switch (requestCode) {
            case RESULT_SPEECH: { // Dès que la reconnaissance vocale est terminée
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    recrep = text.get(0); // Enregistre le résultat dans RecRep

                    Commande commande = null;
                    int n = traitement.comparaison(recrep); // Compare les String pour trouver un ordre
                    if (n < 0) {
                        commande = new Commande(recrep);
                    } // Si la comparaison a échoué
                    else { // Sinon, la commande la plus proche de l'ordre est attribuée à Ordre
                        commande = MemoryProvider.getInstance().getCommandes().get(n);
                    }

                    pretraitement(commande, n); // Envoie en Prétraitement
                    break;
                }
            }

            case OPTION: // Dès un retour de la configuration, il la recharge
                getConfig();
                break;
        }
    }

    public void onInit(int i) { // S'exécute dès la création du mTts
        try {
            if (mTts.isLanguageAvailable(Locale.FRENCH) != TextToSpeech.LANG_AVAILABLE && !testTTS) {
                new AlertDialog.Builder(this)
                        .setTitle("Le TTS n'est pas en Français.")
                        .setMessage("Android détecte que votre dispositif de Synthèse Vocale ne dispose pas du Français dans ses langues. Voulez-vous installer le Fran�ais ? Appuyez sur Non pour continuer quand m�me.")
                        .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                testTTS = true;
                            }
                        })
                        .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent installIntent = new Intent();
                                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                startActivity(installIntent);
                            }
                        })
                        .show();
            } else {
                testTTS = true;
                mTts.setLanguage(Locale.FRENCH);

                if (rep.compareTo("") != 0)
                    mTts.speak(rep, TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
                rep = "";
            } // Au cas où Rep reste le même à la prochaine déclaration du TTS
        } catch (Exception e) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Impossible de vérifier les langues de votre TTS. Yana va tout de même essayer de le lancer.",
                    Toast.LENGTH_SHORT);
            t.show();
            if (rep.compareTo("") != 0)
                mTts.speak(rep, TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
            rep = "";
        }
    }

    public void onDestroy() { // Quitte le TTS quand l'application se termine
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }

    public void onResume() {
        getConfig();
        commandes_Layout();
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu) { // Il dit juste que y'a telle ou telle chose dans le menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) { // Il dit que si on clique sur tel objet, on effectue telle action
        if (item.getItemId() == R.id.Btnconfiguration) {
            startActivityForResult(new Intent(this, Configuration.class), OPTION);
        }
        if (item.getItemId() == R.id.updateCom) {
            commandes_actu();
        }
        return super.onOptionsItemSelected(item);
    }

    void getConfig() { // Importe les paramètres

        configuration = new YanaConfiguration(preferences, getApplicationContext());
        MemoryProvider.getInstance().setConfiguration(configuration);
        if (configuration.getIp() != "") iPadress.setText(configuration.getIp());


        box_TTS = preferences.getBoolean("tts_pref", true); // Importe l'état de la box (autorise ou non le TTS)
        if (box_TTS == false) tts_pref_false.setText("Attention ! Votre TTS est désactivé.");
        else tts_pref_false.setText("");

        Log.d(LOG_TAG, "test");

        bienvenue = preferences.getBoolean("bienvenue", true);

        version_ex = preferences.getString("version", "");
        if (version.compareTo(version_ex) != 0) changelog = true;

        update = preferences.getBoolean("update", false);


        mShakeService = new Intent(Yana.this, ShakeService.class); // Démarre le service en fonction de l'état de la box
        boolean box_shake = preferences.getBoolean("shake", false);
        if ((box_shake == true) && servstate == false) {
            startService(mShakeService);
        } else if ((box_shake == false) && servstate == true) {
            stopService(mShakeService);
        } else if ((box_shake == true) && servstate == true) { // Réactualise les variables au cas où on passe d'une reco en continu � une reco par Shake
            stopService(mShakeService);
            startService(mShakeService);
        }

        mEventService = new Intent(Yana.this, EventService.class); // Démarre le service en fonction de l'état de la box
        boolean box_Event = preferences.getBoolean("event", false);
        if ((box_Event == true) && eventstate == false) {
            startService(mEventService);
            EventService.first = false;
        } else if ((box_Event == false) && eventstate == true) {
            stopService(mEventService);
        }


        if ( configuration.getVoiceSens() >= 1) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Attention ! La sensibilité d'analyse de la voix est trop forte. Votre programme choisira la commande la plus proche de votre ordre. Pour mettre une sensibilit�, votre valeur dans les options doit �tre inf�rieure � 10. ",
                    Toast.LENGTH_SHORT);
            t.show();
        }

        ShakeDetector.getConfig(configuration.getShakeSens());

        Log.d(LOG_TAG, "End Of Config");
    }

    void initialisation() { // Initialise le processus
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");

        try {
            startActivityForResult(intent, RESULT_SPEECH);
        } // Lance l'acquisition vocale

        catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Oh bah zut alors ! Ton Android n'a pas install� le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
                    Toast.LENGTH_SHORT);
            t.show();
        }
    }

    void conversation(String texte, String envoi) { // Ici on inscrit la conversation entre l'utilisateur et le RPi

        TextView valueTV = new TextView(this); // Cr�� le TextView pour afficher le message
        valueTV.setText(texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this); // Importe la petite fl�che de droite ou de gauche

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);

        if (envoi == "envoi") {
            fleche.setImageResource(R.drawable.envoi);
            params_fleche.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);

            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.BELOW, (n - 1));

            valueTV.setBackgroundColor(getResources().getColor(R.color.envoi));
        } else {
            fleche.setImageResource(R.drawable.reponse);
            params_fleche.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);

            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.BELOW, (n - 1));

            valueTV.setBackgroundColor(getResources().getColor(R.color.recu));
        }

        n = n + 1;

        valueTV.setPadding(10, 10, 10, 10);
        params.setMargins(20, 0, 20, 20);
        params_fleche.setMargins(0, 0, 0, 20);

        valueTV.setLayoutParams(params);
        fleche.setLayoutParams(params_fleche);
        conversationView.addView(valueTV);
        conversationView.addView(fleche);

        conversation_scroll.post(new Runnable() {
            public void run() {
                conversation_scroll.fullScroll(View.FOCUS_DOWN);
            }
        }); // Pour ancrer en bas � chaque nouvel ordre
    }

    void commandes_Layout() { // Ici, on va inscrire les commandes sur le panel

        if ( MemoryProvider.getInstance().getCategories().size() > 0 ) {
            ExpandableListAdapter listAdapter = new ExpandableListAdapter(this, MemoryProvider.getInstance().getCategories());
            expListView.setAdapter(listAdapter);

            expListView.setOnChildClickListener(new OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long itemID) {
                    int ID = (int) itemID;
                    ArrayList<Commande> reco = MemoryProvider.getInstance().getCategories().get(groupPosition).getData();
                    int i = traitement.comparaison(reco.get(ID).getName());
                    pretraitement(MemoryProvider.getInstance().getCommandes().get(i),i);
                    return false;
                }
            });
        }

        ArrayAdapter<Commande> modeAdapter = new CommandeAdapter(this,traitement.commandes_a);
        commandesListView.setAdapter(modeAdapter);

        commandesListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int arg2, long itemID) {
                int ID = (int) itemID;
                int i = traitement.comparaison(traitement.commandes_a.get(ID).getName());
                pretraitement(MemoryProvider.getInstance().getCommandes().get(i), i);
            }
        });
    }

    void commandes_actu() { // Ici on va actualiser la liste des commandes
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        Parameter params = new Parameter("","");

        if (commande_actu && !configuration.getToken().equals("")) {
            if (activeNetwork != null) {
                if (traitement.pick_JSON(iPadress.getText().toString(), configuration.getToken())) { // Commence le protocole de reception et les enregistre dans une ArrayList
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Update fait !", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
                            MemoryProvider.getInstance().getCommandes().get(MemoryProvider.getInstance().getCommandes().size() - 1).getName(), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
                    toast.show();
                    traitement.commandes_a = new ArrayList<Commande>(MemoryProvider.getInstance().getCommandes());
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
                        "Vous n'avez pas de connexion internet !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
                toast.show();
                traitement.commandes_a = new ArrayList<Commande>(MemoryProvider.getInstance().getCommandes());
            }
        } else if (configuration.getToken().isEmpty() && !configuration.isAi()) {

            MemoryProvider.getInstance().getCommandes().clear();

            MemoryProvider.getInstance().initData(false);

            Commande command = new Commande("Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.",params);
            MemoryProvider.getInstance().getCommandes().add(command);


            Toast toast = Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
                    MemoryProvider.getInstance().getCommandes().get(MemoryProvider.getInstance().getCommandes().size() - 1).getName(), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
            toast.show();

            traitement.commandes_a = new ArrayList<Commande>(MemoryProvider.getInstance().getCommandes());
        } else {
            commande_actu = true;
            MemoryProvider.getInstance().getCommandes().clear();

            MemoryProvider.getInstance().initData(false);

            Commande command = new Commande("Vous n'avez pas encore actualisé vos commandes.",params);
            MemoryProvider.getInstance().getCommandes().add(command);

            traitement.commandes_a = new ArrayList<Commande>(MemoryProvider.getInstance().getCommandes());
        }

        commandes_Layout();
    }

    void pretraitement(final Commande commande, final int n) { // Ici, on va analyser la r�ponse si elle est traitable localement. Sinon, on l'envoie au RPi
        conversation(commande.getName(), "envoi");

        if (n > 0) {
            Parameter params = commande.getParameter();
            Log.d("", "Params : " + params);

            if (params.getType() != null) {
                String reponse = "";
                String type = params.getType();
                if (type.equals("talk")) {
                    reponse = params.getContenu();
                    if (box_TTS == true && !traitement.sons) { // Lance la synth�se vocale si les options l'autorisent
                        mTts = new TextToSpeech(this, this);
                    }
                } else if (type.equals("sound")) {
                    String son = params.getContenu();
                    reponse = "*" + son + "*";
                    try {
                        int ID = getResources().getIdentifier(son, "raw", "fr.nover.yana");

                        MediaPlayer mp = MediaPlayer.create(this, ID);
                        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                        mp.start();
                    } catch (Exception e) {
                    }
                }

                if (reponse.compareTo("") == 0) conversation(reponse, "reponse");
            }
        }

        myHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                pretraitement2(commande, n);
            }
        }, 250);
    }

    void pretraitement2(Commande commande, int n) { // Deuxi�me partie du Pr�traitement (MyHandler l'oblige pour afficher l'ordre avant le traitement)
        rep = "";

        if (traitement.verif_aux(commande.getName(), this)) rep = traitement.rep;  // V�rification auxiliaire
        else if (n == -1)
            rep = "Je ne vois aucun ordre qui ressemble à ce que vous avez dit..."; // Si Ordre=Recrep alors c'est que la reconnaissance par pertinence a �chou�
        else if (commande.getParameter().getUrl().compareTo("") == 0)
            rep = ""; // Si l'ordre ne contient aucun URL, �a n'est pas utile de l'envoyer au serveur
        else {
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null) { // V�rifie le r�seau
                Log.d("Ordre", "" + commande.getName());
                rep = traitement.HTTP_Contact(configuration.getCommandUrl(commande), getApplicationContext());
            } // Envoie au RPi et enregistre sa r�ponse
            else {
                Toast toast = Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
                        "Vous n'avez pas de connexion internet !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 80);
                toast.show();
            }
        }

        Log.d(LOG_TAG, "Rep : " + rep);

        if (rep.compareTo("") != 0) { // Si la r�ponse n'est pas valide, �a ne sert � rien de la dire ni de l'�noncer
            conversation(rep, "reponse");

            if (box_TTS == true && rep.length() < 300 && !traitement.sons) { // Lance la synth�se vocale si les options l'autorisent et si la r�ponse n'est pas trop longue
                mTts = new TextToSpeech(this, this);
            }
        }
    }


}