package fr.cnrs.opentheso.stats.dto;

/**
 * Une ligne de la distribution "nombre de concepts ayant exactement N
 * langues renseignées" (ex : 2 langues -> 510 concepts).
 */
public class LanguageCoverageStat {

    private final int nbLangues;
    private final long nbConcepts;

    public LanguageCoverageStat(int nbLangues, long nbConcepts) {
        this.nbLangues = nbLangues;
        this.nbConcepts = nbConcepts;
    }

    public int getNbLangues() {
        return nbLangues;
    }

    public long getNbConcepts() {
        return nbConcepts;
    }
}
