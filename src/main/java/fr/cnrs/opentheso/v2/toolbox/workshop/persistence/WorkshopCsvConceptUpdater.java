package fr.cnrs.opentheso.v2.toolbox.workshop.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.NoteHistorique;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.NonPreferredTermHistorique;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.TermHistorique;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeGps;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.HierarchicalRelationshipHistorique;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusCsvGpsParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkshopCsvConceptUpdater {

    private final PreferredTermRepository preferredTermRepository;
    private final TermRepository termRepository;
    private final TermHistoriqueRepository termHistoriqueRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;
    private final NoteRepository noteRepository;
    private final NoteHistoriqueRepository noteHistoriqueRepository;
    private final AlignementRepository alignementRepository;
    private final AlignementTypeRepository alignementTypeRepository;
    private final AlignementSourceRepository alignementSourceRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final GpsRepository gpsRepository;
    private final ImagesRepository imagesRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ConceptRepository conceptRepository;

    @Getter
    private String message = "";

    public boolean updateConcept(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        message = "";
        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        conceptObject.setIdTerm(preferredTerm.map(PreferredTerm::getIdTerm).orElse(null));
        updatePrefLabel(idTheso, conceptObject, idUser);
        updateAltLabel(idTheso, conceptObject, idUser);
        updateNotes(idTheso, conceptObject, idUser);
        updateAlignments(idTheso, conceptObject, idUser);
        updateGeoLocalisation(idTheso, conceptObject);
        updateImages(idTheso, conceptObject, idUser);
        addExternalResources(idTheso, conceptObject.getIdConcept(), conceptObject.getExternalResources());
        return true;
    }

    public boolean updateConceptValueByNewValue(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        message = "";
        switch (nodeReplaceValueByValue.getSKOSProperty()) {
            case SKOSProperty.PREF_LABEL -> {
                if (!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())
                        && !updatePrefLabel(idTheso, nodeReplaceValueByValue, idUser)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
            }
            case SKOSProperty.ALT_LABEL -> {
                if (!updateAltLabel(idTheso, nodeReplaceValueByValue, idUser)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
            }
            case SKOSProperty.DEFINITION -> {
                if (!updateDefinition(idTheso, nodeReplaceValueByValue, idUser)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
            }
            case SKOSProperty.BROADER -> {
                if (!updateBroader(idTheso, nodeReplaceValueByValue, idUser)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
            }
            default -> {
            }
        }
        return true;
    }

    private boolean updatePrefLabel(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        if (StringUtils.isEmpty(nodeReplaceValueByValue.getIdConcept())) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, nodeReplaceValueByValue.getIdConcept());
        if (preferredTerm.isEmpty()) {
            return false;
        }

        if (isTermExistInLangAndThesaurus(preferredTerm.get().getIdTerm(), idTheso, nodeReplaceValueByValue.getIdLang())) {
            updateTermTraduction(
                    nodeReplaceValueByValue.getNewValue(),
                    preferredTerm.get().getIdTerm(),
                    nodeReplaceValueByValue.getIdLang(),
                    idTheso,
                    idUser
            );
        } else {
            addTermTraduction(
                    nodeReplaceValueByValue.getNewValue(),
                    preferredTerm.get().getIdTerm(),
                    nodeReplaceValueByValue.getIdLang(),
                    idTheso,
                    idUser
            );
        }
        return true;
    }

    private boolean updateAltLabel(String idThesaurus, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        if (StringUtils.isEmpty(nodeReplaceValueByValue.getIdConcept())) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, nodeReplaceValueByValue.getIdConcept());
        if (preferredTerm.isEmpty()) {
            return false;
        }

        if (!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
            if (!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                if (!updateNonPreferredTerm(
                        nodeReplaceValueByValue.getOldValue(),
                        nodeReplaceValueByValue.getNewValue(),
                        preferredTerm.get().getIdTerm(),
                        nodeReplaceValueByValue.getIdLang(),
                        idThesaurus,
                        false,
                        idUser
                )) {
                    addMessage("Rename AltLabel error :", nodeReplaceValueByValue);
                }
            }
        } else if (!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
            addNonPreferredTerm(Term.builder()
                    .idTerm(preferredTerm.get().getIdTerm())
                    .lexicalValue(nodeReplaceValueByValue.getNewValue())
                    .lang(nodeReplaceValueByValue.getIdLang())
                    .idThesaurus(idThesaurus)
                    .hidden(false)
                    .source("")
                    .status("")
                    .build(), idUser);
        }
        return true;
    }

    private boolean updateDefinition(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        if (StringUtils.isEmpty(nodeReplaceValueByValue.getIdConcept())) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        if (!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
            if (!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                int idNote = getNoteByValueAndThesaurus(
                        nodeReplaceValueByValue.getOldValue(),
                        "definition",
                        nodeReplaceValueByValue.getIdLang(),
                        idTheso
                );
                if (idNote != -1) {
                    if (!updateNote(
                            idNote,
                            nodeReplaceValueByValue.getIdConcept(),
                            nodeReplaceValueByValue.getIdLang(),
                            idTheso,
                            nodeReplaceValueByValue.getNewValue(),
                            "",
                            "definition",
                            idUser
                    )) {
                        addMessage("Rename definition error :", nodeReplaceValueByValue);
                    }
                } else if (!isNoteExist(
                        nodeReplaceValueByValue.getIdConcept(),
                        idTheso,
                        nodeReplaceValueByValue.getIdLang(),
                        nodeReplaceValueByValue.getNewValue(),
                        "definition"
                )) {
                    addNote(
                            nodeReplaceValueByValue.getIdConcept(),
                            nodeReplaceValueByValue.getIdLang(),
                            idTheso,
                            nodeReplaceValueByValue.getNewValue(),
                            "definition",
                            "",
                            idUser
                    );
                }
            }
        } else if (!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())
                && !isNoteExist(
                nodeReplaceValueByValue.getIdConcept(),
                idTheso,
                nodeReplaceValueByValue.getIdLang(),
                nodeReplaceValueByValue.getNewValue(),
                "definition"
        )) {
            addNote(
                    nodeReplaceValueByValue.getIdConcept(),
                    nodeReplaceValueByValue.getIdLang(),
                    idTheso,
                    nodeReplaceValueByValue.getNewValue(),
                    "definition",
                    "",
                    idUser
            );
        }
        return true;
    }

    private boolean updateBroader(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        if (StringUtils.isEmpty(nodeReplaceValueByValue.getIdConcept())) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        if (!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
            deleteRelationBT(nodeReplaceValueByValue.getIdConcept(), idTheso, nodeReplaceValueByValue.getOldValue(), idUser);
        }

        addRelationBT(nodeReplaceValueByValue.getIdConcept(), idTheso, nodeReplaceValueByValue.getNewValue(), idUser);
        conceptRepository.updateTopConcept(nodeReplaceValueByValue.getIdConcept(), idTheso, false);
        return true;
    }

    private boolean updatePrefLabel(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        if (StringUtils.isEmpty(conceptObject.getIdConcept())) {
            message = message + "\n" + "concept sans identifiant : " + conceptObject.getPrefLabels();
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        if (preferredTerm.isEmpty()) {
            return false;
        }

        for (ThesaurusCsvConceptLabel prefLabel : conceptObject.getPrefLabels()) {
            if (!isTermExistInLangAndThesaurus(preferredTerm.get().getIdTerm(), idTheso, prefLabel.getLang())) {
                addTermTraduction(Term.builder()
                        .lexicalValue(prefLabel.getLabel())
                        .idTerm(preferredTerm.get().getIdTerm())
                        .lang(prefLabel.getLang())
                        .idThesaurus(idTheso)
                        .source("import")
                        .status("")
                        .build(), idUser);
            } else {
                var term = termRepository.findByIdTermAndIdThesaurusAndLang(
                        preferredTerm.get().getIdTerm(), idTheso, prefLabel.getLang());
                String oldLabel = term.map(fr.cnrs.opentheso.entites.Term::getLexicalValue).orElse("");

                if (prefLabel.getLabel().isEmpty()) {
                    termRepository.deleteByIdTermAndLangAndIdThesaurus(
                            preferredTerm.get().getIdTerm(), prefLabel.getLang(), idTheso);
                    return true;
                }

                if (oldLabel.isEmpty()) {
                    updateTermTraduction(
                            prefLabel.getLabel(),
                            preferredTerm.get().getIdTerm(),
                            prefLabel.getLang(),
                            idTheso,
                            idUser
                    );
                    return true;
                }

                if (oldLabel.trim().equals(prefLabel.getLabel().trim())) {
                    continue;
                }

                updateTermTraduction(
                        prefLabel.getLabel(),
                        preferredTerm.get().getIdTerm(),
                        prefLabel.getLang(),
                        idTheso,
                        idUser
                );
            }
        }
        return true;
    }

    private boolean updateAltLabel(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        if (StringUtils.isEmpty(conceptObject.getIdConcept())) {
            message = message + "\n" + "concept sans identifiant : " + conceptObject.getPrefLabels();
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        if (preferredTerm.isEmpty()) {
            return false;
        }

        for (String lang : getLangs(conceptObject.getAltLabels())) {
            var oldLabels = getNonPreferredTermValues(idTheso, preferredTerm.get().getIdTerm(), lang);
            for (String oldLabel : oldLabels) {
                deleteNonPreferredTerm(preferredTerm.get().getIdTerm(), lang, oldLabel, idTheso, idUser);
            }
        }

        for (ThesaurusCsvConceptLabel altLabel : conceptObject.getAltLabels()) {
            if (!altLabel.getLabel().isEmpty()) {
                addNonPreferredTerm(Term.builder()
                        .idTerm(preferredTerm.get().getIdTerm())
                        .lexicalValue(altLabel.getLabel())
                        .lang(altLabel.getLang())
                        .idThesaurus(idTheso)
                        .hidden(false)
                        .source("import")
                        .status("")
                        .build(), idUser);
            }
        }
        return true;
    }

    private boolean updateNotes(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        updateNotesByType(idTheso, conceptObject, conceptObject.getNote(), "note", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getScopeNotes(), "scopeNote", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getDefinitions(), "definition", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getChangeNotes(), "changeNote", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getEditorialNotes(), "editorialNote", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getHistoryNotes(), "historyNote", idUser);
        updateNotesByType(idTheso, conceptObject, conceptObject.getExamples(), "example", idUser);
        return true;
    }

    private void updateNotesByType(
            String idTheso,
            ThesaurusCsvConceptObject conceptObject,
            List<ThesaurusCsvConceptLabel> notes,
            String noteType,
            int idUser
    ) {
        if (notes == null) {
            return;
        }
        for (String lang : getLangs(notes)) {
            deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, noteType);
        }
        for (ThesaurusCsvConceptLabel note : notes) {
            if (!note.getLabel().isEmpty()) {
                addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), noteType, "", idUser);
            }
        }
    }

    private boolean updateAlignments(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        NodeAlignment nodeAlignment = new NodeAlignment();
        nodeAlignment.setId_author(idUser);
        nodeAlignment.setConcept_target("");
        nodeAlignment.setThesaurus_target("");
        nodeAlignment.setInternal_id_concept(conceptObject.getIdConcept());
        nodeAlignment.setInternal_id_thesaurus(idTheso);

        updateAlignmentType(conceptObject.getExactMatchs(), 1, nodeAlignment);
        updateAlignmentType(conceptObject.getCloseMatchs(), 2, nodeAlignment);
        updateAlignmentType(conceptObject.getBroadMatchs(), 3, nodeAlignment);
        updateAlignmentType(conceptObject.getRelatedMatchs(), 4, nodeAlignment);
        updateAlignmentType(conceptObject.getNarrowMatchs(), 5, nodeAlignment);
        return true;
    }

    private void updateAlignmentType(List<String> uris, int typeId, NodeAlignment nodeAlignment) {
        if (CollectionUtils.isEmpty(uris)) {
            return;
        }
        deleteAlignmentOfConceptByType(nodeAlignment.getInternal_id_concept(), nodeAlignment.getInternal_id_thesaurus(), typeId);
        for (String uri : uris) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(typeId);
            addNewAlignment(nodeAlignment);
        }
    }

    private boolean updateGeoLocalisation(String idTheso, ThesaurusCsvConceptObject conceptObject) {
        if ((conceptObject.getLatitude() == null || conceptObject.getLongitude() == null)
                && (conceptObject.getGps() == null || conceptObject.getGps().isEmpty())) {
            return true;
        }

        gpsRepository.deleteByIdConceptAndIdTheso(conceptObject.getIdConcept(), idTheso);
        List<NodeGps> nodeGpses = new ArrayList<>();

        if (StringUtils.isNotEmpty(conceptObject.getLatitude())) {
            try {
                NodeGps nodeGps = new NodeGps();
                nodeGps.setLatitude(Double.valueOf(conceptObject.getLatitude()));
                nodeGps.setLongitude(Double.valueOf(conceptObject.getLongitude()));
                nodeGps.setPosition(1);
                nodeGpses.add(nodeGps);
                saveGps(conceptObject.getIdConcept(), idTheso, nodeGpses);
            } catch (Exception ignored) {
                return true;
            }
        } else if (StringUtils.isNotEmpty(conceptObject.getGps())) {
            var gpsList = ThesaurusCsvGpsParser.readGps(conceptObject.getGps(), idTheso, conceptObject.getIdConcept());
            if (CollectionUtils.isNotEmpty(gpsList)) {
                for (Gps gpsValue : gpsList) {
                    NodeGps nodeGps = new NodeGps();
                    nodeGps.setLatitude(gpsValue.getLatitude());
                    nodeGps.setLongitude(gpsValue.getLongitude());
                    nodeGps.setPosition(gpsValue.getPosition());
                    nodeGpses.add(nodeGps);
                }
                saveGps(conceptObject.getIdConcept(), idTheso, nodeGpses);
            }
        }
        return true;
    }

    private boolean updateImages(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        if (CollectionUtils.isEmpty(conceptObject.getImages())) {
            return true;
        }

        imagesRepository.deleteAllByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());

        for (NodeImage nodeImage : conceptObject.getImages()) {
            if (nodeImage == null || StringUtils.isEmpty(nodeImage.getUri())) {
                continue;
            }
            imagesRepository.save(ImageExterne.builder()
                    .imageCreator("")
                    .idUser(idUser)
                    .idConcept(conceptObject.getIdConcept())
                    .idThesaurus(idTheso)
                    .imageName(StringUtils.isEmpty(nodeImage.getImageName()) ? "" : nodeImage.getImageName())
                    .imageCopyright(nodeImage.getCopyRight())
                    .externalUri(nodeImage.getUri().trim())
                    .build());
        }
        return true;
    }

    private void addExternalResources(String idTheso, String idConcept, List<String> externalResources) {
        if (CollectionUtils.isEmpty(externalResources)) {
            return;
        }
        for (String externalResource : externalResources) {
            if (externalResource == null || externalResource.isEmpty()) {
                return;
            }
            if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(externalResource)) {
                return;
            }
            externalResourcesRepository.save(ExternalResource.builder()
                    .idConcept(idConcept)
                    .idThesaurus(idTheso)
                    .externalUri(externalResource)
                    .build());
        }
    }

    private ArrayList<String> getLangs(List<ThesaurusCsvConceptLabel> labels) {
        ArrayList<String> langs = new ArrayList<>();
        if (labels == null) {
            return langs;
        }
        for (ThesaurusCsvConceptLabel label : labels) {
            if (!langs.contains(label.getLang())) {
                langs.add(label.getLang());
            }
        }
        return langs;
    }

    private void addMessage(String error, NodeReplaceValueByValue nodeReplaceValueByValue) {
        message = message + error + nodeReplaceValueByValue.getIdConcept() + "##"
                + nodeReplaceValueByValue.getOldValue() + "##"
                + nodeReplaceValueByValue.getNewValue() + "##"
                + nodeReplaceValueByValue.getIdLang() + "\n";
    }

    private boolean isTermExistInLangAndThesaurus(String idTerm, String idThesaurus, String idLang) {
        return termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idThesaurus, idLang).isPresent();
    }

    private void updateTermTraduction(String label, String idTerm, String idLang, String idTheso, int idUser) {
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idTheso, idLang);
        if (term.isEmpty()) {
            return;
        }
        term.get().setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label));
        term.get().setContributor(idUser);
        term.get().setModified(new Date());
        termRepository.save(term.get());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(term.get().getIdTerm())
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label))
                .lang(idLang)
                .idThesaurus(idTheso)
                .source("")
                .status("")
                .idUser(idUser)
                .action("UPDATE")
                .modified(LocalDateTime.now())
                .build());
    }

    private void addTermTraduction(String label, String idTerm, String idLang, String idTheso, int idUser) {
        label = fr.cnrs.opentheso.utils.StringUtils.convertString(label);
        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(label)
                .lang(idLang)
                .idThesaurus(idTheso)
                .source("")
                .status("")
                .contributor(idUser)
                .creator(idUser)
                .created(new Date())
                .modified(new Date())
                .build());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(termSaved.getIdTerm())
                .lexicalValue(termSaved.getLexicalValue())
                .lang(termSaved.getLang())
                .idThesaurus(termSaved.getIdThesaurus())
                .source(termSaved.getSource())
                .status(termSaved.getStatus())
                .idUser(idUser)
                .modified(LocalDateTime.now())
                .action("New")
                .build());
    }

    private void addTermTraduction(Term term, int idUser) {
        addTermTraduction(term.getLexicalValue(), term.getIdTerm(), term.getLang(), term.getIdThesaurus(), idUser);
    }

    private boolean addNonPreferredTerm(Term term, int idUser) {
        if (nonPreferredTermRepository.isAltLabelExist(term.getLexicalValue().trim(), term.getIdThesaurus(), term.getLang())) {
            return false;
        }
        nonPreferredTermRepository.save(NonPreferredTerm.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(term.getLexicalValue())
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .hiden(term.isHidden())
                .created(new Date())
                .modified(new Date())
                .build());
        saveNonPreferredTrace(term.getIdTerm(), term.getLexicalValue(), term.getIdThesaurus(), term.getLang(), idUser, term.isHidden(), "ADD");
        return true;
    }

    private void deleteNonPreferredTerm(String idTerm, String idLang, String lexicalValue, String idTheso, int idUser) {
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLexicalValueAndLang(idTheso, idTerm, lexicalValue, idLang);
        saveNonPreferredTrace(idTerm, lexicalValue, idTheso, idLang, idUser, false, "delete");
    }

    private List<String> getNonPreferredTermValues(String idThesaurus, String idTerm, String lang) {
        var nonPreferredTerms = nonPreferredTermRepository.findAllByIdThesaurusAndIdTermAndLangAndHidenNot(
                idThesaurus, idTerm, lang, true);
        if (CollectionUtils.isEmpty(nonPreferredTerms)) {
            return List.of();
        }
        return nonPreferredTerms.stream().map(NonPreferredTerm::getLexicalValue).toList();
    }

    private boolean updateNonPreferredTerm(
            String oldValue,
            String newValue,
            String idTerm,
            String idLang,
            String idTheso,
            boolean isHidden,
            int idUser
    ) {
        var nonPreferredTerm = nonPreferredTermRepository.findByIdTermAndLexicalValueAndLangAndIdThesaurus(
                idTerm, oldValue, idLang, idTheso);
        if (nonPreferredTerm.isEmpty()) {
            return false;
        }
        nonPreferredTerm.get().setLexicalValue(newValue);
        nonPreferredTerm.get().setHiden(isHidden);
        nonPreferredTerm.get().setModified(new Date());
        nonPreferredTermRepository.save(nonPreferredTerm.get());
        saveNonPreferredTrace(idTerm, newValue, idTheso, idLang, idUser, isHidden, "update");
        return true;
    }

    private void saveNonPreferredTrace(
            String idTerm,
            String lexicalValue,
            String idThesaurus,
            String idLang,
            int idUser,
            boolean isHidden,
            String action
    ) {
        nonPreferredTermHistoriqueRepository.save(NonPreferredTermHistorique.builder()
                .idTerm(idTerm)
                .lexicalValue(lexicalValue)
                .idThesaurus(idThesaurus)
                .lang(idLang)
                .idUser(idUser)
                .hiden(isHidden)
                .action(action)
                .modified(new Date())
                .status("")
                .source("")
                .build());
    }

    private void addNote(
            String identifier,
            String idLang,
            String idThesaurus,
            String note,
            String noteTypeCode,
            String noteSource,
            int idUser
    ) {
        idLang = normalizeIdLang(idLang);
        note = fr.cnrs.opentheso.utils.StringUtils.clearValue(note);
        note = fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(note);
        note = StringEscapeUtils.unescapeXml(note);

        if (isNoteExistInThatLang(identifier, idThesaurus, idLang, noteTypeCode)) {
            var noteToUpdate = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                    identifier, idThesaurus, noteTypeCode, idLang);
            if (!noteToUpdate.isEmpty()) {
                noteToUpdate.get(0).setLexicalValue(note);
                noteToUpdate.get(0).setNoteSource(noteSource);
                noteRepository.save(noteToUpdate.get(0));
            }
        } else {
            noteRepository.save(Note.builder()
                    .noteTypeCode(noteTypeCode)
                    .idThesaurus(idThesaurus)
                    .lang(idLang)
                    .lexicalValue(note)
                    .identifier(identifier)
                    .noteSource(noteSource)
                    .idUser(idUser)
                    .created(new Date())
                    .modified(new Date())
                    .build());
        }
    }

    private boolean isNoteExist(String identifier, String idThesaurus, String idLang, String note, String noteTypeCode) {
        idLang = normalizeIdLang(idLang);
        return noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                identifier,
                idThesaurus,
                noteTypeCode,
                idLang,
                fr.cnrs.opentheso.utils.StringUtils.convertString(note)
        ).isPresent();
    }

    private boolean isNoteExistInThatLang(String identifier, String idThesaurus, String idLang, String noteTypeCode) {
        idLang = normalizeIdLang(idLang);
        return !noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                identifier, idThesaurus, noteTypeCode, idLang).isEmpty();
    }

    private void deleteNoteByLang(String identifier, String idThesaurus, String idLang, String noteTypeCode) {
        noteRepository.deleteAllByIdThesaurusAndIdentifierAndLangAndNoteTypeCode(idThesaurus, identifier, idLang, noteTypeCode);
    }

    private int getNoteByValueAndThesaurus(String value, String noteTypeCode, String idLang, String idThesaurus) {
        value = fr.cnrs.opentheso.utils.StringUtils.convertString(value);
        var note = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                value, idThesaurus, noteTypeCode, idLang);
        return note.isEmpty() ? -1 : note.get(0).getId();
    }

    private boolean updateNote(
            int idNote,
            String idConcept,
            String idLang,
            String idThesaurus,
            String note,
            String noteSource,
            String noteTypeCode,
            int idUser
    ) {
        var noteValue = noteRepository.findByIdAndIdThesaurus(idNote, idThesaurus);
        if (noteValue.isEmpty()) {
            return false;
        }
        noteValue.get().setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(note));
        noteValue.get().setNoteSource(noteSource);
        noteValue.get().setModified(new Date());
        noteRepository.save(noteValue.get());
        addConceptNoteHistorique(idConcept, normalizeIdLang(idLang), idThesaurus, note, noteTypeCode, "update", idUser);
        return true;
    }

    private void addConceptNoteHistorique(
            String idConcept,
            String idLang,
            String idThesaurus,
            String note,
            String noteTypeCode,
            String actionPerformed,
            int idUser
    ) {
        noteHistoriqueRepository.save(NoteHistorique.builder()
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .lang(idLang)
                .lexicalvalue(fr.cnrs.opentheso.utils.StringUtils.convertString(note))
                .actionPerformed(actionPerformed)
                .idUser(idUser)
                .notetypecode(noteTypeCode)
                .modified(new Date())
                .build());
    }

    private boolean addNewAlignment(NodeAlignment nodeAlignment) {
        int idAlignementSource = nodeAlignment.getId_source() == null ? -1 : nodeAlignment.getId_source();
        String thesaurusTarget = fr.cnrs.opentheso.utils.StringUtils.convertString(nodeAlignment.getThesaurus_target());
        String idConcept = nodeAlignment.getInternal_id_concept();
        String idThesaurus = nodeAlignment.getInternal_id_thesaurus();
        int idTypeAlignment = nodeAlignment.getAlignement_id_type();
        String uriTarget = fr.cnrs.opentheso.utils.StringUtils.convertString(nodeAlignment.getUri_target());
        String conceptTarget = fr.cnrs.opentheso.utils.StringUtils.convertString(nodeAlignment.getConcept_target());

        if (alignementRepository.existsByConceptThesaurusTypeAndUri(idThesaurus, idConcept, idTypeAlignment, uriTarget)) {
            return true;
        }

        var alignementType = alignementTypeRepository.findById(idTypeAlignment);
        if (alignementType.isEmpty()) {
            return false;
        }

        var alignementSource = idAlignementSource > 0
                ? alignementSourceRepository.findById(idAlignementSource)
                : java.util.Optional.<fr.cnrs.opentheso.entites.AlignementSource>empty();

        alignementRepository.save(Alignement.builder()
                .author(nodeAlignment.getId_author())
                .conceptTarget(conceptTarget)
                .thesaurusTarget(thesaurusTarget)
                .uriTarget(uriTarget)
                .urlAvailable(true)
                .alignementType(alignementType.get())
                .internalIdConcept(idConcept)
                .internalIdThesaurus(idThesaurus)
                .alignementSource(alignementSource.orElse(null))
                .created(new Date())
                .modified(new Date())
                .build());
        return true;
    }

    private void deleteAlignmentOfConceptByType(String idConcept, String idThesaurus, int typeId) {
        alignementRepository.deleteByConceptThesaurusAndType(idConcept, idThesaurus, typeId);
    }

    private void saveGps(String idConcept, String idThesaurus, List<NodeGps> nodeGpses) {
        for (NodeGps nodeGps : nodeGpses) {
            gpsRepository.save(Gps.builder()
                    .idConcept(idConcept)
                    .idTheso(idThesaurus)
                    .latitude(nodeGps.getLatitude())
                    .longitude(nodeGps.getLongitude())
                    .position(nodeGps.getPosition())
                    .build());
        }
    }

    private void deleteRelationBT(String idConceptNt, String idThesaurus, String idConceptBt, int idUser) {
        addRelationHistorique(idConceptNt, idThesaurus, idConceptBt, "BT", idUser, "DEL");
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                idThesaurus, idConceptNt, idConceptBt, "BT");
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                idThesaurus, idConceptBt, idConceptNt, "NT");
    }

    private void addRelationBT(String idConceptNt, String idThesaurus, String idConceptBt, int idUser) {
        addRelationHistorique(idConceptNt, idThesaurus, idConceptBt, "BT", idUser, "ADD");
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConceptNt)
                .idConcept2(idConceptBt)
                .idThesaurus(idThesaurus)
                .role("BT")
                .build());
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConceptBt)
                .idConcept2(idConceptNt)
                .idThesaurus(idThesaurus)
                .role("NT")
                .build());
    }

    private void addRelationHistorique(
            String idConcept1,
            String idThesaurus,
            String idConcept2,
            String role,
            int idUser,
            String action
    ) {
        hierarchicalRelationshipHistoriqueRepository.save(HierarchicalRelationshipHistorique.builder()
                .idConcept1(idConcept1)
                .idConcept2(idConcept2)
                .idThesaurus(idThesaurus)
                .modified(new Date())
                .idUser(idUser)
                .action(action)
                .role(role)
                .build());
    }

    private String normalizeIdLang(String idLang) {
        return switch (idLang) {
            case "en-GB", "en-US" -> "en";
            case "pt-BR", "pt-PT" -> "pt";
            default -> idLang;
        };
    }
}
