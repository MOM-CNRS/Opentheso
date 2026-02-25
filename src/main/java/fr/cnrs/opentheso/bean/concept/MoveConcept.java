package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.bean.candidat.CandidatBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "moveConcept")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MoveConcept implements Serializable {

    private final SearchService searchService;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final CandidatBean candidatBean;
    private final ThesaurusService thesaurusService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final GroupService groupService;
    private final PreferenceService preferenceService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final UserService userService;
    private final RelationService relationService;

    private String idThesoFrom, idThesoTo, idConceptFrom;
    private NodeSearchMini nodeSearchSelected;
    private List<String> idConceptsToMove;


    public void initForCandidate(List idConcepts1, String idThesoFrom) {
        idConceptsToMove = new ArrayList<>();
        this.idConceptsToMove = idConcepts1;
        this.idThesoFrom = idThesoFrom;
        nodeSearchSelected = null;
    }
    
    public void initForConcept(String idConcept, String idThesoFrom) {
        this.idConceptsToMove = new ArrayList<>();
        this.idConceptFrom = idConcept;
        this.idConceptsToMove = conceptService.getIdsOfBranch(idConcept, selectedTheso.getCurrentIdTheso());
        this.idThesoFrom = idThesoFrom;
        this.idThesoTo = null;
        nodeSearchSelected = null;
    }

    /**
     * permet de déplacer des candidats d'un thésaurus vers un autre
     */
    public void moveConceptCA() {

        if (StringUtils.isEmpty(idThesoTo) || StringUtils.isEmpty(idThesoFrom) || idConceptsToMove == null || idConceptsToMove.isEmpty()) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        for (String idConcept : idConceptsToMove) {
            if(!conceptService.moveConceptToAnotherThesaurus(idConcept, idThesoFrom, idThesoTo)) {
                MessageUtils.showErrorMessage(" Le déplacement a échoué !");
                return;
            }

            conceptService.updateDateOfConcept(idThesoTo, idConcept, currentUser.getNodeUser().getIdUser());

            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(currentUser.getNodeUser().getName())
                    .idConcept(idConcept)
                    .idThesaurus(idThesoTo)
                    .build());
        }

        candidatBean.initCandidatModule();
        PrimeFaces.current().ajax().update("tabViewCandidat:panelCandidateList");
        MessageUtils.showInformationMessage("Le déplacement a réussi");
    }

    public void moveConcept() throws IOException {

        if (StringUtils.isEmpty(idThesoTo) || StringUtils.isEmpty(idConceptFrom) || StringUtils.isEmpty(idThesoFrom) || idConceptsToMove == null || idConceptsToMove.isEmpty()) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        List<String> lisIdGroup;
        var nodePreference = preferenceService.getThesaurusPreferences(idThesoTo);
        for (String idConcept : idConceptsToMove) {
            if(!conceptService.moveConceptToAnotherThesaurus(idConcept, idThesoFrom, idThesoTo)) {
                MessageUtils.showErrorMessage("Le déplacement a échoué !");
                return;
            }
            conceptService.updateDateOfConcept(idThesoTo, idConcept, currentUser.getNodeUser().getIdUser());

            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(currentUser.getNodeUser().getName())
                    .idConcept(idConcept)
                    .idThesaurus(idThesoTo)
                    .build());


            lisIdGroup = groupService.getListIdGroupOfConcept(idThesoTo, idConcept);
            for (String idGroup : lisIdGroup) {
                groupService.deleteRelationConceptGroupConcept(idGroup, idConcept, idThesoTo);
            }

            var concept = conceptService.getConcept(idConcept);
            if (concept != null) {
                if(!StringUtils.isEmpty(concept.getIdArk())) {
                    conceptAddService.generateArkId(idThesoTo, List.of(idConcept), selectedTheso.getCurrentLang(), nodePreference);
                }
            }
        }

        // suppression des BT du concept de tête à déplacer
        var listBt = relationService.getListIdBT(idConceptFrom, idThesoTo);
        for (String idBt : listBt) {
            relationService.deleteRelationBT(idConceptFrom, idThesoTo, idBt, currentUser.getNodeUser().getIdUser());
        }

        if(nodeSearchSelected != null && !StringUtils.isEmpty(nodeSearchSelected.getIdConcept())) {
            //cas où le déplacement est vers un concept, on attache ce nouveau concept au concept cible
            relationService.addRelationBT(idConceptFrom, idThesoTo, nodeSearchSelected.getIdConcept(), currentUser.getNodeUser().getIdUser());
            conceptService.setTopConcept(idConceptFrom, idThesoTo, false);
        } else {
            conceptService.setTopConcept(idConceptFrom, idThesoTo, true);
        }

        selectedTheso.reloadSelectedTheso();

        MessageUtils.showInformationMessage("Le déplacement a réussi");
    }

    public List<NodeIdValue> getListThesoAsAdmin(){
        if(currentUser.getNodeUser() == null) return null;

        List<String> authorizedThesaurusAsAdmin;
        if(currentUser.getNodeUser().isSuperAdmin()) {
            authorizedThesaurusAsAdmin = thesaurusService.getAllIdOfThesaurus(true);
        } else {
            authorizedThesaurusAsAdmin = userService.getThesaurusOfUserAsAdmin(currentUser.getNodeUser().getIdUser());
        }

        return authorizedThesaurusAsAdmin.stream()
                .filter(idThesaurus -> !selectedTheso.getCurrentIdTheso().equalsIgnoreCase(idThesaurus))
                .map(idThesaurus -> NodeIdValue.builder()
                        .id(idThesaurus)
                        .value(thesaurusService.getTitleOfThesaurus(idThesaurus, preferenceService.getWorkLanguageOfThesaurus(idThesaurus)))
                        .build())
                .toList();
    }

    /// Cette focntion est nécessaire pour activer l'autocomplete
    public void action(){}

    public List<NodeSearchMini> searchConceptsAutoComplet(String value) {

        if (!StringUtils.isEmpty(idThesoTo)) {
            return searchService.searchAutoCompletionForRelation(value, selectedTheso.getCurrentLang(), idThesoTo, true);
        }
        return List.of();
    }
    
}
