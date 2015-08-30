package fr.nover.yana.model;

/**
 * Created by sylvain on 29/08/15.
 */
public class Commande {

    private String name;
    private Parameter parameter;

    public Commande(String name) {
        this.name = name;
        this.parameter = new Parameter("","");
    }

    public Commande(String name, Parameter params) {
        this(name);
        this.parameter = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }
}
