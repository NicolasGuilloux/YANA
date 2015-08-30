package fr.nover.yana.model;

/**
 * Created by sylvain on 29/08/15.
 */
public class Parameter {

    private String url;
    private String confidence;
    private String type;



    private String contenu;

    public Parameter(String url, String confidence) {
        this.url = url;
        this.confidence = confidence;
    }

    public String getUrl() {
        return url;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
