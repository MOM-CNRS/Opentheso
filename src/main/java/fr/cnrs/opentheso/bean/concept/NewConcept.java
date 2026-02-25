package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.facets.NodeFacet;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.relations.NodeTypeRelation;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named("newConcept")
public class NewConcept implements Serializable {

    private final Tree tree;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final GroupService groupService;
    private final ConceptAddService conceptAddService;
    private final ConceptService conceptService;
    private final TermService termService;
    private final RelationService relationService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;

    private boolean isCreated, duplicate, isConceptUnderFacet;
    private String prefLabel, notation, idNewConcept, source, relationType, idGroup, idBTfacet, idFacet;
    private List<NodeTypeRelation> typesRelationsNT;
    private List<NodeGroup> nodeGroups;
    private List<NodeSearchMini> nodeSearchMinis;


    public void reset() {
        isCreated = false;
        duplicate = false;
        prefLabel = null;
        idNewConcept = null;
        notation = null;
        isConceptUnderFacet = false;

        if (conceptBean.getNodeConcept() != null &&
                CollectionUtils.isNotEmpty(conceptBean.getNodeConcept().getNodeConceptGroup())) {
            idGroup = conceptBean.getNodeConcept().getNodeConceptGroup().get(0).getConceptGroup().getIdGroup();
        }

        typesRelationsNT = relationService.getTypesRelationsNT();
        nodeGroups = groupService.getListConceptGroup(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
    }

    public void resetForFacet(NodeFacet nodeFacet) {
        reset();
        isConceptUnderFacet = true;
        idBTfacet = nodeFacet.getIdConceptParent();
        idFacet = nodeFacet.getIdFacet();
    }

    public void infosTopConcept() {
        MessageUtils.showMessage(FacesMessage.SEVERITY_WARN, "Information", "Rédiger une aide ici pour Add Top Concept!");
    }

    public void addTopConcept(String idLang, String status, String idThesaurus, int idUser) {

        isCreated = false;
        duplicate = false;

        if (!validateFields(idThesaurus, idLang)) return;

        Concept concept = buildConcept(idThesaurus, status);
        Term term = buildTerm(idThesaurus, idLang, status);

        idNewConcept = conceptAddService.addConcept(null, null, concept, term, idUser);
        if (idNewConcept == null) {
            MessageUtils.showInformationMessage("Erreur pendant la création du concept");
            return;
        }

        log.debug("Enregistrement du trace de l'action");
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CREATOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idNewConcept)
                .idThesaurus(idThesaurus)
                .build());

        conceptBean.getConcept(idThesaurus, idNewConcept, idLang, currentUser);
        isCreated = true;
        
        MessageUtils.showInformationMessage("Le top concept a bien été ajouté");
        tree.addNewChild(tree.getRoot(), idNewConcept, idThesaurus, idLang, notation);
        tree.expandTreeToPath(idNewConcept, idThesaurus, idLang);
        init();

        PrimeFaces.current().ajax().update("containerIndex");
    }

    private boolean validateFields(String idTheso, String idLang) {
        if (StringUtils.isBlank(prefLabel)) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_WARN, "Attention!", "Le label est obligatoire !");
            return false;
        }
        String label = prefLabel.trim();
        if (termService.existsPrefLabel(label, idLang, idTheso)) {
            duplicate = true;
            MessageUtils.showMessage(FacesMessage.SEVERITY_WARN, "Attention!", "Un prefLabel existe déjà avec ce nom !");
            return false;
        }
        if (termService.isAltLabelExist(label, idTheso, idLang)) {
            duplicate = true;
            MessageUtils.showMessage(FacesMessage.SEVERITY_WARN, "Attention!", "Un synonyme existe déjà avec ce nom !");
            return false;
        }
        if (StringUtils.isNotBlank(notation) && conceptService.isNotationExist(idTheso, notation.trim())) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Attention!", "Notation existe déjà, veuillez choisir une autre !");
            return false;
        }
        if (StringUtils.isNotBlank(idNewConcept) && conceptAddService.isIdExiste(idNewConcept, idTheso)) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Attention!", "Identifiant déjà attribué !");
            return false;
        }
        return true;
    }

    private Concept buildConcept(String idTheso, String status) {
        return Concept.builder()
                .idGroup(idGroup)
                .idThesaurus(idTheso)
                .idConcept(StringUtils.defaultIfBlank(idNewConcept, null))
                .status(status)
                .notation(notation)
                .topConcept(false)
                .build();
    }

    private Term buildTerm(String idTheso, String idLang, String status) {
        return Term.builder()
                .idThesaurus(idTheso)
                .lang(idLang)
                .lexicalValue(prefLabel.trim())
                .source(StringUtils.defaultString(source))
                .status(status)
                .build();
    }

    private void init() {
        duplicate = false;
        idNewConcept = null;
        prefLabel = "";
        notation = "";
    }

    public void cancel() {
        duplicate = false;
    }
}
