package fr.cnrs.opentheso.v2.concept.mapper;

import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptUriLabel;
import fr.cnrs.opentheso.v2.concept.policy.ConceptUriBuilder;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.shared.repository.ConceptFullQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ConceptFullAssembler {

    private final ConceptFullQueryRepository conceptFullQueryRepository;
    private final ExternalResourcesRepository externalResourcesRepository;

    public Optional<ConceptFullSnapshot> assemble(
            String thesaurusId,
            String conceptId,
            String lang,
            int narrowerOffset,
            int narrowerLimit,
            boolean includePrivateGroups,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Optional.empty();
        }
        return conceptFullQueryRepository.findConceptCore(conceptId, thesaurusId)
                .map(core -> buildNode(
                        core,
                        thesaurusId,
                        conceptId,
                        lang,
                        narrowerOffset,
                        narrowerLimit,
                        includePrivateGroups,
                        preferences,
                        applicationBaseUrl
                ));
    }

    public List<ConceptHierarchicalRelation> assembleNarrowerRelations(
            String thesaurusId,
            String conceptId,
            String lang,
            int offset,
            int limit,
            boolean includePrivateGroups,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang) || limit <= 0) {
            return Collections.emptyList();
        }
        List<ConceptHierarchicalRelation> relations = mapNarrowerRelations(
                conceptFullQueryRepository.findNarrowerRelations(
                        conceptId,
                        thesaurusId,
                        lang,
                        includePrivateGroups,
                        offset,
                        limit
                ),
                thesaurusId,
                preferences,
                applicationBaseUrl
        );
        return relations != null ? relations : Collections.emptyList();
    }

    private ConceptFullSnapshot buildNode(
            Object[] core,
            String thesaurusId,
            String conceptId,
            String lang,
            int narrowerOffset,
            int narrowerLimit,
            boolean includePrivateGroups,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        String status = stringAt(core, 1);
        String arkId = stringAt(core, 2);
        String handleId = stringAt(core, 3);
        String doiId = stringAt(core, 4);

        ConceptFullSnapshot concept = new ConceptFullSnapshot();
        concept.setUri(ConceptUriBuilder.buildConceptUri(
                preferences, applicationBaseUrl, conceptId, thesaurusId, arkId, handleId, doiId
        ));
        concept.setResourceType(ConceptResourceStatus.CONCEPT_RESOURCE_TYPE);
        concept.setResourceStatus(ConceptResourceStatus.fromDbStatus(status));
        concept.setConceptType(stringAt(core, 8));
        concept.setIdentifier(conceptId);
        concept.setPermanentId(ConceptUriBuilder.resolvePermanentId(preferences, arkId, handleId, doiId));
        concept.setNotation(stringAt(core, 5));
        concept.setCreated(formatDate(core[6]));
        concept.setModified(formatDate(core[7]));
        concept.setCreatorName(preferences != null && preferences.displayUserName()
                ? conceptFullQueryRepository.findCreator(conceptId, thesaurusId).orElse(null)
                : null);
        concept.setContributorName(preferences != null && preferences.displayUserName()
                ? nullIfEmpty(conceptFullQueryRepository.findContributors(conceptId, thesaurusId))
                : null);

        concept.setPrefLabel(mapPreferredLabel(conceptId, thesaurusId, lang));
        concept.setAltLabels(mapAltLabels(conceptId, thesaurusId, lang, false));
        concept.setHiddenLabels(mapAltLabels(conceptId, thesaurusId, lang, true));
        concept.setPrefLabelsTraduction(mapPrefTranslations(conceptId, thesaurusId, lang));
        concept.setAltLabelTraduction(mapAltTranslations(conceptId, thesaurusId, lang, false));
        concept.setHiddenLabelTraduction(mapAltTranslations(conceptId, thesaurusId, lang, true));

        mapNotes(concept, conceptId, thesaurusId);
        concept.setBroaders(mapNarrowerRelations(
                conceptFullQueryRepository.findBroaderRelations(conceptId, thesaurusId, lang, includePrivateGroups),
                thesaurusId,
                preferences,
                applicationBaseUrl
        ));
        concept.setRelateds(mapNarrowerRelations(
                conceptFullQueryRepository.findRelatedRelations(conceptId, thesaurusId, lang),
                thesaurusId,
                preferences,
                applicationBaseUrl
        ));
        concept.setNarrowers(mapNarrowerRelations(
                conceptFullQueryRepository.findNarrowerRelations(
                        conceptId,
                        thesaurusId,
                        lang,
                        includePrivateGroups,
                        narrowerOffset,
                        narrowerLimit
                ),
                thesaurusId,
                preferences,
                applicationBaseUrl
        ));

        mapAlignments(concept, conceptId, thesaurusId);
        concept.setGps(mapGps(conceptId, thesaurusId));
        concept.setMembres(mapGroupMemberships(conceptId, thesaurusId, lang, preferences, applicationBaseUrl));
        concept.setImages(mapImages(conceptId, thesaurusId));
        concept.setReplaces(mapReplacementRelations(
                conceptFullQueryRepository.findReplaces(conceptId, thesaurusId, lang),
                thesaurusId,
                preferences,
                applicationBaseUrl
        ));
        concept.setReplacedBy(mapReplacementRelations(
                conceptFullQueryRepository.findReplacedBy(conceptId, thesaurusId, lang),
                thesaurusId,
                preferences,
                applicationBaseUrl
        ));
        concept.setFacets(mapFacets(conceptId, thesaurusId, lang));
        concept.setExternalResources(mapExternalResources(conceptId, thesaurusId));
        return concept;
    }

    private ConceptTermLabel mapPreferredLabel(String conceptId, String thesaurusId, String lang) {
        return conceptFullQueryRepository.findPreferredLabel(conceptId, thesaurusId, lang)
                .map(row -> new ConceptTermLabel(
                        lang,
                        stringAt(row, 0),
                        stringAt(row, 1),
                        parseInt(stringAt(row, 2))
                ))
                .orElse(null);
    }

    private List<ConceptTermLabel> mapAltLabels(String conceptId, String thesaurusId, String lang, boolean hidden) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findAltLabels(conceptId, thesaurusId, lang, hidden)) {
            labels.add(new ConceptTermLabel(
                    lang,
                    stringAt(row, 0),
                    stringAt(row, 1),
                    parseInt(stringAt(row, 2))
            ));
        }
        if (labels.isEmpty()) {
            return null;
        }
        Collections.sort(labels);
        return labels;
    }

    private List<ConceptTermLabel> mapPrefTranslations(String conceptId, String thesaurusId, String lang) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findPrefLabelTranslations(conceptId, thesaurusId, lang)) {
            labels.add(new ConceptTermLabel(
                    parseInt(stringAt(row, 3)),
                    stringAt(row, 0),
                    stringAt(row, 2),
                    stringAt(row, 1),
                    stringAt(row, 4)
            ));
        }
        return labels.isEmpty() ? null : labels;
    }

    private List<ConceptTermLabel> mapAltTranslations(String conceptId, String thesaurusId, String lang, boolean hidden) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findAltLabelTranslations(conceptId, thesaurusId, lang, hidden)) {
            labels.add(new ConceptTermLabel(
                    parseInt(stringAt(row, 3)),
                    stringAt(row, 0),
                    stringAt(row, 2),
                    stringAt(row, 1),
                    ""
            ));
        }
        return labels.isEmpty() ? null : labels;
    }

    private void mapNotes(ConceptFullSnapshot concept, String conceptId, String thesaurusId) {
        List<ConceptSnapshotNote> notes = new ArrayList<>();
        List<ConceptSnapshotNote> definitions = new ArrayList<>();
        List<ConceptSnapshotNote> examples = new ArrayList<>();
        List<ConceptSnapshotNote> editorialNotes = new ArrayList<>();
        List<ConceptSnapshotNote> changeNotes = new ArrayList<>();
        List<ConceptSnapshotNote> scopeNotes = new ArrayList<>();
        List<ConceptSnapshotNote> historyNotes = new ArrayList<>();

        for (Object[] row : conceptFullQueryRepository.findAllConceptNotes(conceptId, thesaurusId)) {
            ConceptSnapshotNote note = toConceptNote(row);
            switch (stringAt(row, 1)) {
                case "note" -> notes.add(note);
                case "definition" -> definitions.add(note);
                case "example" -> examples.add(note);
                case "editorialNote" -> editorialNotes.add(note);
                case "changeNote" -> changeNotes.add(note);
                case "scopeNote" -> scopeNotes.add(note);
                case "historyNote" -> historyNotes.add(note);
                default -> {
                }
            }
        }
        concept.setNotes(nullIfEmpty(notes));
        concept.setDefinitions(nullIfEmpty(definitions));
        concept.setExamples(nullIfEmpty(examples));
        concept.setEditorialNotes(nullIfEmpty(editorialNotes));
        concept.setChangeNotes(nullIfEmpty(changeNotes));
        concept.setScopeNotes(nullIfEmpty(scopeNotes));
        concept.setHistoryNotes(nullIfEmpty(historyNotes));
    }

    private ConceptSnapshotNote toConceptNote(Object[] row) {
        return new ConceptSnapshotNote(
                parseInt(stringAt(row, 0)),
                stringAt(row, 3),
                fr.cnrs.opentheso.utils.StringUtils.normalizeStringForXml(stringAt(row, 2)),
                stringAt(row, 4)
        );
    }

    private List<ConceptHierarchicalRelation> mapNarrowerRelations(
            List<Object[]> rows,
            String thesaurusId,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        if (rows.isEmpty()) {
            return null;
        }
        Set<ConceptHierarchicalRelation> relations = new LinkedHashSet<>();
        for (Object[] row : rows) {
            String relatedConceptId = stringAt(row, 1);
            relations.add(new ConceptHierarchicalRelation(
                    ConceptUriBuilder.buildConceptUri(
                            preferences,
                            applicationBaseUrl,
                            relatedConceptId,
                            thesaurusId,
                            stringAt(row, 3),
                            stringAt(row, 4),
                            stringAt(row, 5)
                    ),
                    relatedConceptId,
                    stringAt(row, 0),
                    stringAt(row, 2)
            ));
        }
        List<ConceptHierarchicalRelation> sorted = new ArrayList<>(relations);
        Collections.sort(sorted);
        return sorted;
    }

    private List<ConceptUriLabel> mapReplacementRelations(
            List<Object[]> rows,
            String thesaurusId,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        if (rows.isEmpty()) {
            return null;
        }
        List<ConceptUriLabel> items = new ArrayList<>();
        for (Object[] row : rows) {
            String relatedConceptId = stringAt(row, 0);
            items.add(new ConceptUriLabel(
                    ConceptUriBuilder.buildConceptUri(
                            preferences,
                            applicationBaseUrl,
                            relatedConceptId,
                            thesaurusId,
                            stringAt(row, 1),
                            stringAt(row, 2),
                            stringAt(row, 3)
                    ),
                    relatedConceptId,
                    stringAt(row, 4)
            ));
        }
        Collections.sort(items);
        return items;
    }

    private void mapAlignments(ConceptFullSnapshot concept, String conceptId, String thesaurusId) {
        List<ConceptUriLabel> exact = new ArrayList<>();
        List<ConceptUriLabel> close = new ArrayList<>();
        List<ConceptUriLabel> broad = new ArrayList<>();
        List<ConceptUriLabel> related = new ArrayList<>();
        List<ConceptUriLabel> narrow = new ArrayList<>();

        for (Object[] row : conceptFullQueryRepository.findAlignments(conceptId, thesaurusId)) {
            ConceptUriLabel item = new ConceptUriLabel(
                    stringAt(row, 1),
                    stringAt(row, 0),
                    stringAt(row, 3)
            );
            int type = parseInt(stringAt(row, 2));
            switch (type) {
                case 1 -> exact.add(item);
                case 2 -> close.add(item);
                case 3 -> broad.add(item);
                case 4 -> related.add(item);
                case 5 -> narrow.add(item);
                default -> {
                }
            }
        }
        concept.setExactMatchs(nullIfEmpty(exact));
        concept.setCloseMatchs(nullIfEmpty(close));
        concept.setBroadMatchs(nullIfEmpty(broad));
        concept.setRelatedMatchs(nullIfEmpty(related));
        concept.setNarrowMatchs(nullIfEmpty(narrow));
    }

    private List<ConceptGpsPoint> mapGps(String conceptId, String thesaurusId) {
        List<ConceptGpsPoint> points = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findGpsPoints(conceptId, thesaurusId)) {
            points.add(new ConceptGpsPoint(
                    toDouble(row[0]),
                    toDouble(row[1]),
                    parseInt(stringAt(row, 2))
            ));
        }
        return points.isEmpty() ? null : points;
    }

    private List<ConceptUriLabel> mapGroupMemberships(
            String conceptId,
            String thesaurusId,
            String lang,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        List<ConceptUriLabel> members = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findGroupMemberships(conceptId, thesaurusId, lang)) {
            String groupId = stringAt(row, 0);
            members.add(new ConceptUriLabel(
                    ConceptUriBuilder.buildGroupUri(
                            preferences,
                            applicationBaseUrl,
                            groupId,
                            thesaurusId,
                            stringAt(row, 1),
                            stringAt(row, 2)
                    ),
                    groupId,
                    stringAt(row, 4)
            ));
        }
        if (members.isEmpty()) {
            return null;
        }
        Collections.sort(members);
        return members;
    }

    private List<ConceptImageItem> mapImages(String conceptId, String thesaurusId) {
        List<ConceptImageItem> images = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findImages(conceptId, thesaurusId)) {
            String imageName = stringAt(row, 1);
            images.add(new ConceptImageItem(
                    parseInt(stringAt(row, 0)),
                    "null".equalsIgnoreCase(imageName) ? "" : imageName,
                    stringAt(row, 2),
                    stringAt(row, 4),
                    stringAt(row, 3)
            ));
        }
        return images.isEmpty() ? null : images;
    }

    private List<ConceptUriLabel> mapFacets(String conceptId, String thesaurusId, String lang) {
        List<ConceptUriLabel> facets = new ArrayList<>();
        for (Object[] row : conceptFullQueryRepository.findFacets(conceptId, thesaurusId, lang)) {
            String facetId = stringAt(row, 0);
            if (StringUtils.isBlank(facetId)) {
                continue;
            }
            facets.add(new ConceptUriLabel("", facetId, stringAt(row, 1)));
        }
        if (facets.isEmpty()) {
            return null;
        }
        Collections.sort(facets);
        return facets;
    }

    private List<ConceptExternalResourceItem> mapExternalResources(String conceptId, String thesaurusId) {
        List<ExternalResource> entities = externalResourcesRepository
                .findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return entities.stream()
                .filter(resource -> StringUtils.isNotBlank(resource.getExternalUri())
                        || StringUtils.isNotBlank(resource.getDescription()))
                .sorted(Comparator.comparing(
                        resource -> StringUtils.defaultString(resource.getExternalUri()),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(resource -> new ConceptExternalResourceItem(
                        StringUtils.defaultString(resource.getExternalUri()),
                        StringUtils.defaultString(resource.getDescription())
                ))
                .toList();
    }

    private static String stringAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    private static int parseInt(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static String formatDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toString();
        }
        return value.toString();
    }

    private static <T> List<T> nullIfEmpty(List<T> values) {
        return CollectionUtils.isEmpty(values) ? null : values;
    }
}
