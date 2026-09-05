package fr.cnrs.opentheso.v2.candidat.export;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProcessedCandidatesCsvWriter {

    public byte[] write(List<CandidatDto> candidates, char delimiter) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, Charset.forName("UTF-8"));
                 CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().setDelimiter(delimiter).build())) {
                csvFilePrinter.printRecord(List.of(
                        "Id", "Candidat", "Créé par", "Date de création", "Traité par",
                        "Date de traitement", "Message de l'admin", "Votes", "Votes de notes",
                        "Nombre de participants"));
                if (candidates == null || candidates.isEmpty()) {
                    return null;
                }
                for (CandidatDto candidatDto : candidates) {
                    ArrayList<Object> csvRow = new ArrayList<>();
                    csvRow.add(candidatDto.getIdConcepte());
                    csvRow.add(candidatDto.getNomPref());
                    csvRow.add(candidatDto.getCreatedBy());
                    csvRow.add(candidatDto.getCreationDate());
                    csvRow.add(candidatDto.getCreatedByAdmin());
                    csvRow.add(candidatDto.getInsertionDate());
                    csvRow.add(candidatDto.getAdminMessage());
                    csvRow.add(candidatDto.getNbrVote());
                    csvRow.add(candidatDto.getNbrNoteVote());
                    csvRow.add(candidatDto.getNbrParticipant());
                    csvFilePrinter.printRecord(csvRow);
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            log.error("Erreur pendant l'export CSV des candidats traités", e);
            return null;
        }
    }
}
