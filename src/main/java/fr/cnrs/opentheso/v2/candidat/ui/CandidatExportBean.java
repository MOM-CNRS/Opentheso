package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.utils.MessageUtils;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

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
            return DefaultStreamedContent.builder()
                    .contentType(result.contentType())
                    .name(result.filename())
                    .stream(() -> new ByteArrayInputStream(result.content()))
                    .build();
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Erreur pendant l'export des candidats");
            return null;
        }
    }
}
