package fr.cnrs.opentheso.concept.mapper;

import fr.cnrs.opentheso.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.concept.model.ConceptFicheRows;
import fr.cnrs.opentheso.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.concept.model.ConceptLabelSort;
import fr.cnrs.opentheso.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.concept.model.ConceptUriLabel;
import fr.cnrs.opentheso.concept.policy.ConceptUriBuilder;
import fr.cnrs.opentheso.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.shared.repository.ConceptFullQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
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

    public Optional<ConceptFullSnapshot> assemble(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        return assemble(thesaurusId, conceptId, lang, paging, preferences, applicationBaseUrl, false);
    }

    public Optional<ConceptFullSnapshot> assemble(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl,
            boolean includeCandidates
    ) {
        if (paging == null || StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Optional.empty();
        }
        boolean includeNarrowerCandidates = paging.includePrivateGroups()
                || (preferences != null && preferences.showCandidatesToGuests());
        return conceptFullQueryRepository.loadFiche(
                        conceptId,
                        thesaurusId,
                        lang,
                        paging.includePrivateGroups(),
                        includeCandidates,
                        includeNarrowerCandidates)
                .map(rows -> buildNode(
                        rows, thesaurusId, conceptId, lang, paging, preferences, applicationBaseUrl));
    }

    public List<ConceptHierarchicalRelation> assembleNarrowerRelations(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        if (paging == null || StringUtils.isAnyBlank(thesaurusId, conceptId, lang) || paging.narrowerLimit() <= 0) {
            return Collections.emptyList();
        }
        return loadAlignedNarrowers(
                thesaurusId, conceptId, lang, paging, preferences, applicationBaseUrl);
    }

    private ConceptFullSnapshot buildNode(
            ConceptFicheRows rows,
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        Object[] core = rows.core();
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
        concept.setCreatorName(StringUtils.isBlank(rows.creator()) ? null : rows.creator());
        concept.setContributorName(emptyIfNull(rows.contributors()));

        concept.setPrefLabel(mapPreferredLabel(lang, rows.preferred()));
        concept.setAltLabels(mapAltLabels(lang, rows.altLabels()));
        concept.setHiddenLabels(mapAltLabels(lang, rows.hiddenLabels()));
        concept.setPrefLabelsTraduction(mapPrefTranslations(rows.prefTranslations()));
        concept.setAltLabelTraduction(mapAltTranslations(rows.altTranslations()));
        concept.setHiddenLabelTraduction(mapAltTranslations(rows.hiddenTranslations()));

        mapNotes(concept, rows.notes());
        concept.setBroaders(mapNarrowerRelations(
                rows.broaders(), thesaurusId, preferences, applicationBaseUrl));
        concept.setRelateds(mapNarrowerRelations(
                rows.relateds(), thesaurusId, preferences, applicationBaseUrl));
        concept.setNarrowers(sliceNarrowers(
                rows.narrowers(), thesaurusId, paging, preferences, applicationBaseUrl));

        mapAlignments(concept, rows.alignments());
        concept.setGps(mapGps(rows.gps()));
        concept.setMembres(mapGroupMemberships(rows.groups(), thesaurusId, preferences, applicationBaseUrl));
        concept.setImages(mapImages(rows.images()));
        concept.setReplaces(mapReplacementRelations(
                rows.replaces(), thesaurusId, preferences, applicationBaseUrl));
        concept.setReplacedBy(mapReplacementRelations(
                rows.replacedBy(), thesaurusId, preferences, applicationBaseUrl));
        concept.setFacets(mapFacets(rows.facets()));
        concept.setExternalResources(mapExternalResources(rows.externalResources()));
        concept.setBreadcrumbAncestors(rows.ancestors());
        return concept;
    }

    private ConceptTermLabel mapPreferredLabel(String lang, Object[] row) {
        if (row == null) {
            return null;
        }
        return new ConceptTermLabel(
                lang,
                stringAt(row, 0),
                stringAt(row, 1),
                parseInt(stringAt(row, 2))
        );
    }

    private List<ConceptTermLabel> mapAltLabels(String lang, List<Object[]> source) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : source) {
            labels.add(new ConceptTermLabel(
                    lang,
                    stringAt(row, 0),
                    stringAt(row, 1),
                    parseInt(stringAt(row, 2))
            ));
        }
        Collections.sort(labels);
        return labels;
    }

    private List<ConceptTermLabel> mapPrefTranslations(List<Object[]> source) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : source) {
            labels.add(new ConceptTermLabel(
                    parseInt(stringAt(row, 3)),
                    stringAt(row, 0),
                    stringAt(row, 2),
                    stringAt(row, 1),
                    stringAt(row, 4)
            ));
        }
        return labels;
    }

    private List<ConceptTermLabel> mapAltTranslations(List<Object[]> source) {
        List<ConceptTermLabel> labels = new ArrayList<>();
        for (Object[] row : source) {
            labels.add(new ConceptTermLabel(
                    parseInt(stringAt(row, 3)),
                    stringAt(row, 0),
                    stringAt(row, 2),
                    stringAt(row, 1),
                    ""
            ));
        }
        return labels;
    }

    private void mapNotes(ConceptFullSnapshot concept, List<Object[]> source) {
        List<ConceptSnapshotNote> notes = new ArrayList<>();
        List<ConceptSnapshotNote> definitions = new ArrayList<>();
        List<ConceptSnapshotNote> examples = new ArrayList<>();
        List<ConceptSnapshotNote> editorialNotes = new ArrayList<>();
        List<ConceptSnapshotNote> changeNotes = new ArrayList<>();
        List<ConceptSnapshotNote> scopeNotes = new ArrayList<>();
        List<ConceptSnapshotNote> historyNotes = new ArrayList<>();

        for (Object[] row : source) {
            ConceptSnapshotNote note = toConceptNote(row);
            switch (stringAt(row, 1).trim().toLowerCase(java.util.Locale.ROOT)) {
                case "note", "customnote" -> notes.add(note);
                case "definition" -> definitions.add(note);
                case "example" -> examples.add(note);
                case "editorialnote" -> editorialNotes.add(note);
                case "changenote" -> changeNotes.add(note);
                case "scopenote" -> scopeNotes.add(note);
                case "historynote" -> historyNotes.add(note);
                default -> {
                    // Unknown note type; ignore it rather than fail the snapshot.
                }
            }
        }
        concept.setNotes(notes);
        concept.setDefinitions(definitions);
        concept.setExamples(examples);
        concept.setEditorialNotes(editorialNotes);
        concept.setChangeNotes(changeNotes);
        concept.setScopeNotes(scopeNotes);
        concept.setHistoryNotes(historyNotes);
    }

    private ConceptSnapshotNote toConceptNote(Object[] row) {
        return new ConceptSnapshotNote(
                parseInt(stringAt(row, 0)),
                stringAt(row, 3),
                fr.cnrs.opentheso.utils.StringUtils.normalizeStringForXml(stringAt(row, 2)),
                stringAt(row, 4)
        );
    }

    private List<ConceptHierarchicalRelation> sliceNarrowers(
            List<Object[]> preloaded,
            String thesaurusId,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        return pageNarrowers(new ArrayList<>(preloaded), thesaurusId, paging, preferences, applicationBaseUrl);
    }

    private List<ConceptHierarchicalRelation> loadAlignedNarrowers(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        boolean includeCandidates = paging.includePrivateGroups()
                || (preferences != null && preferences.showCandidatesToGuests());
        List<Object[]> rows = new ArrayList<>(conceptFullQueryRepository.findNarrowerRelations(
                conceptId, thesaurusId, lang, includeCandidates));
        return pageNarrowers(rows, thesaurusId, paging, preferences, applicationBaseUrl);
    }

    private List<ConceptHierarchicalRelation> pageNarrowers(
            List<Object[]> rows,
            String thesaurusId,
            ConceptAssemblePaging paging,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        boolean sortByNotation = preferences != null && preferences.sortByNotation();
        rows.sort((left, right) -> compareTreeChildren(left, right, sortByNotation));
        int from = Math.min(Math.max(paging.narrowerOffset(), 0), rows.size());
        int to = Math.min(from + paging.narrowerLimit(), rows.size());
        return mapNarrowerRelations(
                rows.subList(from, to), thesaurusId, preferences, applicationBaseUrl, true);
    }

    private static int compareTreeChildren(Object[] left, Object[] right, boolean sortByNotation) {
        if (sortByNotation) {
            int byNotation = StringUtils.defaultString(stringAt(left, 6))
                    .compareToIgnoreCase(StringUtils.defaultString(stringAt(right, 6)));
            if (byNotation != 0) {
                return byNotation;
            }
        }
        return ConceptLabelSort.compareLabels(stringAt(left, 0), stringAt(right, 0));
    }

    private List<ConceptHierarchicalRelation> mapNarrowerRelations(
            List<Object[]> rows,
            String thesaurusId,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        return mapNarrowerRelations(rows, thesaurusId, preferences, applicationBaseUrl, false);
    }

    private List<ConceptHierarchicalRelation> mapNarrowerRelations(
            List<Object[]> rows,
            String thesaurusId,
            ThesaurusPreferences preferences,
            String applicationBaseUrl,
            boolean preserveOrder
    ) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<ConceptHierarchicalRelation> ordered = new ArrayList<>();
        Set<ConceptHierarchicalRelation> relations = new LinkedHashSet<>();
        for (Object[] row : rows) {
            String relatedConceptId = stringAt(row, 1);
            ConceptHierarchicalRelation relation = new ConceptHierarchicalRelation(
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
            );
            if (preserveOrder) {
                ordered.add(relation);
            } else {
                relations.add(relation);
            }
        }
        if (preserveOrder) {
            return List.copyOf(ordered);
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
            return List.of();
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

    private void mapAlignments(ConceptFullSnapshot concept, List<Object[]> source) {
        List<ConceptUriLabel> exact = new ArrayList<>();
        List<ConceptUriLabel> close = new ArrayList<>();
        List<ConceptUriLabel> broad = new ArrayList<>();
        List<ConceptUriLabel> related = new ArrayList<>();
        List<ConceptUriLabel> narrow = new ArrayList<>();

        for (Object[] row : source) {
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
                    // Unknown alignment type; ignore it rather than fail the snapshot.
                }
            }
        }
        concept.setExactMatchs(exact);
        concept.setCloseMatchs(close);
        concept.setBroadMatchs(broad);
        concept.setRelatedMatchs(related);
        concept.setNarrowMatchs(narrow);
    }

    private List<ConceptGpsPoint> mapGps(List<Object[]> source) {
        List<ConceptGpsPoint> points = new ArrayList<>();
        for (Object[] row : source) {
            points.add(new ConceptGpsPoint(
                    toDouble(row[0]),
                    toDouble(row[1]),
                    parseInt(stringAt(row, 2))
            ));
        }
        return points;
    }

    private List<ConceptUriLabel> mapGroupMemberships(
            List<Object[]> source,
            String thesaurusId,
            ThesaurusPreferences preferences,
            String applicationBaseUrl
    ) {
        List<ConceptUriLabel> members = new ArrayList<>();
        for (Object[] row : source) {
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
        Collections.sort(members);
        return members;
    }

    private List<ConceptImageItem> mapImages(List<Object[]> source) {
        List<ConceptImageItem> images = new ArrayList<>();
        for (Object[] row : source) {
            images.add(new ConceptImageItem(
                    parseInt(stringAt(row, 0)),
                    imageCaptionPart(stringAt(row, 1)),
                    imageCaptionPart(stringAt(row, 2)),
                    imageCaptionPart(stringAt(row, 4)),
                    stringAt(row, 3)
            ));
        }
        return images;
    }

    private List<ConceptUriLabel> mapFacets(List<Object[]> source) {
        List<ConceptUriLabel> facets = new ArrayList<>();
        for (Object[] row : source) {
            String facetId = stringAt(row, 0);
            if (StringUtils.isBlank(facetId)) {
                continue;
            }
            facets.add(new ConceptUriLabel("", facetId, stringAt(row, 1)));
        }
        Collections.sort(facets);
        return facets;
    }

    private List<ConceptExternalResourceItem> mapExternalResources(List<Object[]> source) {
        List<ConceptExternalResourceItem> resources = new ArrayList<>();
        for (Object[] row : source) {
            String uri = stringAt(row, 0);
            String description = stringAt(row, 1);
            if (StringUtils.isBlank(uri) && StringUtils.isBlank(description)) {
                continue;
            }
            resources.add(new ConceptExternalResourceItem(uri, description));
        }
        resources.sort(Comparator.comparing(
                ConceptExternalResourceItem::uri,
                String.CASE_INSENSITIVE_ORDER
        ));
        return resources;
    }

    static String imageCaptionPart(String value) {
        return fr.cnrs.opentheso.utils.SkosText.imageCaptionPart(value);
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
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        return value.toString();
    }

    private static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }
}
