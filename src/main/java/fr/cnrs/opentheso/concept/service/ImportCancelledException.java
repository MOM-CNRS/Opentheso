package fr.cnrs.opentheso.concept.service;

/**
 * L'utilisateur a demandé l'arrêt d'un import encore en cours.
 * Les lots déjà écrits doivent être retirés par l'appelant.
 */
public class ImportCancelledException extends RuntimeException {

    public ImportCancelledException() {
        super("Import annulé");
    }
}
