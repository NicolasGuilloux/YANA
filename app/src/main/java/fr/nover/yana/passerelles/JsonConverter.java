package fr.nover.yana.passerelles;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.StringTokenizer;

import fr.nover.yana.model.Parameter;

/**
 * Created by sylvain on 29/08/15.
 */
public class JsonConverter {
    public Parameter convertParameters(JSONObject emp) throws JSONException {
        Parameter params = new Parameter("","");

        String URL = emp.getString("url");
        URL = URL.replace("{", "%7B");
        URL = URL.replace("}", "%7D");
        StringTokenizer tokens = new StringTokenizer(URL, "?");
        tokens.nextToken();
        URL = tokens.nextToken();
        params.setUrl(URL);

        params.setConfidence(emp.getString("confidence"));

        String type = "", contenu = "";
        try {
            emp.getString("PreAction");
            type = emp.getString("type");
            if (type.compareTo("talk") == 0) contenu = emp.getString("sentence");
            else if (type.compareTo("sound") == 0) {
                contenu = emp.getString("file");
                contenu = contenu.replace(".wav", "");
                contenu = contenu.replace(".mp3", "");
            }
            params.setType(type);
            params.setContenu(contenu);
        } catch (Exception e) {
        }

        return params;
    }
}
