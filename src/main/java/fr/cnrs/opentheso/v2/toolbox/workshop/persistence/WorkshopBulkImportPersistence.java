package fr.cnrs.opentheso.v2.toolbox.workshop.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptHistorique;
import fr.cnrs.opentheso.entites.ConceptReplacedBy;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.HierarchicalRelationshipHistorique;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.NonPreferredTermHistorique;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.TermHistorique;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.UserGroupLabelRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkshopBulkImportPersistence {

    private final LanguageRepository languageRepository;
    private final UserGroupLabelRepository userGroupLabelRepository;
    private final UserRepository userRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final ConceptReplacedByRepository conceptReplacedByRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final TermRepository termRepository;
    private final TermHistoriqueRepository termHistoriqueRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;
    private final NoteRepository noteRepository;
    private final AlignementRepository alignementRepository;
    private final AlignementTypeRepository alignementTypeRepository;
    private final AlignementSourceRepository alignementSourceRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ImagesRepository imagesRepository;
    private final ConceptSearchEngine conceptSearchEngine;
    private final ThesaurusCsvImportEngine thesaurusCsvImportEngine;
    private final WorkshopCsvConceptUpdater workshopCsvConceptUpdater;

    private String message = "";
    private String formatDate = "yyyy-MM-dd";

    public List<LanguageIso639> findAllLanguages() {
        return languageRepository.findAllOrderByCodePays();
    }

    public List<UserGroupLabel> findUserProjects(int userId) {
        return userRepository.findById(userId)
                .map(user -> Boolean.TRUE.equals(user.getIsSuperAdmin())
                        ? userGroupLabelRepository.findAll()
                        : userGroupLabelRepository.findProjectsByRole(userId, 2))
                .orElseGet(List::of);
    }

    public List<UserGroupLabel> findProjectsByRole(int userId, int roleId) {
        return userGroupLabelRepository.findProjectsByRole(userId, roleId);
    }

    public String getUserDisplayName(int userId) {
        return userRepository.findById(userId).map(user -> StringUtils.defaultString(user.getUsername())).orElse("");
    }

    public void refreshAfterBulkImport(String thesaurusId) {
        // no-op
    }

    public String getIdConceptFromArkId(String arkId, String idThesaurus) {
        return conceptRepository.findConceptIdByArkIgnoreCase(arkId, idThesaurus).orElse(null);
    }

    public String getIdConceptFromHandleId(String handleId) {
        return conceptRepository.findConceptIdByHandleIgnoreCase(handleId).orElse(null);
    }

    public boolean isIdExiste(String idConcept, String idThesaurus) {
        return conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus).isPresent();
    }

    public void deleteNotes(String identifier, String idThesaurus) {
        noteRepository.deleteAllByIdentifierAndIdThesaurus(identifier, idThesaurus);
    }

    public boolean isNoteExist(String identifier, String idThesaurus, String idLang, String note, String noteTypeCode) {
        return noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                identifier,
                idThesaurus,
                noteTypeCode,
                normalizeIdLang(idLang),
                fr.cnrs.opentheso.utils.StringUtils.convertString(note)
        ).isPresent();
    }

    public void addNote(
            String identifier,
            String idLang,
            String idThesaurus,
            String note,
            String noteTypeCode,
            String noteSource,
            int idUser
    ) {
        String normalizedLang = normalizeIdLang(idLang);
        String normalizedNote = StringEscapeUtils.unescapeXml(
                fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(
                        fr.cnrs.opentheso.utils.StringUtils.clearValue(note)
                )
        );

        var existing = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                identifier, idThesaurus, noteTypeCode, normalizedLang);
        if (!existing.isEmpty()) {
            existing.get(0).setLexicalValue(normalizedNote);
            existing.get(0).setNoteSource(noteSource);
            noteRepository.save(existing.get(0));
            return;
        }

        noteRepository.save(Note.builder()
                .noteTypeCode(noteTypeCode)
                .idThesaurus(idThesaurus)
                .lang(normalizedLang)
                .lexicalValue(normalizedNote)
                .identifier(identifier)
                .noteSource(noteSource)
                .idUser(idUser)
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public boolean addNewAlignment(NodeAlignment nodeAlignment) {
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
                : Optional.<fr.cnrs.opentheso.entites.AlignementSource>empty();

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

    public List<NodeAlignmentSmall> getAllAlignmentsOfConcept(String idConcept, String idThesaurus) {
        var alignements = alignementRepository.findAllAlignmentsByConceptAndThesaurus(idConcept, idThesaurus);
        if (CollectionUtils.isEmpty(alignements)) {
            return List.of();
        }
        return alignements.stream()
                .map(element -> NodeAlignmentSmall.builder()
                        .uri_target(element.getUri_target())
                        .alignement_id_type(element.getAlignement_id_type())
                        .build())
                .toList();
    }

    public boolean deleteAlignmentByUri(String uri, String idConcept, String idThesaurus) {
        try {
            return alignementRepository.deleteByUriAndConceptAndThesaurus(uri, idConcept, idThesaurus) > 0;
        } catch (Exception ex) {
            message = ex.getMessage();
            return false;
        }
    }

    public List<String> getAllIdConceptOfThesaurus(String idThesaurus) {
        var concepts = conceptRepository.findAllByIdThesaurusAndStatusNot(idThesaurus, "CA");
        if (CollectionUtils.isEmpty(concepts)) {
            return List.of();
        }
        return concepts.stream().map(fr.cnrs.opentheso.entites.Concept::getIdConcept).toList();
    }

    public List<String> getIdsOfBranch(String idConceptRoot, String idThesaurus) {
        List<String> ids = new ArrayList<>();
        collectBranchIds(idConceptRoot, idThesaurus, ids);
        return ids;
    }

    public Map<String, String> getArkIdsFromIdConcepts(Set<String> idConcepts, String idThesaurus) {
        if (CollectionUtils.isEmpty(idConcepts)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (Object[] row : conceptRepository.findArkFromIdConcepts(idConcepts, idThesaurus)) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    public Map<String, String> getIdConceptsFromArkIds(Set<String> arkIds, String idThesaurus) {
        if (CollectionUtils.isEmpty(arkIds)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (Object[] row : conceptRepository.findArkIds(arkIds, idThesaurus)) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    public Concept getConcept(String idConcept, String idThesaurus) {
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isEmpty()) {
            return null;
        }
        var entity = concept.get();
        return Concept.builder()
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .idArk(entity.getIdArk())
                .idHandle(entity.getIdHandle())
                .idDoi(entity.getIdDoi())
                .created(entity.getCreated())
                .modified(entity.getModified())
                .status(entity.getStatus())
                .notation(entity.getNotation())
                .topConcept(entity.getTopConcept())
                .creator(entity.getCreator())
                .contributor(entity.getContributor())
                .conceptType(entity.getConceptType() == null ? null : entity.getConceptType().toLowerCase())
                .build();
    }

    public boolean updateArkIdOfConcept(String idConcept, String idThesaurus, String idArk) {
        conceptRepository.setIdArk(idArk, new Date(), idConcept, idThesaurus);
        return true;
    }

    public boolean updateNotation(String idConcept, String idThesaurus, String notation) {
        return conceptRepository.updateNotation(idConcept, idThesaurus, notation) > 0;
    }

    public boolean deprecateConcept(String idConcept, String idThesaurus, int idUser) {
        try {
            conceptRepository.setStatus("DEP", idConcept, idThesaurus);
            var concept = getConcept(idConcept, idThesaurus);
            if (concept != null) {
                addConceptHistorique(concept, idUser);
            }
            return true;
        } catch (Exception ex) {
            message = ex.getMessage();
            return false;
        }
    }

    public void addReplacedBy(String idConcept, String idThesaurus, String idConceptReplaceBy, int idUser) {
        conceptReplacedByRepository.save(ConceptReplacedBy.builder()
                .idConcept1(idConcept)
                .idConcept2(idConceptReplaceBy)
                .idThesaurus(idThesaurus)
                .idUser(idUser)
                .modified(new Date())
                .build());
    }

    public boolean updateDateOfConcept(String idThesaurus, String idConcept, int contributor) {
        return conceptRepository.updateDateOfConcept(idThesaurus, idConcept, contributor) > 0;
    }

    public Term getThisTerm(String idConcept, String idThesaurus, String idLang) {
        if (termRepository.existsTranslationForConcept(idConcept, idThesaurus, idLang)) {
            var result = termRepository.getPreferredTermWithConceptInfo(idConcept, idThesaurus, idLang);
            if (result instanceof Object[] row) {
                return Term.builder()
                        .idTerm((String) row[0])
                        .idConcept((String) row[1])
                        .lexicalValue((String) row[2])
                        .lang((String) row[3])
                        .idThesaurus((String) row[4])
                        .created(Date.from((Instant) row[5]))
                        .modified(Date.from((Instant) row[6]))
                        .source((String) row[7])
                        .status((String) row[8])
                        .contributor(row[9] != null ? (Integer) row[9] : -1)
                        .creator(row[10] != null ? (Integer) row[10] : -1)
                        .build();
            }
        }

        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isPresent()) {
            return Term.builder()
                    .idTerm("")
                    .idConcept(idConcept)
                    .lexicalValue("")
                    .lang(idLang)
                    .idThesaurus(idThesaurus)
                    .created(concept.get().getCreated())
                    .modified(concept.get().getModified())
                    .status(concept.get().getStatus())
                    .build();
        }
        return null;
    }

    public PreferredTerm getPreferredTermByThesaurusAndConcept(String idThesaurus, String idConcept) {
        return preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept).orElse(null);
    }

    public void addTermTraduction(String label, String idTerm, String idLang, String idThesaurus, int idUser) {
        String convertedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(label);
        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(convertedLabel)
                .lang(idLang)
                .idThesaurus(idThesaurus)
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

    public void updateTermTraduction(String label, String idTerm, String idLang, String idThesaurus, int idUser) {
        var term = termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, idThesaurus, idLang);
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
                .idThesaurus(idThesaurus)
                .source("")
                .status("")
                .idUser(idUser)
                .action("UPDATE")
                .modified(LocalDateTime.now())
                .build());
    }

    public boolean addNonPreferredTerm(Term term, int idUser) {
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

    public void deleteNonPreferredTerm(String idTerm, String idLang, String lexicalValue, String idThesaurus, int idUser) {
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLexicalValueAndLang(idThesaurus, idTerm, lexicalValue, idLang);
        saveNonPreferredTrace(idTerm, lexicalValue, idThesaurus, idLang, idUser, false, "delete");
    }

    public ImageExterne addExternalImage(
            String idConcept,
            String idThesaurus,
            String imageName,
            String copyRight,
            String uri,
            String creator,
            int idUser
    ) {
        return imagesRepository.save(ImageExterne.builder()
                .imageCreator(creator)
                .idUser(idUser)
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .imageName(StringUtils.isEmpty(imageName) ? "" : imageName)
                .imageCopyright(copyRight)
                .externalUri(uri.trim())
                .build());
    }

    public String addConcept(String idParent, String relationType, Concept concept, Term term, int idUser) {
        if (idParent == null) {
            concept.setTopConcept(true);
        }

        String idConcept = addConceptInTable(concept, idUser);
        if (idConcept == null) {
            return null;
        }

        if (StringUtils.isNotEmpty(concept.getIdGroup())) {
            addConceptGroupConcept(concept.getIdGroup(), idConcept, concept.getIdThesaurus());
        }

        String idTerm = addTerm(term, idConcept, idUser);
        if (idTerm == null) {
            return null;
        }
        term.setIdTerm(idTerm);

        if (!concept.isTopConcept()) {
            String inverseRelation = switch (StringUtils.defaultString(relationType, "NT")) {
                case "NT" -> "BT";
                case "NTG" -> "BTG";
                case "NTP" -> "BTP";
                case "NTI" -> "BTI";
                default -> "BT";
            };
            addLinkHierarchicalRelation(HierarchicalRelationship.builder()
                    .idConcept1(idParent)
                    .idConcept2(idConcept)
                    .idThesaurus(concept.getIdThesaurus())
                    .role(relationType)
                    .build(), idUser);
            addLinkHierarchicalRelation(HierarchicalRelationship.builder()
                    .idConcept1(idConcept)
                    .idConcept2(idParent)
                    .idThesaurus(concept.getIdThesaurus())
                    .role(inverseRelation)
                    .build(), idUser);
        }
        return idConcept;
    }

    public boolean addConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
        return true;
    }

    public void addSubGroup(String fatherGroupId, String childGroupId, String thesaurusId) {
        thesaurusCsvImportEngine.addSubGroup(fatherGroupId, childGroupId, thesaurusId);
    }

    public boolean addGroup(String idTheso, ThesaurusCsvConceptObject conceptObject) {
        return thesaurusCsvImportEngine.addGroup(idTheso, conceptObject);
    }

    public boolean addConceptV2(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser, String formatDate) {
        return thesaurusCsvImportEngine.addConceptV2(idTheso, conceptObject, idUser, formatDate);
    }

    public String getIdGroupFromArkId(String idArk, String idThesaurus) {
        var group = conceptGroupRepository.findAllByIdThesaurusAndIdArk(idThesaurus, idArk);
        return group.map(fr.cnrs.opentheso.entites.ConceptGroup::getIdGroup).orElse(null);
    }

    public String getIdGroupFromHandleId(String idHandle) {
        return conceptGroupRepository.findByIdHandle(idHandle)
                .map(fr.cnrs.opentheso.entites.ConceptGroup::getIdGroup)
                .orElse(null);
    }

    public boolean isIdGroupExiste(String idGroup, String idThesaurus) {
        return conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus).isPresent();
    }

    public List<NodeSearchMini> searchExactTermForAutocompletion(String value, String lang, String thesaurusId) {
        return toNodeSearchMinis(conceptSearchEngine.autocomplete(value, ConceptSearchMode.EXACT, thesaurusId, lang, false));
    }

    public List<NodeSearchMini> searchExactMatch(String value, String lang, String thesaurusId, boolean isPrivate) {
        return toNodeSearchMinis(conceptSearchEngine.autocomplete(value, ConceptSearchMode.EXACT, thesaurusId, lang, isPrivate));
    }

    public List<NodeSearchMini> searchStartWith(String value, String lang, String thesaurusId, boolean isPrivate) {
        return toNodeSearchMinis(conceptSearchEngine.autocomplete(value, ConceptSearchMode.START_WITH, thesaurusId, lang, isPrivate));
    }

    public List<NodeSearchMini> searchFullTextElastic(String value, String lang, String thesaurusId, boolean isPrivate) {
        return toNodeSearchMinis(conceptSearchEngine.autocomplete(value, ConceptSearchMode.FULL_TEXT, thesaurusId, lang, isPrivate));
    }

    public boolean updateConcept(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser) {
        return workshopCsvConceptUpdater.updateConcept(idTheso, conceptObject, idUser);
    }

    public boolean updateConceptValueByNewValue(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser) {
        return workshopCsvConceptUpdater.updateConceptValueByNewValue(idTheso, nodeReplaceValueByValue, idUser);
    }

    public String getMessage() {
        return StringUtils.defaultString(message) + StringUtils.defaultString(workshopCsvConceptUpdater.getMessage());
    }

    public void setFormatDate(String formatDate) {
        this.formatDate = formatDate;
        thesaurusCsvImportEngine.setFormatDate(formatDate);
    }

    public ConceptDcTerm save(ConceptDcTerm conceptDcTerm) {
        return conceptDcTermRepository.save(conceptDcTerm);
    }

    public void saveAll(List<HierarchicalRelationship> relations) {
        hierarchicalRelationshipRepository.saveAll(relations);
    }

    public Optional<PreferredTerm> findByIdThesaurusAndIdConcept(String idThesaurus, String idConcept) {
        return preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept);
    }

    public List<String> findExistingIds(Set<String> ids, String idThesaurus) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return conceptRepository.findExistingIds(ids, idThesaurus);
    }

    public List<String> getAllUsedLanguagesOfThesaurus(String idThesaurus) {
        var languages = termRepository.searchDistinctLangInThesaurus(idThesaurus);
        return CollectionUtils.isEmpty(languages) ? List.of() : languages;
    }

    public int deleteAllByConceptAndThesaurus(String idConcept, String idThesaurus) {
        return nonPreferredTermRepository.deleteAllByConceptAndThesaurus(idConcept, idThesaurus);
    }

    private void collectBranchIds(String idConceptRoot, String idThesaurus, List<String> ids) {
        if (ids.contains(idConceptRoot)) {
            return;
        }
        ids.add(idConceptRoot);
        var children = hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(
                idThesaurus, idConceptRoot, "NT");
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (HierarchicalRelationship child : children) {
            collectBranchIds(child.getIdConcept2(), idThesaurus, ids);
        }
    }

    private List<NodeSearchMini> toNodeSearchMinis(List<ConceptSearchSuggestion> suggestions) {
        if (CollectionUtils.isEmpty(suggestions)) {
            return List.of();
        }
        List<NodeSearchMini> results = new ArrayList<>();
        for (ConceptSearchSuggestion suggestion : suggestions) {
            NodeSearchMini mini = new NodeSearchMini();
            mini.setIdConcept(suggestion.conceptId());
            mini.setPrefLabel(suggestion.preferredLabel());
            mini.setAltLabelValue(suggestion.altLabel());
            mini.setAltLabel(suggestion.isAltLabel());
            mini.setConcept(suggestion.isConcept());
            mini.setGroup(suggestion.isGroup());
            mini.setFacet(suggestion.isFacet());
            mini.setDeprecated(suggestion.isDeprecated());
            results.add(mini);
        }
        return results;
    }

    private String addConceptInTable(Concept concept, int idUser) {
        concept.setNotation(StringUtils.defaultString(concept.getNotation()));
        concept.setIdArk(StringUtils.defaultString(concept.getIdArk()));

        if (concept.getIdConcept() == null) {
            concept.setIdConcept(getNumericConceptId());
        }
        if (StringUtils.isEmpty(concept.getIdConcept())) {
            return null;
        }

        conceptRepository.save(fr.cnrs.opentheso.entites.Concept.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .created(new Date())
                .modified(new Date())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .creator(idUser)
                .conceptType("concept")
                .contributor(idUser)
                .gps(false)
                .idDoi("")
                .idHandle("")
                .build());
        addConceptHistorique(concept, idUser);
        return concept.getIdConcept();
    }

    private String getNumericConceptId() {
        Long idNumerique = conceptRepository.getNextConceptNumericId();
        if (idNumerique == null) {
            return null;
        }
        long nextId = idNumerique;
        String idConcept = String.valueOf(nextId);
        while (!conceptRepository.findByIdConcept(idConcept).isEmpty()) {
            idConcept = String.valueOf(++nextId);
        }
        return idConcept;
    }

    private String addTerm(Term term, String idConcept, int idUser) {
        int idTermNum = termRepository.getMaxInternalId();
        String idTerm;
        do {
            idTerm = String.valueOf(++idTermNum);
        } while (termRepository.findByIdTermAndIdThesaurus(idTerm, term.getIdThesaurus()).isPresent());

        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(term.getLexicalValue()))
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
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
                .action("ADD")
                .modified(LocalDateTime.now())
                .build());
        preferredTermRepository.save(PreferredTerm.builder()
                .idTerm(termSaved.getIdTerm())
                .idThesaurus(term.getIdThesaurus())
                .idConcept(idConcept)
                .build());
        return idTerm;
    }

    private void addLinkHierarchicalRelation(HierarchicalRelationship relationship, int idUser) {
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(relationship.getIdConcept1())
                .idConcept2(relationship.getIdConcept2())
                .role(relationship.getRole())
                .idThesaurus(relationship.getIdThesaurus())
                .build());
        hierarchicalRelationshipHistoriqueRepository.save(HierarchicalRelationshipHistorique.builder()
                .idConcept1(relationship.getIdConcept1())
                .idConcept2(relationship.getIdConcept2())
                .idThesaurus(relationship.getIdThesaurus())
                .modified(new Date())
                .idUser(idUser)
                .action("ADD")
                .role(relationship.getRole())
                .build());
    }

    private void addConceptHistorique(Concept concept, int idUser) {
        conceptHistoriqueRepository.save(ConceptHistorique.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .idGroup(concept.getIdGroup() == null ? "" : concept.getIdGroup())
                .idUser(idUser)
                .modified(new Date())
                .build());
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

    private String normalizeIdLang(String idLang) {
        return switch (idLang) {
            case "en-GB", "en-US" -> "en";
            case "pt-BR", "pt-PT" -> "pt";
            default -> idLang;
        };
    }
}
