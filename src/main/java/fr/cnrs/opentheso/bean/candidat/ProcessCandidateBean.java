package fr.cnrs.opentheso.bean.candidat;

import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.services.MailService;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.services.ArkService;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.HandleConceptService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.exports.csv.CsvWriteHelper;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "processCandidateBean")
public class ProcessCandidateBean implements Serializable {

    private final Tree tree;
    private final RoleOnThesaurusBean roleOnThesaurus;
    private final CandidatBean candidatBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;

    private final MailService mailBean;
    private final ConceptService conceptService;
    private final CandidatService candidatService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final CsvWriteHelper csvWriteHelper;
    private final ConceptAddService conceptAddService;
    private final HandleConceptService handleConceptService;
    private final ArkService arkService;
    private final UserService userService;

    private CandidatDto selectedCandidate;
    private String adminMessage;


    public void reset(CandidatDto candidatSelected) {
        this.selectedCandidate = candidatSelected;
        adminMessage = null;
    }

    public StreamedContent exportProcessedCandidates(List<CandidatDto> candidatDtos) {

        var datas = csvWriteHelper.writeProcessedCandidates(candidatDtos, ';');
        if (datas == null) {
            return null;
        }

        PrimeFaces.current().executeScript("PF('waitDialog').hide();");

        try ( ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name(selectedTheso.getThesoName() + "_candidats" + ".csv")
                    .stream(() -> input)
                    .build();
        } catch (IOException ignored) {
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        return new DefaultStreamedContent();
    }
    
    
    public void insertCandidat() throws IOException {
        if (selectedCandidate == null) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        if (candidatService.insertCandidate(selectedCandidate, adminMessage, currentUser.getNodeUser().getIdUser())) {
            MessageUtils.showErrorMessage("Erreur d'insertion");
            return;
        }
        // envoie de mail au créateur du candidat si l'option mail est activée
        var nodeUser = userService.getUser(selectedCandidate.getCreatedById());

        if(nodeUser != null && nodeUser.isAlertMail()) {
            // provoque une erreur d'Objets NULL
            //new Thread(() -> sendMailCandidateAccepted(nodeUser.getMail(), selectedCandidate)).start();
            sendMailCandidateAccepted(nodeUser.getMail(), selectedCandidate);
        }
        
        generateArk(roleOnThesaurus.getNodePreference(), selectedCandidate);

        conceptService.updateDateOfConcept(selectedCandidate.getIdThesaurus(), selectedCandidate.getIdConcepte(),
                currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(selectedCandidate.getIdConcepte())
                .idThesaurus(selectedCandidate.getIdThesaurus())
                .build());

    //    MessageUtils.showInformationMessage("Candidat inséré avec succès");

        reset(null);

        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
        candidatBean.initCandidatModule();

        tree.loadConceptTree();

        PrimeFaces.current().ajax().update("containerIndex:tabViewCandidat");
        MessageUtils.showInformationMessage("Candidat inséré avec succès");
    }
    
    private void generateArk(Preferences nodePreference, CandidatDto selectedCandidateTemp){
        if (nodePreference != null) {
            // création de l'identifiant Handle
            if (nodePreference.isUseHandle()) {
                if (!handleConceptService.generateIdHandle(selectedCandidateTemp.getIdConcepte(), selectedCandidateTemp.getIdThesaurus())) {
                    MessageUtils.showErrorMessage("La création Handle a échouée");
                    log.error("La création Handle a échoué");
                }
            }     
            // serveur Ark
            if (nodePreference.isUseArk()) {
                var result = conceptAddService.generateArkId(selectedCandidateTemp.getIdThesaurus(), List.of(selectedCandidateTemp.getIdConcepte()),
                        selectedCandidateTemp.getLang(), null);
                if (CollectionUtils.isEmpty(result)) {
                    log.error("La création Ark a échoué");
                }
            }
            // ark Local
            if (nodePreference.isUseArkLocal()) {
                List<String> idConcepts = new ArrayList<>();
                idConcepts.add(selectedCandidateTemp.getIdConcepte());
                if (!arkService.generateArkIdLocal(selectedCandidateTemp.getIdThesaurus(), idConcepts)) {
                    MessageUtils.showErrorMessage("La création du Ark local a échoué");
                    log.error("La création du Ark local a échoué");
                }
            }                
        }          
    }
    
    public void rejectCandidat() throws IOException {
        if (selectedCandidate == null) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        if (candidatService.rejectCandidate(selectedCandidate, adminMessage, currentUser.getNodeUser().getIdUser())) {
            MessageUtils.showErrorMessage("Erreur d'insertion");
            return;
        }

        // envoie de mail au créateur du candidat si l'option mail est activée
        var nodeUser = userService.getUser(selectedCandidate.getCreatedById());
        if(nodeUser.isAlertMail()) sendMailCandidateRejected(nodeUser.getMail(), selectedCandidate);
        
        MessageUtils.showInformationMessage("Candidat(s) rejeté(s) avec succès");
        reset(null);

        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
        candidatBean.initCandidatModule();
        PrimeFaces.current().ajax().update("containerIndex:tabViewCandidat");
    }

    public void insertListCandidat(int idUser, Preferences nodePreference) throws IOException {
        if (candidatBean.getSelectedCandidates() == null || candidatBean.getSelectedCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }
        
        // envoie de mail au créateur du candidat si l'option mail est activée
        for (CandidatDto selectedCandidate1 : candidatBean.getSelectedCandidates()) {
            selectedCandidate1 = candidatBean.getAllInfosOfCandidate(selectedCandidate1);
            if (candidatService.insertCandidate(selectedCandidate1, adminMessage, idUser)) {
                MessageUtils.showErrorMessage("Erreur d'insertion pour le candidat : " + selectedCandidate1.getNomPref() + "(" + selectedCandidate1.getIdConcepte() + ")");
                return;
            }

            conceptService.updateDateOfConcept(selectedCandidate1.getIdThesaurus(), selectedCandidate1.getIdConcepte(), idUser);

            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(currentUser.getNodeUser().getName())
                    .idConcept(selectedCandidate1.getIdConcepte())
                    .idThesaurus(selectedCandidate1.getIdThesaurus())
                    .build());
            
            generateArk(nodePreference, selectedCandidate1);
            var nodeUser = userService.getUser(selectedCandidate1.getCreatedById());
            if(nodeUser != null && nodeUser.isAlertMail()) sendMailCandidateAccepted(nodeUser.getMail(), selectedCandidate1);
        }

        MessageUtils.showInformationMessage("Candidats insérés avec succès");
        reset(null);
        candidatBean.initCandidatModule();
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
    }     
    
