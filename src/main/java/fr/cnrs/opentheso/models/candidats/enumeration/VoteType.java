package fr.cnrs.opentheso.models.candidats.enumeration;

public enum VoteType {

    CANDIDAT("CA"),
    NOTE("NT"),
    CONTRE("CN");

    private String label;

    VoteType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
