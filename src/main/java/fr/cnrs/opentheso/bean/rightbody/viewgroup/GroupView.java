package fr.cnrs.opentheso.bean.rightbody.viewgroup;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.entites.ConceptGroupType;
import fr.cnrs.opentheso.repositories.ConceptStatusRepository;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ViewEditorHomeBean;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ViewEditorThesaurusHomeBean;
import fr.cnrs.opentheso.services.*;

import fr.cnrs.opentheso.stats.services.StatEventService;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;


@Getter
@Setter
@Slf4j
@SessionScoped
@RequiredArgsConstructor
@Named(value = "groupView")
public class GroupView implements Serializable {

    private final NoteService noteService;
    private final IndexSetting indexSetting;
    private final GroupService groupService;
    private final SelectedTheso selectedTheso;
    private final GroupTypeService groupTypeService;
    private final IpAddressService ipAddressService;
    private final ViewEditorHomeBean viewEditorHomeBean;
    private final ConceptStatusRepository conceptStatusRepository;
    private final ViewEditorThesaurusHomeBean viewEditorThesaurusHomeBean;
    private final StatEventService statEventService;

    private NodeGroup nodeGroup;
    private List<NodeGroupTraductions> nodeGroupTraductions;
    private ConceptGroupType nodeGroupType;
    
    private NodeNote note;
    private NodeNote scopeNote;
    private NodeNote changeNote;
    private NodeNote definition;
    private NodeNote editorialNote;
    private NodeNote example;
    private NodeNote historyNote;

    /// Notes du concept pour l'affichage du multilingue
    private List<NodeNote> noteAllLang;
    private List<NodeNote> scopeNoteAllLang;
    private List<NodeNote> changeNoteAllLang;
    private List<NodeNote> definitionAllLang;
    private List<NodeNote> editorialNoteAllLang;
    private List<NodeNote> exampleAllLang;
    private List<NodeNote> historyNoteAllLang;
    
    private int count;
    private boolean toggleSwitchNotesLang = true;


    public void init() {
        count = 0;
        nodeGroup = null;
        nodeGroupType = null;
        nodeGroupTraductions = new ArrayList<>();
    }

    public void clear(){
        nodeGroupTraductions = new ArrayList<>();
        nodeGroup = null;
        nodeGroupType = null;
    }

    /**
     * récuparation des informations pour le concept sélectionné
     */
    public void getGroup(String idTheso, String idGroup, String idLang) {

        nodeGroup = groupService.getThisConceptGroup(idGroup, idTheso, idLang);
        
        nodeGroupTraductions = groupService.getGroupTraduction(idGroup, idTheso, idLang);
        nodeGroupType = groupTypeService.getGroupType(nodeGroup.getConceptGroup().getIdTypeCode());
        logGroup();
        setNotes(idTheso, idGroup, idLang);

        count = conceptStatusRepository.countConceptsInGroup(idTheso, idGroup);
        indexSetting.setIsValueSelected(true);
        viewEditorHomeBean.reset();
        viewEditorThesaurusHomeBean.reset();
    }

    private void logGroup() {
        String ipAddress = ipAddressService.getClientIpAddress();
        String label = (nodeGroup.getLexicalValue() == null
                ? "(" + nodeGroup.getConceptGroup().getIdGroup() + ")"
                : nodeGroup.getLexicalValue());
        String lang = (nodeGroup.getLexicalValue() == null
                ? selectedTheso.getCurrentLang()
                : nodeGroup.getIdLang());

        log.info("Group: {}, Langue: {}, identifier: {}, Thesaurus: {}, Idt: {}, IP: {}",
                label,
                lang,
                nodeGroup.getConceptGroup().getIdGroup(),
                selectedTheso.getThesoName(),
                selectedTheso.getCurrentIdTheso(),
                ipAddress);

        statEventService.logCollectionView(
                nodeGroup.getConceptGroup().getIdGroup(),
                label,
                lang,
                selectedTheso.getThesoName(),
                selectedTheso.getCurrentIdTheso());
    }

    public void setNotes(String idThesaurus, String idGroup, String idLang) {

        if (toggleSwitchNotesLang) {
            setNotesForAllLang(noteService.getListNotesAllLang(idGroup, idThesaurus));
        } else {
            setAllNotes(noteService.getListNotes(idGroup, idThesaurus, idLang));
        }
    }
    
    /////////////////////////////////
    /////////////////////////////////
    // fonctions pour les notes /////    
    /////////////////////////////////
    /////////////////////////////////
    private void setAllNotes(List<NodeNote> nodeNotes) {
        clearNotes();
        for (NodeNote nodeNote : nodeNotes) {
            switch (nodeNote.getNoteTypeCode()) {
                case "note":
                    note  = nodeNote;
                    break;
                case "scopeNote":
                    scopeNote = nodeNote;
                    break;
                case "changeNote":
                    changeNote = nodeNote;
                    break;
                case "definition":
                    definition = nodeNote;
                    break;
                case "editorialNote":
                    editorialNote = nodeNote;
                    break;
                case "example":
                    example = nodeNote;
                    break;
                case "historyNote":
                    historyNote = nodeNote;
                    break;
            }
        }
    }

    private void setNotesForAllLang(List<NodeNote> nodeNotes) {
        clearNotesAllLang();
        clearNotes();

        if (CollectionUtils.isNotEmpty(nodeNotes)) {
            // note
            nodeNotes.stream()
                    .filter(note1 -> "note".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(noteAllLang::add);

            // scopeNote
            nodeNotes.stream()
                    .filter(note1 -> "scopeNote".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(scopeNoteAllLang::add);

            // changeNote
            nodeNotes.stream()
                    .filter(note1 -> "changeNote".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(changeNoteAllLang::add);

            // definition
            nodeNotes.stream()
                    .filter(note1 -> "definition".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(definitionAllLang::add);

            // editorialNote
            nodeNotes.stream()
                    .filter(note1 -> "editorialNote".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(editorialNoteAllLang::add);

            // example
            nodeNotes.stream()
                    .filter(note1 -> "example".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(exampleAllLang::add);

            // historyNote
            nodeNotes.stream()
                    .filter(note1 -> "historyNote".equals(note1.getNoteTypeCode()))
                    .map(note1 -> {
                        NodeNote nodeNote = new NodeNote();
                        nodeNote.setIdNote(note1.getIdNote());
                        nodeNote.setIdConcept(note1.getIdentifier());
                        nodeNote.setNoteTypeCode(note1.getNoteTypeCode());
                        nodeNote.setLexicalValue(note1.getLexicalValue());
                        nodeNote.setLang(note1.getLang());
                        nodeNote.setNoteSource(note1.getNoteSource());
                        return nodeNote;
                    })
                    .forEach(historyNoteAllLang::add);
        }

    }
    private void clearNotesAllLang() {
        noteAllLang = new ArrayList<>();
        scopeNoteAllLang = new ArrayList<>();
        changeNoteAllLang = new ArrayList<>();
        definitionAllLang = new ArrayList<>();
        editorialNoteAllLang = new ArrayList<>();
        exampleAllLang = new ArrayList<>();
        historyNoteAllLang = new ArrayList<>();
    }

    private void clearNotes() {
        note = null;
        scopeNote = null;
        changeNote = null;
        definition = null;
        editorialNote = null;
        example = null;
        historyNote = null;
    }
}
