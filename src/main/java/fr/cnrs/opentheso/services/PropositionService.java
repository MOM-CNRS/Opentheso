package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.PropositionModification;
import fr.cnrs.opentheso.entites.PropositionModificationDetail;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.PropositionProjection;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.propositions.Proposition;
import fr.cnrs.opentheso.models.propositions.PropositionActionEnum;
import fr.cnrs.opentheso.models.propositions.PropositionCategoryEnum;
import fr.cnrs.opentheso.models.propositions.PropositionDao;
import fr.cnrs.opentheso.models.propositions.PropositionFromApi;
import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.models.concept.NodeConcept;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.proposition.NotePropBean;
import fr.cnrs.opentheso.bean.proposition.SynonymPropBean;
import fr.cnrs.opentheso.bean.proposition.TraductionPropBean;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class PropositionService {

    private final Tree tree;
    private final ConceptView conceptView;
    private final CurrentUser currentUser;
    private final IndexSetting indexSetting;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final MailService mailBean;
    private final TermRepository termRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final PropositionModificationRepository propositionModificationRepository;
    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NonPreferredTermService nonPreferredTermService;
    private final TermService termService;
    private final PreferenceService preferenceService;
    private final PreferredTermRepository preferredTermRepository;
    private final PropositionRepository propositionRepository;
    private final ConceptService conceptService;
    private final NoteService noteService;


    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public boolean envoyerProposition(Proposition proposition, String nom, String email, String commentaire, String thesaurusName) {

        log.debug("Début de l'envoi de la proposition");
        if (propositionModificationRepository.findPropositionByEmailConceptLang(email, proposition.getConceptID(),
                selectedTheso.getCurrentLang()) != null) {

            log.error("Il existe déjà une proposition pour le même concept en cours de traitement !");
            MessageUtils.showWarnMessage("Vous avez déjà une proposition pour le même concept en cours de traitement !");
            return false;
        }

        var propositionModification = PropositionModification.builder()
                .nom(nom)
                .email(email)
                .commentaire(commentaire)
                .idConcept(proposition.getConceptID())
                .idTheso(selectedTheso.getCurrentIdTheso())
                .lang(selectedTheso.getCurrentLang())
                .status(PropositionStatusEnum.ENVOYER.name())
                .date(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()))
                .build();

        log.debug("Recherche du nom du thésaurus à partir de son id {} et la langue {}",
                selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());

        try {
            var subject = "[Opentheso] Confirmation de l'envoi de votre proposition";
            var contentFile = "<html><body>"
                    + "Cher(e) " + propositionModification.getNom() + ", <br/> "
                    + "<p> Votre proposition a bien été reçue par nos administrateurs, elle sera étudiée dans les plus brefs délais.<br/>"
                    + "Vous recevrez un mail dès que votre proposition sera traitée.<br/></p> "
                    + "Nous vous remercions de votre contribution à l'enrichissement du thésaurus <b>" + thesaurusName + "(" + propositionModification.getIdTheso() + ")" + "</b> "
                    + "(concept : <a href=\"" + getPath() + "/?idc=" + propositionModification.getIdConcept()
                    + "&idt=" + propositionModification.getIdTheso() + "\">" + proposition.getNomConcept().getLexicalValue() + "</a>). <br/><br/> Cordialement,<br/>"
                    + "L'équipe " + thesaurusName + ".<br/> <img src=\"" + getPath() + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

            log.debug("Envoi de l'émail...");
            sendEmail(propositionModification.getEmail(), subject, contentFile);
        } catch (Exception ex) {
            MessageUtils.showWarnMessage("Erreur détectée pendant l'envoie du mail de notification! \nVotre proposition a été enregistrée !");
        }

        log.debug("Enregistrement de la proposition dans la base");
        var propositionSaved = propositionModificationRepository.save(propositionModification);

        if (StringUtils.isNotEmpty(proposition.getNomConceptProp())) {
            propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                    .idProposition(propositionSaved.getId())
                    .action(PropositionActionEnum.UPDATE.name())
                    .categorie(PropositionCategoryEnum.NOM.name())
                    .lang(selectedTheso.getCurrentLang())
                    .value(proposition.getNomConceptProp())
                    .oldValue(proposition.getNomConcept().getLexicalValue())
                    .build());
        }

        if (!CollectionUtils.isEmpty(proposition.getSynonymsProp())) {
            log.debug("Enregistrement des synonymes présent dans la proposition");
            for (SynonymPropBean synonymProp : proposition.getSynonymsProp()) {
                saveSynonyms(propositionSaved.getId(), synonymProp);
            }
        }

        if (!CollectionUtils.isEmpty(proposition.getTraductionsProp())) {
            log.debug("Enregistrement des traductions présents dans la proposition");
            for (var traductionProp : proposition.getTraductionsProp()) {
                if (traductionProp.isToAdd() || traductionProp.isToUpdate() || traductionProp.isToRemove()) {
                    var propositionDetail = new PropositionModificationDetail();

                    if (traductionProp.isToAdd()) {
                        propositionDetail.setAction(PropositionActionEnum.ADD.name());
                        propositionDetail.setOldValue(traductionProp.getLexicalValue());
                    } else if (traductionProp.isToRemove()) {
                        propositionDetail.setAction(PropositionActionEnum.DELETE.name());
                        propositionDetail.setOldValue(traductionProp.getOldValue());
                    } else {
                        propositionDetail.setAction(PropositionActionEnum.UPDATE.name());
                        propositionDetail.setOldValue(traductionProp.getOldValue());
                    }

                    propositionDetail.setIdProposition(propositionSaved.getId());
                    propositionDetail.setCategorie(PropositionCategoryEnum.TRADUCTION.name());
                    propositionDetail.setLang(traductionProp.getLang());
                    propositionDetail.setValue(traductionProp.getLexicalValue());
                    propositionDetail.setIdTerm(traductionProp.getIdTerm());
                    propositionModificationDetailRepository.save(propositionDetail);
                }
            }
        }

        if (proposition.getNote() != null) {
            log.debug("Enregistrement des notes présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getNote(), PropositionCategoryEnum.NOTE.name());
        }

        if (proposition.getChangeNote() != null) {
            log.debug("Enregistrement des changes notes présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getChangeNote(), PropositionCategoryEnum.CHANGE_NOTE.name());
        }

        if (proposition.getDefinition() != null) {
            log.debug("Enregistrement des définitions présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getDefinition(), PropositionCategoryEnum.DEFINITION.name());
        }

        if (proposition.getEditorialNote() != null) {
            log.debug("Enregistrement des editorials notes présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getEditorialNote(), PropositionCategoryEnum.EDITORIAL_NOTE.name());
        }

        if (proposition.getExample() != null) {
            log.debug("Enregistrement des examples présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getExample(), PropositionCategoryEnum.EXAMPLE.name());
        }

        if (proposition.getHistoryNote() != null) {
            log.debug("Enregistrement des histories notes présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getHistoryNote(), PropositionCategoryEnum.HISTORY.name());
        }

        if (proposition.getScopeNote() != null) {
            log.debug("Enregistrement des scopes présents dans la proposition");
            noteManagement(propositionSaved.getId(), proposition.getScopeNote(), PropositionCategoryEnum.SCOPE.name());
        }

        return true;
    }

    private void saveSynonyms(Integer propositionId, SynonymPropBean synonymProp) {
        if (synonymProp.isToAdd() || synonymProp.isToUpdate() || synonymProp.isToRemove()) {
            PropositionActionEnum action;
            if (synonymProp.isToAdd()) {
                action = PropositionActionEnum.ADD;
            } else if (synonymProp.isToRemove()) {
                action = PropositionActionEnum.DELETE;
            } else {
                action = PropositionActionEnum.UPDATE;
            }

            propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                    .idProposition(propositionId)
                    .action(action.name())
                    .categorie(PropositionCategoryEnum.SYNONYME.name())
                    .lang(synonymProp.getLang())
                    .value(synonymProp.getLexicalValue())
                    .oldValue(synonymProp.getOldValue())
                    .status(synonymProp.getStatus())
                    .hiden(synonymProp.isHiden())
                    .idTerm(synonymProp.getIdTerm())
                    .build());
        }
    }

    private String getPath() {
        if (FacesContext.getCurrentInstance() == null) {
            return "";
        }

        return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin")
                + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    public int searchNbrNewProposition() {

        log.debug("Recherche du nombre des nouvelles propositions");
        if(StringUtils.isEmpty(selectedTheso.getCurrentIdTheso()))
            return 0;

        var result = propositionModificationRepository.findAllByIdThesoAndStatus(selectedTheso.getCurrentIdTheso(),
                PropositionStatusEnum.ENVOYER.name());
        return CollectionUtils.isEmpty(result) ? 0 : result.size();
    }

    public void sendEmail(String emailDestination, String subject, String contentFile) throws IOException {
        if (currentUser.getNodeUser() != null
                && !currentUser.getNodeUser().getMail().equalsIgnoreCase(emailDestination)
                && currentUser.getNodeUser().isAlertMail()) {
            new Thread(() -> mailBean.sendMail(emailDestination, subject, contentFile)).start();
            //mailBean.sendMail(emailDestination, subject, contentFile);
        }
    }

    public void refuserProposition(PropositionDao propositionSelected, String commentaireAdmin) {

        var propositionSaved = propositionModificationRepository.findById(propositionSelected.getId());
        if (propositionSaved.isPresent()) {
            propositionSaved.get().setStatus(PropositionStatusEnum.REFUSER.name());
            propositionSaved.get().setApprouvePar(currentUser.getNodeUser().getName());
            propositionSaved.get().setDate(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()));
            propositionSaved.get().setAdminComment(commentaireAdmin);
            propositionModificationRepository.save(propositionSaved.get());
        }

        try {
            var subject = "[Opentheso] Résultat de votre proposition";
            var contentFile = "<html><body>"
                    + "Cher(e) " + propositionSelected.getNom() + ", <br/> "
                    + "<p>Votre proposition d’enrichissement sur le concept " + propositionSelected.getNomConcept() + " du thésaurus " + propositionSelected.getThesoName()
                    + " a été refusée par les administrateurs.<br/>"
                    + "Voici leur message: <br/>"
                    + propositionSelected.getAdminComment() + "<br/>"
                    + "N’hésitez pas à faire de nouvelles propositions, nous les étudierons avec attention. <b>"
                    + "<br/><br/> Cordialement,<br/>"
                    + "L'équipe " + propositionSelected.getThesoName()
                    + ".<br/> <img src=\"" + getPath() + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

            sendEmail(propositionSelected.getEmail(), subject, contentFile);
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Erreur detectée pendant l'envoie du mail de notification!");
        }
    }

    public void supprimerPropostion(PropositionDao propositionSelected) {

        propositionModificationDetailRepository.deleteById(propositionSelected.getId());
        propositionModificationRepository.deleteById(propositionSelected.getId());
    }

    public void insertProposition(Proposition proposition, PropositionDao propositionSelected,
                                  String commentaireAdmin, boolean prefTermeAccepted, boolean varianteAccepted, boolean traductionAccepted,
                                  boolean noteAccepted, boolean definitionAccepted, boolean changeNoteAccepted, boolean scopeAccepted,
                                  boolean editorialNotesAccepted, boolean examplesAccepted, boolean historyAccepted) throws IOException {

        if (prefTermeAccepted && proposition.isUpdateNomConcept()) {
            Term term = termService.getThisTerm(propositionSelected.getIdConcept(), propositionSelected.getIdTheso(),
                    propositionSelected.getLang());

            term.setLexicalValue(proposition.getNomConceptProp());
            termService.updateTermTraduction(term, currentUser.getNodeUser().getIdUser());

            tree.reset();
            tree.initialise(propositionSelected.getIdTheso(), propositionSelected.getLang());
            roleOnThesoBean.initNodePref(propositionSelected.getIdTheso());
            selectedTheso.setSelectedIdTheso(propositionSelected.getIdTheso());
            selectedTheso.setSelectedLang(propositionSelected.getLang());
            selectedTheso.setSelectedThesaurusForSearch();
            indexSetting.setIsSelectedTheso(true);
        }

        if (varianteAccepted && CollectionUtils.isNotEmpty(proposition.getSynonymsProp())) {
            for (SynonymPropBean synonymPropBean : proposition.getSynonymsProp()) {
                if (synonymPropBean.isToAdd()) {

                    var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(propositionSelected.getIdTheso(), propositionSelected.getIdConcept());
                    var term = Term.builder()
                            .idTerm(preferredTerm.map(PreferredTerm::getIdTerm).orElse(null))
                            .lexicalValue(synonymPropBean.getLexicalValue())
                            .lang(synonymPropBean.getLang().toLowerCase())
                            .idThesaurus(selectedTheso.getCurrentIdTheso())
                            .hidden(synonymPropBean.isHiden())
                            .status(synonymPropBean.isHiden() ? "Hidden" : "USE")
                            .build();
                    log.debug("Enregistrement du synonyme");
                    nonPreferredTermService.addNonPreferredTerm(term, currentUser.getNodeUser().getIdUser());
                } else if (synonymPropBean.isToRemove()) {
                    log.debug("Suppression du non preferred term '{}'", synonymPropBean.getLexicalValue());
                    nonPreferredTermService.deleteNonPreferredTerm(synonymPropBean.getIdTerm(), synonymPropBean.getLang().toLowerCase(),
                            synonymPropBean.getLexicalValue(), selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser());
                } else if (synonymPropBean.isToUpdate()) {
                    if (nonPreferredTermService.updateNonPreferredTerm(synonymPropBean.getOldValue(), synonymPropBean.getLexicalValue(),
                            synonymPropBean.getIdTerm(), synonymPropBean.getLang().toLowerCase(),
                            selectedTheso.getCurrentIdTheso(), synonymPropBean.isHiden(), currentUser.getNodeUser().getIdUser())) {
                        MessageUtils.showErrorMessage("La modification du synonyme a échoué !");
                        return;
                    }
                }
            }
        }

        if (traductionAccepted && CollectionUtils.isNotEmpty(proposition.getTraductionsProp())) {
            for (TraductionPropBean traductionProp : proposition.getTraductionsProp()) {
                if (traductionProp.isToAdd()) {
                    var term = Term.builder()
                            .lexicalValue(traductionProp.getLexicalValue())
                            .idTerm(traductionProp.getIdTerm())
                            .lang(traductionProp.getLang().toLowerCase())
                            .idThesaurus(selectedTheso.getCurrentIdTheso())
                            .source("")
                            .status("")
                            .build();
                    termService.addTermTraduction(term, currentUser.getNodeUser().getIdUser());
                } else if (traductionProp.isToRemove()) {
                    termRepository.deleteByIdTermAndLangAndIdThesaurus(traductionProp.getIdTerm(),
                            traductionProp.getLang(), selectedTheso.getCurrentIdTheso());
                } else if (traductionProp.isToUpdate()) {
                    termService.updateTermTraduction(traductionProp.getLexicalValue(), traductionProp.getIdTerm(),
                            traductionProp.getLang(), selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser());
                }
            }
        }

        if (noteAccepted && proposition.getNote() != null) {
            if (proposition.getNote().isToAdd()) {
                addNewNote(proposition.getNote(), "note");
            } else if (proposition.getNote().isToUpdate()) {
                updateNote(proposition.getNote());
            } else if (proposition.getNote().isToRemove()) {
                deleteNote(proposition.getNote());
            }

        }

        if (changeNoteAccepted && proposition.getChangeNote() != null) {
            if (proposition.getChangeNote().isToAdd()) {
                addNewNote(proposition.getChangeNote(), "changeNote");
            } else if (proposition.getChangeNote().isToUpdate()) {
                updateNote(proposition.getChangeNote());
            } else if (proposition.getChangeNote().isToRemove()) {
                deleteNote(proposition.getChangeNote());
            }
        }

        if (definitionAccepted && proposition.getDefinition() != null) {
            if (proposition.getDefinition().isToAdd()) {
                addNewNote(proposition.getDefinition(), "definition");
            } else if (proposition.getDefinition().isToUpdate()) {
                updateNote(proposition.getDefinition());
            } else if (proposition.getDefinition().isToRemove()) {
                deleteNote(proposition.getDefinition());
            }
        }

        if (editorialNotesAccepted && proposition.getEditorialNote() != null) {
            if (proposition.getEditorialNote().isToAdd()) {
                addNewNote(proposition.getEditorialNote(), "editorialNote");
            } else if (proposition.getEditorialNote().isToUpdate()) {
                updateNote(proposition.getEditorialNote());
            } else if (proposition.getEditorialNote().isToRemove()) {
                deleteNote(proposition.getEditorialNote());
            }
        }

        if (examplesAccepted && proposition.getExample() != null) {
            if (proposition.getExample().isToAdd()) {
                addNewNote(proposition.getExample(), "example");
            } else if (proposition.getExample().isToUpdate()) {
                updateNote(proposition.getExample());
            } else if (proposition.getExample().isToRemove()) {
                deleteNote(proposition.getExample());
            }

        }

        if (scopeAccepted && proposition.getScopeNote() != null) {
            if (proposition.getScopeNote().isToAdd()) {
                addNewNote(proposition.getScopeNote(), "scopeNote");
            } else if (proposition.getScopeNote().isToUpdate()) {
                updateNote(proposition.getScopeNote());
            } else if (proposition.getScopeNote().isToRemove()) {
                deleteNote(proposition.getScopeNote());
            }
        }

        if (historyAccepted && proposition.getHistoryNote() != null) {
            if (proposition.getHistoryNote().isToAdd()) {
                addNewNote(proposition.getHistoryNote(), "historyNote");
            } else if (proposition.getHistoryNote().isToUpdate()) {
                updateNote(proposition.getHistoryNote());
            } else if (proposition.getHistoryNote().isToRemove()) {
                deleteNote(proposition.getHistoryNote());
            }
        }

        conceptService.updateDateOfConcept(propositionSelected.getIdTheso(), propositionSelected.getLang(), currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(propositionSelected.getIdConcept())
                .idThesaurus(propositionSelected.getIdTheso())
                .build());

        var propositionSaved = propositionModificationRepository.findById(propositionSelected.getId());
        if (propositionSaved.isPresent()) {
            propositionSaved.get().setStatus(PropositionStatusEnum.APPROUVER.name());
            propositionSaved.get().setApprouvePar(currentUser.getNodeUser().getName());
            propositionSaved.get().setDate(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()));
            propositionSaved.get().setAdminComment(commentaireAdmin);
            propositionModificationRepository.save(propositionSaved.get());
        }

        conceptView.getConcept(propositionSelected.getIdTheso(), propositionSelected.getIdConcept(), propositionSelected.getLang(), currentUser);

        try {
            String subject = "[Opentheso] Résultat de votre proposition";
            String contentFile = "<html><body>"
                    + "Cher(e) " + propositionSelected.getNom() + ", <br/> "
                    + "<p>Votre proposition d'enrichissement sur le concept " + propositionSelected.getNomConcept()
                    + " du thésaurus " + propositionSelected.getThesoName()
                    + " a été acceptée par les administrateurs.<br/>"
                    + " voici leur message : "
                    + propositionSelected.getAdminComment()
                    + "<br/>"
                    + "N’hésitez pas à faire de nouvelles propositions, nous les étudierons avec attention."
                    + "<br/><br/> Cordialement,<br/>"
                    + "L'équipe " + propositionSelected.getThesoName() + ".<br/> <img src=\"" + getPath()
                    + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

            sendEmail(propositionSelected.getEmail(), subject, contentFile);
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Erreur détectée pendant l'envoie du mail de notification!");
        }
    }

    private void deleteNote(NotePropBean notePropBean) {
        noteService.deleteThisNote(notePropBean.getIdNote(), notePropBean.getIdConcept(), notePropBean.getLang(),
                selectedTheso.getCurrentIdTheso(), notePropBean.getNoteTypeCode(), notePropBean.getLexicalValue(),
                currentUser.getNodeUser().getIdUser());
    }

    private void updateNote(NotePropBean notePropBean) {
        if (!noteService.updateNote(notePropBean.getIdNote(), notePropBean.getIdConcept(), notePropBean.getLang(),
                selectedTheso.getCurrentIdTheso(), notePropBean.getLexicalValue(), notePropBean.getNoteSource(),
                notePropBean.getNoteTypeCode(), currentUser.getNodeUser().getIdUser())) {

            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur !", " Erreur de modification !");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    private void addNewNote(NotePropBean notePropBean, String typeNote) {
        noteService.addNote(conceptView.getNodeConcept().getConcept().getIdConcept(), notePropBean.getLang(),
                selectedTheso.getCurrentIdTheso(), notePropBean.getLexicalValue(), typeNote, "",
                currentUser.getNodeUser().getIdUser());
    }

    public boolean updateNomConcept(String newConceptName) {

        // vérification si le term à ajouter existe déjà, s oui, on a l'Id, sinon, on a Null
        var value = fr.cnrs.opentheso.utils.StringUtils.convertString(newConceptName);
        var termFound = termRepository.findByLexicalValueAndLangAndIdThesaurus(value, selectedTheso.getSelectedLang(), selectedTheso.getCurrentIdTheso());
        if (termFound.isPresent()) {
            var term = termRepository.findByIdTermAndIdThesaurusAndLang(termFound.get().getIdTerm(),
                    selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang());
            var label = term.isPresent() ? term.get().getLexicalValue() : "";
            MessageUtils.showWarnMessage("Le label '" + label + "' existe déjà !");
            return false;
        }

        return true;
    }

    public List<fr.cnrs.opentheso.entites.Proposition> getPropositionByConceptAndThesaurus(String idThesaurus, String idConcept) {

        log.debug("Recherche des propositions pour le concept id {} ({})", idConcept, idThesaurus);
        var propositions = propositionRepository.findAllByIdConceptAndIdThesaurusOrderByCreated(idThesaurus, idConcept);
        if (CollectionUtils.isEmpty(propositions)) {
            log.debug("Aucune proposition n'est trouvée pour le concept id {}", idConcept);
            return List.of();
        }
        return propositions;
    }

    public Proposition selectProposition(NodeConcept nodeConcept) {
        Proposition proposition = new Proposition();
        proposition.setConceptID(nodeConcept.getConcept().getIdConcept());
        proposition.setUpdateNomConcept(false);
        proposition.setNomConceptProp(null);
        proposition.setNomConcept(nodeConcept.getTerm());
        proposition.setSynonymsProp(toSynonymPropBean(nodeConcept.getNodeEM(),
                conceptView.getNodeConcept().getTerm().getIdTerm()));
        proposition.setTraductionsProp(toTraductionPropBeans(nodeConcept.getNodeTermTraductions(),
                nodeConcept.getTerm().getIdTerm()));

        for (NodeNote nodeNote : nodeConcept.getNodeNotes()) {
            switch (nodeNote.getNoteTypeCode()) {
                case "note":
                    proposition.setNote(toNotePropBean(nodeNote));
                    break;
                case "scopeNote":
                    proposition.setScopeNote(toNotePropBean(nodeNote));
                    break;
                case "changeNote":
                    proposition.setChangeNote(toNotePropBean(nodeNote));
                    break;
                case "definition":
                    proposition.setDefinition(toNotePropBean(nodeNote));
                    break;
                case "editorialNote":
                    proposition.setEditorialNote(toNotePropBean(nodeNote));
                    break;
                case "example":
                    proposition.setExample(toNotePropBean(nodeNote));
                    break;
                case "historyNote":
                    proposition.setHistoryNote(toNotePropBean(nodeNote));
                    break;
            }
        }

        return proposition;
    }

    private NotePropBean toNotePropBean(NodeNote nodeNote) {
        NotePropBean notePropBean = new NotePropBean();
        notePropBean.setModified(nodeNote.getModified());
        notePropBean.setCreated(nodeNote.getCreated());
        notePropBean.setIdUser(nodeNote.getIdUser());
        notePropBean.setIdConcept(nodeNote.getIdConcept());
        notePropBean.setIdNote(nodeNote.getIdNote());
        notePropBean.setIdTerm(nodeNote.getIdTerm());
        notePropBean.setLang(nodeNote.getLang());
        notePropBean.setLexicalValue(nodeNote.getLexicalValue());
        notePropBean.setOldValue(nodeNote.getLexicalValue());
        notePropBean.setNoteTypeCode(nodeNote.getNoteTypeCode());
        notePropBean.setUser(nodeNote.getUser());
        return notePropBean;
    }

    public void preparerPropositionSelect(Proposition proposition, PropositionDao propositionDao) {

        var propositionDetails = propositionModificationDetailRepository.findAllByIdProposition(propositionDao.getId());

        proposition.setConceptID(conceptView.getNodeConcept().getConcept().getIdConcept());
        proposition.setNomConceptProp(null);
        proposition.setNomConcept(conceptView.getNodeConcept().getTerm());

        proposition.setSynonymsProp(toSynonymPropBean(conceptView.getNodeConcept().getNodeEM(),
                conceptView.getNodeConcept().getTerm().getIdTerm()));

        proposition.setTraductionsProp(toTraductionPropBeans(conceptView.getNodeConcept().getNodeTermTraductions(),
                conceptView.getNodeConcept().getTerm().getIdTerm()));

        for (NodeNote nodeNote : conceptView.getNodeConcept().getNodeNotes()) {
            switch (nodeNote.getNoteTypeCode()) {
                case "note":
                    proposition.setNote(toNotePropBean(nodeNote));
                    break;
                case "scopeNote":
                    proposition.setScopeNote(toNotePropBean(nodeNote));
                    break;
                case "changeNote":
                    proposition.setChangeNote(toNotePropBean(nodeNote));
                    break;
                case "definition":
                    proposition.setDefinition(toNotePropBean(nodeNote));
                    break;
                case "editorialNote":
                    proposition.setEditorialNote(toNotePropBean(nodeNote));
                    break;
                case "example":
                    proposition.setExample(toNotePropBean(nodeNote));
                    break;
                case "historyNote":
                    proposition.setHistoryNote(toNotePropBean(nodeNote));
                    break;
            }
        }

        for (var propositionDetailDao : propositionDetails) {
            if (PropositionCategoryEnum.NOM.name().equals(propositionDetailDao.getCategorie())) {
                proposition.setUpdateNomConcept(true);
                proposition.setNomConceptProp(propositionDetailDao.getValue());
            } else if (PropositionCategoryEnum.SYNONYME.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    SynonymPropBean propAddSyn = new SynonymPropBean();
                    propAddSyn.setLexicalValue(propositionDetailDao.getValue());
                    propAddSyn.setToAdd(true);
                    propAddSyn.setIdUser(currentUser.getNodeUser().getIdUser() + "");
                    propAddSyn.setLang(propositionDetailDao.getLang());
                    propAddSyn.setStatus(propositionDetailDao.getStatus());
                    propAddSyn.setIdTerm(propositionDetailDao.getIdTerm());
                    propAddSyn.setHiden(propositionDetailDao.isHiden());
                    proposition.getSynonymsProp().add(propAddSyn);
                }

                for (int i = 0; i < proposition.getSynonymsProp().size(); i++) {
                    if (proposition.getSynonymsProp().get(i).getLexicalValue().equals(propositionDetailDao.getValue())) {
                        if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                            proposition.getSynonymsProp().get(i).setToRemove(true);
                        } else if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                            proposition.getSynonymsProp().get(i).setToAdd(true);
                        } else {
                            proposition.getSynonymsProp().get(i).setToUpdate(false);
                            proposition.getSynonymsProp().get(i).setToRemove(false);
                            proposition.getSynonymsProp().get(i).setToAdd(false);
                        }
                    } else if (proposition.getSynonymsProp().get(i).getLexicalValue().equals(propositionDetailDao.getOldValue())
                            && proposition.getSynonymsProp().get(i).getLang().equals(propositionDetailDao.getLang())) {
                        proposition.getSynonymsProp().get(i).setToUpdate(true);
                        proposition.getSynonymsProp().get(i).setLexicalValue(propositionDetailDao.getValue());
                    }
                }
            } else if (PropositionCategoryEnum.TRADUCTION.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    TraductionPropBean traductionPropBean = new TraductionPropBean();
                    traductionPropBean.setLexicalValue(propositionDetailDao.getValue());
                    traductionPropBean.setToAdd(true);
                    traductionPropBean.setLang(propositionDetailDao.getLang());
                    traductionPropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.getTraductionsProp().add(traductionPropBean);
                }

                for (int i = 0; i < proposition.getTraductionsProp().size(); i++) {
                    if (proposition.getTraductionsProp().get(i).getOldValue() != null
                            && proposition.getTraductionsProp().get(i).getLang().equals(propositionDetailDao.getLang())) {
                        if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                            proposition.getTraductionsProp().get(i).setToUpdate(true);
                            proposition.getTraductionsProp().get(i).setLexicalValue(propositionDetailDao.getValue());
                        } else {
                            proposition.getTraductionsProp().get(i).setToRemove(true);
                        }
                    }
                }
            } else if (PropositionCategoryEnum.NOTE.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setNote(notePropBean);
                }

                if (proposition.getNote().getOldValue() != null
                        && proposition.getNote().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getNote().setToUpdate(true);
                        proposition.getNote().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getNote().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.CHANGE_NOTE.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setChangeNote(notePropBean);
                }

                if (proposition.getChangeNote().getOldValue() != null
                        && proposition.getChangeNote().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getChangeNote().setToUpdate(true);
                        proposition.getChangeNote().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getChangeNote().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.DEFINITION.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setDefinition(notePropBean);
                }

                if (proposition.getDefinition().getOldValue() != null
                        && proposition.getDefinition().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getDefinition().setToUpdate(true);
                        proposition.getDefinition().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getDefinition().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.EDITORIAL_NOTE.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setEditorialNote(notePropBean);
                }

                if (proposition.getEditorialNote().getOldValue() != null
                        && proposition.getEditorialNote().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getEditorialNote().setToUpdate(true);
                        proposition.getEditorialNote().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getEditorialNote().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.HISTORY.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setHistoryNote(notePropBean);
                }

                if (proposition.getHistoryNote().getOldValue() != null
                        && proposition.getHistoryNote().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getHistoryNote().setToUpdate(true);
                        proposition.getHistoryNote().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getHistoryNote().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.SCOPE.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setScopeNote(notePropBean);
                }

                if (proposition.getScopeNote().getOldValue() != null
                        && proposition.getScopeNote().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getScopeNote().setToUpdate(true);
                        proposition.getScopeNote().setLexicalValue(propositionDetailDao.getValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getScopeNote().setToRemove(true);
                    }
                }

            } else if (PropositionCategoryEnum.EXAMPLE.name().equals(propositionDetailDao.getCategorie())) {

                if (PropositionActionEnum.ADD.name().equals(propositionDetailDao.getAction())) {
                    NotePropBean notePropBean = new NotePropBean();
                    notePropBean.setLexicalValue(propositionDetailDao.getValue());
                    notePropBean.setToAdd(true);
                    notePropBean.setLang(propositionDetailDao.getLang());
                    notePropBean.setIdTerm(propositionDetailDao.getIdTerm());
                    proposition.setExample(notePropBean);
                }

                if (proposition.getExample().getOldValue() != null
                        && proposition.getExample().getOldValue().equals(propositionDetailDao.getOldValue())) {
                    if (PropositionActionEnum.UPDATE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getExample().setToUpdate(true);
                        proposition.getExample().setLexicalValue(propositionDetailDao.getValue());
                        proposition.getExample().setOldValue(propositionDetailDao.getOldValue());
                    } else if (PropositionActionEnum.DELETE.name().equals(propositionDetailDao.getAction())) {
                        proposition.getExample().setToRemove(true);
                    }
                }

            }
        }

        if (propositionDao.getStatus().equals(PropositionStatusEnum.ENVOYER.name())) {
            propositionModificationRepository.updateStatus(propositionDao.getId(), PropositionStatusEnum.LU.name());
        }
    }

    private List<TraductionPropBean> toTraductionPropBeans(List<NodeTermTraduction> nodesTermTraductions, String idTerm) {
        List<TraductionPropBean> traduction = new ArrayList<>();
        for (NodeTermTraduction nodeTermTraduction : nodesTermTraductions) {
            TraductionPropBean traductionPropBean = new TraductionPropBean();
            traductionPropBean.setLang(nodeTermTraduction.getLang());
            traductionPropBean.setLexicalValue(nodeTermTraduction.getLexicalValue());
            traductionPropBean.setOldValue(nodeTermTraduction.getLexicalValue());
            traductionPropBean.setIdTerm(idTerm);
            traduction.add(traductionPropBean);
        }
        return traduction;
    }

    public List<SynonymPropBean> toSynonymPropBean(List<NodeEM> nodesEm, String idTerm) {
        List<SynonymPropBean> synonyms = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(nodesEm)) {
            for (NodeEM nodeEM : nodesEm) {
                SynonymPropBean synonymPropBean = new SynonymPropBean();
                synonymPropBean.setAction(nodeEM.getAction());
                synonymPropBean.setCreated(nodeEM.getCreated());
                synonymPropBean.setModified(nodeEM.getModified());
                synonymPropBean.setHiden(nodeEM.isHiden());
                synonymPropBean.setOldHiden(nodeEM.isHiden());
                synonymPropBean.setLang(nodeEM.getLang());
                synonymPropBean.setLexicalValue(nodeEM.getLexicalValue());
                synonymPropBean.setOldValue(nodeEM.getLexicalValue());
                synonymPropBean.setSource(nodeEM.getSource());
                synonymPropBean.setStatus(nodeEM.getStatus());
                synonymPropBean.setIdTerm(idTerm);
                synonyms.add(synonymPropBean);
            }
        }

        return synonyms;
    }

    public List<PropositionDao> searchAllPropositions(String idTheso) {
        List<PropositionDao> propositions = new ArrayList<>();
        propositions.addAll(searchPropositionsNonTraitee(idTheso));
        propositions.addAll(getAllPropositionByStatus(PropositionStatusEnum.APPROUVER.name(), idTheso));
        propositions.addAll(getAllPropositionByStatus(PropositionStatusEnum.REFUSER.name(), idTheso));
        return propositions;
    }

    public List<PropositionDao> searchPropositionsNonTraitee(String idTheso) {
        List<PropositionDao> propositions = new ArrayList<>();
        propositions.addAll(getAllPropositionByStatus(PropositionStatusEnum.ENVOYER.name(), idTheso));
        propositions.addAll(getAllPropositionByStatus(PropositionStatusEnum.LU.name(), idTheso));
        return propositions;
    }

    private List<PropositionDao> getAllPropositionByStatus(String status, String idTheso) {
        return propositionModificationRepository
                .findAllPropositionsByStatusAndTheso(status, idTheso)
                .stream()
                .map(this::mapToDao)
                .toList();
    }

    private PropositionDao mapToDao(PropositionProjection projection) {
        PropositionDao dao = new PropositionDao();
        dao.setId(projection.getId());
        dao.setIdConcept(projection.getIdConcept());
        dao.setLang(projection.getLang());
        dao.setIdTheso(projection.getIdTheso());
        dao.setStatus(projection.getStatus());
        dao.setDatePublication(projection.getDate());
        dao.setNom(projection.getNom());
        dao.setEmail(projection.getEmail());
        dao.setCommentaire(projection.getCommentaire());
        dao.setUserAction(projection.getApprouvePar());
        dao.setDateUpdate(instantToString(projection.getApprouveDate()));
        dao.setAdminComment(projection.getAdminComment());
        dao.setNomConcept(projection.getLexicalValue());
        dao.setCodeDrapeau(projection.getCodePays());
        return dao;
    }
    private String instantToString(Instant instant) {
        return instant != null ? FORMATTER.format(instant) : null;
    }

    public void createProposition(PropositionFromApi proposition, User user) {

        var thesaurusLang = preferenceService.getWorkLanguageOfThesaurus(proposition.getIdTheso());

        int propositionId = propositionModificationRepository.save(PropositionModification.builder()
                        .nom(user.getUsername())
                        .email(user.getMail())
                        .commentaire(proposition.getCommentaire())
                        .date(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()))
                        .idConcept(proposition.getConceptID())
                        .idTheso(proposition.getIdTheso())
                        .lang(thesaurusLang)
                        .status(PropositionStatusEnum.ENVOYER.name())
                        .build())
                .getId();

        saveTerme(proposition, propositionId, thesaurusLang);

        if (CollectionUtils.isNotEmpty(proposition.getTraductionsProp())) {
            saveTraductions(proposition, propositionId, thesaurusLang);
        }

        if (!CollectionUtils.isEmpty(proposition.getSynonymsProp())) {
            saveSynonymes(proposition, propositionId);
        }

        if (CollectionUtils.isNotEmpty(proposition.getNotes())) {
            for (NotePropBean note : proposition.getNotes()) {
                noteManagement(propositionId, note, PropositionCategoryEnum.NOTE.name());
            }
        }

        if (CollectionUtils.isNotEmpty(proposition.getDefinitions())) {
            for (NotePropBean definition : proposition.getDefinitions()) {
                noteManagement(propositionId, definition, PropositionCategoryEnum.DEFINITION.name());
            }
        }
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour du thésaurus id pour les propositions présentes dans le thésaurus id {}", oldIdThesaurus);
        propositionRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        propositionModificationRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public void deleteByThesaurus(String idThesaurus) {

        log.debug("Suppression des propositions présentes dans le thésaurus id {}", idThesaurus);
        propositionModificationDetailRepository.deleteByIdThesaurus(idThesaurus);
        propositionModificationRepository.deleteAllByIdTheso(idThesaurus);
        propositionRepository.deleteAllByIdThesaurus(idThesaurus);
    }

    private void saveTerme(PropositionFromApi proposition,  Integer propositionId, String thesaurusLang) {
        var prefLabel = proposition.getTraductionsProp().stream()
                .filter(element -> element.getLang().equals(thesaurusLang))
                .findFirst();
        prefLabel.ifPresent(traductionPropBean -> propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                .idProposition(propositionId)
                .action(PropositionActionEnum.UPDATE.name())
                .categorie(PropositionCategoryEnum.NOM.name())
                .lang(traductionPropBean.getLang())
                .value(traductionPropBean.getLexicalValue())
                .oldValue(traductionPropBean.getOldValue())
                .build()));
    }

    private void saveTraductions(PropositionFromApi proposition, Integer propositionId,  String thesaurusLang) {

        var traduction = proposition.getTraductionsProp().stream().filter(element -> !element.getLang().equals(thesaurusLang)).findFirst();

        if (traduction.isPresent()) {
            if (traduction.get().isToAdd() || traduction.get().isToUpdate() || traduction.get().isToRemove()) {
                var propositionDetail = new PropositionModificationDetail();
                if (traduction.get().isToAdd()) {
                    propositionDetail.setAction(PropositionActionEnum.ADD.name());
                    propositionDetail.setOldValue(traduction.get().getLexicalValue());
                } else if (traduction.get().isToRemove()) {
                    propositionDetail.setAction(PropositionActionEnum.DELETE.name());
                    propositionDetail.setOldValue(traduction.get().getOldValue());
                } else {
                    propositionDetail.setAction(PropositionActionEnum.UPDATE.name());
                    propositionDetail.setOldValue(traduction.get().getOldValue());
                }

                var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(proposition.getIdTheso(), proposition.getConceptID());
                if (preferredTerm.isPresent()) {
                    propositionDetail.setIdProposition(propositionId);
                    propositionDetail.setCategorie(PropositionCategoryEnum.TRADUCTION.name());
                    propositionDetail.setLang(traduction.get().getLang());
                    propositionDetail.setValue(traduction.get().getLexicalValue());
                    propositionDetail.setIdTerm(preferredTerm.get().getIdTerm());
                    propositionModificationDetailRepository.save(propositionDetail);
                }
            }
        }
    }

    private void saveSynonymes(PropositionFromApi proposition, Integer propositionId) {
        for (SynonymPropBean synonymProp : proposition.getSynonymsProp()) {
            saveSynonyms(propositionId, synonymProp);
        }
    }

    private void noteManagement(int propositionID, NotePropBean note, String category) {
        if (note.isToAdd() || note.isToUpdate() || note.isToRemove()) {
            var propositionDetail = new PropositionModificationDetail();
            if (note.isToAdd()) {
                propositionDetail.setAction(PropositionActionEnum.ADD.name());
                propositionDetail.setOldValue(note.getLexicalValue());
            } else if (note.isToRemove()) {
                propositionDetail.setAction(PropositionActionEnum.DELETE.name());
                propositionDetail.setOldValue(note.getOldValue());
            } else {
                propositionDetail.setAction(PropositionActionEnum.UPDATE.name());
                propositionDetail.setOldValue(note.getOldValue());
            }

            propositionDetail.setIdProposition(propositionID);
            propositionDetail.setCategorie(category);
            propositionDetail.setLang(note.getLang());
            propositionDetail.setValue(note.getLexicalValue());
            propositionDetail.setIdTerm(note.getIdTerm());
            propositionModificationDetailRepository.save(propositionDetail);
        }
    }

}
