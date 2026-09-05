package fr.cnrs.opentheso.v2.toolbox.workshop.io;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvReader;

/**
 * Lecteur CSV de l'atelier : même parser que l'édition thésaurus.
 */
public class WorkshopCsvReader extends ThesaurusCsvReader {

    public WorkshopCsvReader(char delimiter) {
        super(delimiter);
    }
}