    public void rejectCandidatList() throws IOException {
        if (candidatBean.getSelectedCandidates() == null || candidatBean.getSelectedCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        for (CandidatDto selectedCandidate1 : candidatBean.getSelectedCandidates()) {
            selectedCandidate1 = candidatBean.getAllInfosOfCandidate(selectedCandidate1);
            if (candidatService.rejectCandidate(selectedCandidate1, adminMessage, currentUser.getNodeUser().getIdUser())) {
                MessageUtils.showErrorMessage("Erreur pour le candidat : " + selectedCandidate1.getNomPref() + "(" + selectedCandidate1.getIdConcepte() + ")");
                return;
            }
            var nodeUser = userService.getUser(selectedCandidate1.getCreatedById());
            if(nodeUser.isAlertMail()) sendMailCandidateRejected(nodeUser.getMail(), selectedCandidate1);
            conceptService.updateDateOfConcept(selectedCandidate1.getIdThesaurus(), selectedCandidate1.getIdConcepte(),
                    currentUser.getNodeUser().getIdUser());

            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(currentUser.getNodeUser().getName())
                    .idConcept(selectedCandidate1.getIdConcepte())
                    .idThesaurus(selectedCandidate1.getIdThesaurus())
                    .build());
        }

        MessageUtils.showInformationMessage("Candidats insérés avec succès");

        reset(null);

        candidatBean.initCandidatModule();
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
    }    
    
    private void sendMailCandidateAccepted(String mail, CandidatDto candidat) {
        if(adminMessage == null) adminMessage = "";
        var subject = "[" + selectedTheso.getThesoName() + "] Confirmation de l'acceptation de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été accepté par nos administrateurs, il est désormais intégré au thésaurus " + selectedTheso.getThesoName() + "<br/></p>"
                + "Nous vous remercions de votre contribution à l'enrichissement du thésaurus <b>" + selectedTheso.getThesoName() + "</b> "
                + "(concept : <a href=\"" + getPath() + "/?idc=" + candidat.getIdConcepte()
                + "&idt=" + candidat.getIdThesaurus() + "\">" + candidat.getNomPref() + "</a>). "
                + "</b></b>"
                + "Message de l'administrateur : " + "</b>"
                + adminMessage
                + "</b></b>"
                + "<br/><br/> Cordialement,<br/>"
                + "L'équipe " + selectedTheso.getThesoName() + ".<br/> <img src=\"" + getPath() + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

        if(mailBean.sendMail(mail, subject, contentFile)) {
            MessageUtils.showErrorMessage("!! votre propostion n'a pas été envoyée !!");
        }
    }
    
    private boolean sendMailCandidateRejected(String mail, CandidatDto candidat) {
        if(adminMessage == null) adminMessage = "";
        var subject = "[" + selectedTheso.getThesoName() + "] Refus de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été refusé par nos administrateurs, il n'a pas été intégré au thésaurus " + selectedTheso.getThesoName() + "<br/></p>"
                + "Nous vous remercions de votre contribution à l'enrichissement du thésaurus <b>" + selectedTheso.getThesoName() + "</b> "
                + "</b></b>"
                + "Message de l'administrateur : " + "</b>"
                + adminMessage
                + "</b></b>"
                + "L'équipe " + selectedTheso.getThesoName() + ".<br/> <img src=\"" + getPath() + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

        if(!mailBean.sendMail(mail, subject, contentFile)) {
            MessageUtils.showErrorMessage("!! votre propostion n'a pas été envoyée !!");
            return false;
        }
        return true;
    }
    
    private String getPath(){
        if(FacesContext.getCurrentInstance() == null) {
            return "";
        }
        var path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

}
