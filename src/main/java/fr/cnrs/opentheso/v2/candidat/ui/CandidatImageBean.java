package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named("v2ImageCandidatBean")
public class CandidatImageBean implements Serializable {

    private final transient CandidatBean candidatBean;
    private final transient CandidatMutationService candidatMutationService;
    private final transient ThesaurusContext thesaurusContext;

    private String uri, copyright, name, creator;

    public void addNewImage(int idUser) {
        if (StringUtils.isBlank(uri)) {
            MessageUtils.showErrorMessage("Aucune URI insérée !");
            return;
        }

        candidatMutationService.addExternalImage(
                candidatBean.getCandidatSelected().getIdConcepte(),
                thesaurusContext.resolveThesaurusId(),
                name, copyright, uri, creator, idUser);

        candidatBean.getCandidatSelected().setImages(candidatMutationService.loadExternalImages(
                candidatBean.getCandidatSelected().getIdThesaurus(),
                candidatBean.getCandidatSelected().getIdConcepte()));

        MessageUtils.showInformationMessage("Image ajoutée avec succès");
        initImageDialog();
        PrimeFaces.current().ajax().update("tabViewCandidat");
    }

    public void deleteImage(String imageUri) {
        candidatMutationService.deleteExternalImage(
                thesaurusContext.resolveThesaurusId(),
                candidatBean.getCandidatSelected().getIdConcepte(),
                imageUri);

        candidatBean.getCandidatSelected().setImages(candidatMutationService.loadExternalImages(
                candidatBean.getCandidatSelected().getIdThesaurus(),
                candidatBean.getCandidatSelected().getIdConcepte()));

        MessageUtils.showInformationMessage("Image supprimée avec succès");
    }

    public void initImageDialog() {
        uri = null;
        copyright = null;
        name = null;
        creator = null;
    }
}
