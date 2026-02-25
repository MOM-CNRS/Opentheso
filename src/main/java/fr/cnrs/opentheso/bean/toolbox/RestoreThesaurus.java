package fr.cnrs.opentheso.bean.toolbox;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
@Getter
@Setter
@RequestScoped
@RequiredArgsConstructor
@Named(value = "restoreThesaurus")
public class RestoreThesaurus implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final RestoreThesaurusService restoreThesaurusService;
    private final SelectedTheso selectedTheso;
    private final ConceptView conceptView;

    private boolean overwrite, overwriteLocalArk;
    private String naan, prefix;


    public void generateSitemap() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        restoreThesaurusService.generateSitemap(idThesaurus);
    }

    /**
     * permet de corriger l'appartenance d'un cocnept à une collection,
     * si la collection n'existe plus, on supprime l'appartenance de ces concepts à la collection en question
     */
    public void reorganizeConceptsAndCollections() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        restoreThesaurusService.reorganizeConceptsAndCollections(idThesaurus);
        MessageUtils.showInformationMessage("Correction réussie !!!");
    }

    /**
     * Permet de supprimer les relations de type (a -> BT -> b et b -> BT -> a )
     * parcours toute la bracnche en partant du concept en paramètre
     */
    public void deleteLoopRelations(){

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        var idConcept = conceptView.getNodeConcept().getConcept().getIdConcept();
        if (StringUtils.isEmpty(idConcept)) {
            MessageUtils.showErrorMessage("Erreur manque de paramètres");
            return;
        }

        restoreThesaurusService.deleteLoopRelations(idThesaurus, idConcept);

        MessageUtils.showInformationMessage("Correction terminée");
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    public void reorganizing() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        restoreThesaurusService.reorganizing(idThesaurus);
        MessageUtils.showInformationMessage("Correction réussie !!!");
    }

    public void correctDisplayTopTerm() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        int count = restoreThesaurusService.correctDisplayTopTerm(idThesaurus);
        MessageUtils.showInformationMessage("Correction réussie, concepts affecté : " + count);
    }

    public void switchRolesFromTermToConcept() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        restoreThesaurusService.switchRolesFromTermToConcept(idThesaurus, workLanguage);
        MessageUtils.showInformationMessage("Correction réussie !!!");
    }

    /**
     * permet de générer les id Ark d'après l'id du concept,
     * si overwrite est activé, on écrase tout et on recommence avec des nouveaus Id Ark
     */
    public void generateArkFromConceptId() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        prefix = StringUtils.isEmpty(prefix) ? "" : prefix.trim();
        var nbrArkGenerated = restoreThesaurusService.generateArkFromConceptId(idThesaurus, prefix, naan, overwrite);
        MessageUtils.showInformationMessage("Concepts changés: " + nbrArkGenerated);
    }

    /**
     * permet de générer les id Ark en local en se basant au paramètre prédéfini dans Identifiant
     * si overwrite est activé, on écrase tout et on recommence avec des nouveaus Id Ark
     */
    public void generateArkLacal() {

        var idThesaurus = selectedTheso.getCurrentIdTheso();
        if (StringUtils.isEmpty(idThesaurus)) {
            log.error("Aucun thésaurus n'est présent");
            return;
        }

        var nbrArkGenerated = restoreThesaurusService.generateArkLacal(idThesaurus, overwriteLocalArk);
        MessageUtils.showInformationMessage("Concepts changés: " + nbrArkGenerated);
    }
}
