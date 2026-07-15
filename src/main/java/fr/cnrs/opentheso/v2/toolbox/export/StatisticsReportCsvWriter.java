package fr.cnrs.opentheso.v2.toolbox.export;

import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.function.Function;

@Data
@Slf4j
public class StatisticsReportCsvWriter {

    private final String separator = ";";
    private BufferedWriter writer;
    private ByteArrayOutputStream output;

    public void createGenericStatistiquesRapport(List<GenericStatistiqueData> datas) {
        writeCSV(
                datas,
                "Collection;Concepts;Synonymes;Termes non traduits;Notes;Align Wikidata;Total align",
                data -> String.join(separator,
                        data.getCollection(),
                        String.valueOf(data.getConceptsNbr()),
                        String.valueOf(data.getSynonymesNbr()),
                        String.valueOf(data.getTermesNonTraduitsNbr()),
                        String.valueOf(data.getNotesNbr()),
                        String.valueOf(data.getWikidataAlignNbr()),
                        String.valueOf(data.getTotalAlignment()))
        );
    }

    public void createConceptsStatistiquesRapport(List<ConceptStatisticData> datas) {
        writeCSV(
                datas,
                "IdConcept;Label;Type;Date création;Date modification;Utilisateur",
                data -> String.join(separator,
                        data.getIdConcept(),
                        data.getLabel(),
                        data.getType(),
                        data.getDateCreation() != null ? data.getDateCreation() : "",
                        data.getDateModification() != null ? data.getDateModification() : "",
                        data.getUtilisateur() != null ? data.getUtilisateur() : "")
        );
    }

    private <T> void writeCSV(List<T> datas, String header, Function<T, String> lineMapper) {
        output = new ByteArrayOutputStream();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(output));
            writer.write(header);
            writer.newLine();
            for (T data : datas) {
                writer.write(lineMapper.apply(data));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Erreur pendant la création du rapport CSV", e);
        }
    }
}
