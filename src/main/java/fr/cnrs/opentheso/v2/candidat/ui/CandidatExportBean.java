package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

@Slf4j
@Named("v2CandidatExportBean")
@ViewScoped
@RequiredArgsConstructor
public class CandidatExportBean implements Serializable {

    private final CandidatExportService candidatExportService;
    private final CandidatBean candidatBean;

    public StreamedContent exportPendingCandidates() {
        try {
            candidatBean.resetExportProgress();
            var result = candidatExportService.exportPendingCandidates(
                    candidatBean.getActiveThesaurusId(),
                    candidatBean.getCandidatList(),
                    candidatBean.getSelectedExportFormat(),
                    candidatBean::updateExportProgress
            );
            candidatBean.setListCandidatsActivate(true);
            return DefaultStreamedContent.builder()
                    .contentType(result.contentType())
                    .name(result.filename())
                    .stream(() -> new ByteArrayInputStream(result.content()))
                    .build();
        } catch (Exception ex) {
            log.error("Export SKOS des candidats impossible", ex);
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : "Erreur pendant l'export des candidats"
            );
            return DefaultStreamedContent.builder()
                    .contentType("text/plain")
                    .name("export-error.txt")
                    .stream(() -> new ByteArrayInputStream(new byte[0]))
                    .build();
        }
    }
}
