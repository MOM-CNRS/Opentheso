package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.concept.policy.ConceptUriBuilder;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptIdentifiers;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.concept.model.LeftTreeMode;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeKinds;

@Service
@RequiredArgsConstructor
public class ConceptReadService {

    private final ConceptQueryRepository conceptQueryRepository;
    private final ConceptDetailEnrichmentService conceptDetailEnrichmentService;
    private final ConceptTreeConsultationService conceptTreeConsultationService;
    private final ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ApplicationUriService applicationUriService;
    private final AuthenticatedUserSource authenticatedUserSource;

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadRootNodes(String thesaurusId, String lang) {
        return doLoadRootNodes(thesaurusId, lang, LeftTreeMode.CONCEPT, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadRootNodes(String thesaurusId, String lang, LeftTreeMode mode) {
        return doLoadRootNodes(thesaurusId, lang, mode, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadRootNodes(
            String thesaurusId,
            String lang,
            LeftTreeMode mode,
            boolean sortByNotation
    ) {
        return doLoadRootNodes(thesaurusId, lang, mode, sortByNotation);
    }

    private List<ConceptTreeNodeData> doLoadRootNodes(
            String thesaurusId,
            String lang,
            LeftTreeMode mode,
            boolean sortByNotation
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return switch (mode) {
            case CONCEPT -> loadRootConceptNodes(thesaurusId, lang, sortByNotation);
            case COLLECTION, ARBRE -> conceptQueryRepository.findConceptGroups(thesaurusId, lang).stream()
                    .map(row -> ConceptMapper.toGroupNode(row, ConceptTreeNodeKinds.GROUP))
                    .toList();
            case INDEX -> Collections.emptyList();
        };
    }

    private List<ConceptTreeNodeData> loadRootConceptNodes(String thesaurusId, String lang, boolean sortByNotation) {
        boolean authenticated = authenticatedUserSource.isLoggedIn();
        return conceptTreeConsultationService.loadTopConcepts(
                thesaurusId,
                lang,
                sortByNotation,
                authenticated
        );
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang,
            LeftTreeMode mode
    ) {
        return doLoadChildNodes(parentId, parentType, thesaurusId, lang, mode, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang,
            LeftTreeMode mode,
            boolean sortByNotation
    ) {
        return doLoadChildNodes(parentId, parentType, thesaurusId, lang, mode, sortByNotation);
    }

    private List<ConceptTreeNodeData> doLoadChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang,
            LeftTreeMode mode,
            boolean sortByNotation
    ) {
        if (StringUtils.isAnyBlank(parentId, thesaurusId)) {
            return Collections.emptyList();
        }
        if (ConceptTreeNodeKinds.GROUP.equals(parentType) || ConceptTreeNodeKinds.SUB_GROUP.equals(parentType)) {
            return loadGroupChildren(parentId, thesaurusId, lang, mode);
        }
        if ((mode == LeftTreeMode.CONCEPT || mode == LeftTreeMode.ARBRE)
                && (ConceptTreeNodeKinds.FACET.equals(parentType) || isConceptNodeType(parentType))) {
            return conceptTreeConsultationService.loadConceptTreeChildren(
                    thesaurusId,
                    parentId,
                    parentType,
                    lang,
                    sortByNotation,
                    authenticatedUserSource.isLoggedIn()
            );
        }
        return conceptQueryRepository.findChildConcepts(parentId, thesaurusId, lang).stream()
                .map(ConceptMapper::toConceptNode)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang
    ) {
        return doLoadChildNodes(parentId, parentType, thesaurusId, lang, LeftTreeMode.CONCEPT, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadTreeRootNodes(String thesaurusId, String lang) {
        return doLoadTreeRootNodes(thesaurusId, lang, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadTreeRootNodes(String thesaurusId, String lang, boolean sortByNotation) {
        return doLoadTreeRootNodes(thesaurusId, lang, sortByNotation);
    }

    private List<ConceptTreeNodeData> doLoadTreeRootNodes(String thesaurusId, String lang, boolean sortByNotation) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        var nodes = new ArrayList<ConceptTreeNodeData>();
        for (var row : conceptQueryRepository.findTreeRootConcepts(thesaurusId, lang)) {
            nodes.add(toThesaurusTreeNode(row));
        }
        return conceptTreeConsultationService.sortNodes(nodes, sortByNotation);
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadTreeChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang
    ) {
        return doLoadTreeChildNodes(parentId, parentType, thesaurusId, lang, isSortByNotation(thesaurusId, lang));
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadTreeChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang,
            boolean sortByNotation
    ) {
        return doLoadTreeChildNodes(parentId, parentType, thesaurusId, lang, sortByNotation);
    }

    private List<ConceptTreeNodeData> doLoadTreeChildNodes(
            String parentId,
            String parentType,
            String thesaurusId,
            String lang,
            boolean sortByNotation
    ) {
        if (StringUtils.isAnyBlank(parentId, thesaurusId)) {
            return Collections.emptyList();
        }
        if (ConceptTreeNodeKinds.FACET.equals(parentType)) {
            var members = new ArrayList<ConceptTreeNodeData>();
            for (var row : conceptQueryRepository.findFacetMembersForTree(thesaurusId, parentId, lang)) {
                members.add(toThesaurusTreeNode(row));
            }
            return conceptTreeConsultationService.sortNodes(members, sortByNotation);
        }
        var nodes = new ArrayList<ConceptTreeNodeData>();
        for (var row : conceptQueryRepository.findTreeChildConcepts(thesaurusId, parentId, lang)) {
            nodes.add(toThesaurusTreeNode(row));
        }
        for (var facet : conceptQueryRepository.findFacetsOfConceptForTree(thesaurusId, parentId, lang)) {
            nodes.add(new ConceptTreeNodeData(
                    facet.facetId(),
                    StringUtils.isBlank(facet.label()) ? "(" + facet.facetId() + ")" : facet.label(),
                    "",
                    ConceptTreeNodeKinds.FACET,
                    facet.hasMembers()
            ));
        }
        return conceptTreeConsultationService.sortNodes(nodes, sortByNotation);
    }

    @Transactional(readOnly = true)
    public List<Object[]> loadCandidateMeta(String thesaurusId, List<String> conceptIds) {
        return conceptQueryRepository.findCandidateMeta(thesaurusId, conceptIds);
    }

    private ConceptTreeNodeData toThesaurusTreeNode(ConceptTreeRow row) {
        String status = StringUtils.trimToEmpty(row.status());
        String nodeType;
        if ("REJ".equalsIgnoreCase(status)) {
            nodeType = "rejete";
        } else if ("CA".equalsIgnoreCase(status)) {
            nodeType = "candidat";
        } else if ("INS".equalsIgnoreCase(status)) {
            nodeType = "insere";
        } else if (ConceptStatusPolicy.isDeprecated(status)) {
            nodeType = "deprecated";
        } else {
            nodeType = row.hasChildren() ? "concept" : "file";
        }
        return new ConceptTreeNodeData(
                row.conceptId(),
                StringUtils.isBlank(row.label()) ? "(" + row.conceptId() + ")" : row.label(),
                StringUtils.defaultString(row.notation()),
                nodeType,
                row.hasChildren()
        );
    }

    @Transactional(readOnly = true)
    public int countSubtreeConcepts(String thesaurusId, String conceptId, String nodeType) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return 0;
        }
        if (ConceptTreeNodeKinds.GROUP.equals(nodeType) || ConceptTreeNodeKinds.SUB_GROUP.equals(nodeType)) {
            return conceptQueryRepository.countConceptsInGroup(thesaurusId, conceptId);
        }
        return 1 + conceptQueryRepository.countDescendantConcepts(thesaurusId, conceptId);
    }

    private List<ConceptTreeNodeData> loadGroupChildren(
            String parentId,
            String thesaurusId,
            String lang,
            LeftTreeMode mode
    ) {
        var children = new ArrayList<ConceptTreeNodeData>();
        conceptQueryRepository.findChildConceptGroups(parentId, thesaurusId, lang).stream()
                .map(row -> ConceptMapper.toGroupNode(row, ConceptTreeNodeKinds.SUB_GROUP))
                .forEach(children::add);
        if (mode == LeftTreeMode.COLLECTION) {
            var groupConcepts = conceptQueryRepository.findConceptsOfGroup(parentId, thesaurusId, lang);
            int limit = Math.min(groupConcepts.size(), 4000);
            for (int i = 0; i < limit; i++) {
                children.add(ConceptMapper.toCollectionConceptNode(groupConcepts.get(i)));
            }
            // Legacy TreeGroups : au-delà de 4000, un nœud "...." coupe l'affichage
            if (groupConcepts.size() > 4000) {
                children.add(new ConceptTreeNodeData("....", "....", "", "file", false));
            }
        } else {
            conceptQueryRepository.findTopConceptsOfGroup(parentId, thesaurusId, lang).stream()
                    .map(ConceptMapper::toConceptNode)
                    .forEach(children::add);
        }
        return List.copyOf(children);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptSummary> loadSummary(String thesaurusId, String conceptId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return Optional.empty();
        }
        return conceptQueryRepository.findConceptHeader(conceptId, thesaurusId, lang)
                .map(ConceptMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptDetail> loadDetail(String thesaurusId, String conceptId, String lang) {
        return doLoadDetailWithSource(thesaurusId, conceptId, lang, false)
                .map(ConceptDetailLoadResult::detail);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptDetail> loadDetail(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates
    ) {
        return doLoadDetailWithSource(thesaurusId, conceptId, lang, includeCandidates)
                .map(ConceptDetailLoadResult::detail);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptDetailLoadResult> loadDetailWithSource(String thesaurusId, String conceptId, String lang) {
        return doLoadDetailWithSource(thesaurusId, conceptId, lang, false);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptDetailLoadResult> loadDetailWithSource(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates
    ) {
        return doLoadDetailWithSource(thesaurusId, conceptId, lang, includeCandidates);
    }

    private Optional<ConceptDetailLoadResult> doLoadDetailWithSource(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Optional.empty();
        }
        boolean authenticated = authenticatedUserSource.isLoggedIn();
        return conceptDetailEnrichmentService.loadFullConcept(
                        thesaurusId, conceptId, lang, authenticated, includeCandidates)
                .map(fullConcept -> new ConceptDetailLoadResult(
                        buildDetailFromFullConcept(fullConcept, thesaurusId, conceptId, lang),
                        fullConcept
                ));
    }

    public ConceptDetail buildDetailFromFullConcept(
            ConceptFullSnapshot fullConcept,
            String thesaurusId,
            String conceptId,
            String lang
    ) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, lang);
        List<BreadcrumbStep> breadcrumb = List.of();
        List<List<BreadcrumbStep>> paths = List.of();
        if (preferences != null && preferences.breadcrumb()) {
            paths = conceptBreadcrumbReadService.loadBreadcrumbPaths(thesaurusId, conceptId, lang);
            breadcrumb = paths.isEmpty() ? List.of() : List.copyOf(paths.get(0));
        }
        List<BreadcrumbStep> flatBreadcrumb = breadcrumb;

        ConceptDetail base = ConceptMapper.toDetailFromFullConcept(fullConcept, thesaurusId, lang, flatBreadcrumb);
        String preferredTermId = resolvePreferredTermId(fullConcept, thesaurusId, conceptId, lang);
        ConceptDetailEnrichmentService.ConceptExportIds exportIds =
                conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences);
        List<ConceptCustomRelationItem> customRelations = preferences != null && preferences.useCustomRelation()
                ? conceptDetailEnrichmentService.loadCustomRelations(thesaurusId, conceptId, lang)
                : List.of();

        return new ConceptDetail(
                base.summary(),
                flatBreadcrumb,
                base.broaderTerms(),
                base.narrowerTerms(),
                base.relatedTerms(),
                base.synonyms(),
                base.hiddenSynonyms(),
                base.translations(),
                base.notes(),
                base.alignmentGroups(),
                base.collections(),
                base.facets(),
                base.replacedBy(),
                base.replaces(),
                paths.isEmpty() ? List.of(List.copyOf(flatBreadcrumb)) : paths,
                conceptDetailEnrichmentService.mapImages(fullConcept),
                conceptDetailEnrichmentService.mapGpsPoints(fullConcept),
                conceptDetailEnrichmentService.mapExternalResources(fullConcept),
                customRelations,
                List.of(),
                buildIdentifiers(
                        thesaurusId,
                        conceptId,
                        lang,
                        base.summary(),
                        preferences,
                        exportIds,
                        fullConcept.getUri()
                ),
                conceptDetailEnrichmentService.mapContributors(fullConcept),
                preferredTermId
        );
    }

    @Transactional(readOnly = true)
    public boolean hasActiveCorpus(String thesaurusId) {
        return conceptDetailEnrichmentService.hasActiveCorpus(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptCorpusLinkItem> loadCorpusLinks(String thesaurusId, CorpusSearchContext context) {
        return conceptDetailEnrichmentService.loadCorpusLinks(thesaurusId, context);
    }

    public CorpusSearchContext toCorpusSearchContext(ConceptSummary summary) {
        if (summary == null) {
            return null;
        }
        return new CorpusSearchContext(
                summary.conceptId(),
                summary.arkId(),
                summary.preferredLabel()
        );
    }

    public record ConceptDetailLoadResult(ConceptDetail detail, ConceptFullSnapshot fullConcept) {
    }

    @Transactional(readOnly = true)
    public List<BreadcrumbStep> loadBreadcrumb(String thesaurusId, String conceptId, String lang) {
        List<List<BreadcrumbStep>> paths = doLoadBreadcrumbPaths(thesaurusId, conceptId, lang);
        if (!paths.isEmpty()) {
            return paths.get(0);
        }
        return ConceptMapper.toBreadcrumb(
                conceptQueryRepository.findBreadcrumb(conceptId, thesaurusId, lang)
        );
    }

    @Transactional(readOnly = true)
    public List<List<BreadcrumbStep>> loadBreadcrumbPaths(String thesaurusId, String conceptId, String lang) {
        return doLoadBreadcrumbPaths(thesaurusId, conceptId, lang);
    }

    private List<List<BreadcrumbStep>> doLoadBreadcrumbPaths(String thesaurusId, String conceptId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Collections.emptyList();
        }
        return conceptBreadcrumbReadService.loadBreadcrumbPaths(thesaurusId, conceptId, lang);
    }

    @Transactional(readOnly = true)
    public Optional<GroupDetailOverview> loadGroupDetail(String thesaurusId, String groupId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, groupId)) {
            return Optional.empty();
        }
        return loadGroupDetailFromRepository(thesaurusId, groupId, lang);
    }

    private Optional<GroupDetailOverview> loadGroupDetailFromRepository(
            String thesaurusId,
            String groupId,
            String lang
    ) {
        return conceptQueryRepository.findGroupHeader(groupId, thesaurusId, lang)
                .map(header -> {
                    String typeCode = ConceptMapper.stringAt(header, 5);
                    var type = conceptQueryRepository.findGroupType(typeCode);
                    return new GroupDetailOverview(
                            ConceptMapper.stringAt(header, 0),
                            ConceptMapper.stringAt(header, 1),
                            lang,
                            typeCode,
                            type.map(row -> ConceptMapper.stringAt(row, 0)).orElse(""),
                            type.map(row -> ConceptMapper.stringAt(row, 1)).orElse(""),
                            conceptQueryRepository.countConceptsInGroup(thesaurusId, groupId),
                            ConceptMapper.stringAt(header, 2),
                            ConceptMapper.stringAt(header, 3),
                            ConceptMapper.stringAt(header, 4),
                            conceptQueryRepository.findGroupTranslations(groupId, thesaurusId, lang).stream()
                                    .map(row -> new GroupTranslationItem(
                                            ConceptMapper.stringAt(row, 0),
                                            ConceptMapper.stringAt(row, 1)
                                    ))
                                    .toList(),
                            conceptQueryRepository.findNotesByIdentifier(groupId, thesaurusId, lang).stream()
                                    .map(ConceptMapper::toNote)
                                    .toList(),
                            loadGroupMembers(thesaurusId, groupId, lang)
                    );
                });
    }

    private List<fr.cnrs.opentheso.v2.concept.model.FacetMemberItem> loadGroupMembers(
            String thesaurusId,
            String groupId,
            String lang
    ) {
        return conceptQueryRepository.findConceptsOfGroup(groupId, thesaurusId, lang).stream()
                .map(row -> new fr.cnrs.opentheso.v2.concept.model.FacetMemberItem(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 2)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FacetDetailOverview> loadFacetDetail(String thesaurusId, String facetId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, facetId)) {
            return Optional.empty();
        }
        return loadFacetDetailFromRepository(thesaurusId, facetId, lang);
    }

    private Optional<FacetDetailOverview> loadFacetDetailFromRepository(
            String thesaurusId,
            String facetId,
            String lang
    ) {
        return conceptQueryRepository.findFacetHeader(facetId, thesaurusId, lang)
                .map(header -> {
                    String parentId = ConceptMapper.stringAt(header, 1);
                    String parentLabel = conceptQueryRepository.findConceptHeader(parentId, thesaurusId, lang)
                            .map(row -> row.prefLabel())
                            .filter(StringUtils::isNotBlank)
                            .orElse(parentId);
                    return new FacetDetailOverview(
                            ConceptMapper.stringAt(header, 0),
                            ConceptMapper.stringAt(header, 2),
                            lang,
                            parentId,
                            parentLabel,
                            conceptQueryRepository.findFacetMembers(facetId, thesaurusId, lang).stream()
                                    .map(row -> new FacetMemberItem(row.conceptId(), row.label()))
                                    .toList(),
                            conceptQueryRepository.findFacetTranslations(facetId, thesaurusId, lang).stream()
                                    .map(row -> new GroupTranslationItem(
                                            ConceptMapper.stringAt(row, 0),
                                            ConceptMapper.stringAt(row, 1)
                                    ))
                                    .toList(),
                            conceptQueryRepository.findNotesByIdentifier(facetId, thesaurusId, lang).stream()
                                    .map(ConceptMapper::toNote)
                                    .toList()
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> searchByLabel(String thesaurusId, String lang, String query, int limit) {
        if (StringUtils.isAnyBlank(thesaurusId, query)) {
            return Collections.emptyList();
        }
        return conceptQueryRepository.searchByLabel(thesaurusId, lang, query.trim(), limit).stream()
                .map(ConceptMapper::toConceptNode)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> searchIndex(
            String thesaurusId,
            String lang,
            String query,
            boolean permuted,
            boolean withAltLabel,
            int limit
    ) {
        return doSearchIndex(thesaurusId, lang, query, permuted, withAltLabel, limit);
    }

    private List<ConceptTreeNodeData> doSearchIndex(
            String thesaurusId,
            String lang,
            String query,
            boolean permuted,
            boolean withAltLabel,
            int limit
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, query)) {
            return Collections.emptyList();
        }
        var results = searchIndexFromRepository(thesaurusId, lang, query.trim(), permuted, withAltLabel);
        if (results.size() <= limit) {
            return results;
        }
        return List.copyOf(results.subList(0, limit));
    }

    private List<ConceptTreeNodeData> searchIndexFromRepository(
            String thesaurusId,
            String lang,
            String query,
            boolean permuted,
            boolean withAltLabel
    ) {
        var rows = new ArrayList<Object[]>();
        if (withAltLabel) {
            rows.addAll(permuted
                    ? conceptQueryRepository.searchIndexSynonymsPermuted(thesaurusId, lang, query)
                    : conceptQueryRepository.searchIndexSynonymsPrefix(thesaurusId, lang, query));
        } else {
            rows.addAll(permuted
                    ? conceptQueryRepository.searchIndexPreferredPermuted(thesaurusId, lang, query)
                    : conceptQueryRepository.searchIndexPreferredPrefix(thesaurusId, lang, query));
        }
        var seen = new java.util.LinkedHashSet<String>();
        return rows.stream()
                .map(ConceptMapper::toIndexSearchNode)
                .filter(node -> seen.add(node.nodeId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> searchIndex(String thesaurusId, String lang, String query, int limit) {
        return doSearchIndex(thesaurusId, lang, query, false, false, limit);
    }

    private String resolvePreferredTermId(
            ConceptFullSnapshot fullConcept,
            String thesaurusId,
            String conceptId,
            String lang
    ) {
        if (fullConcept.getPrefLabel() != null && StringUtils.isNotBlank(fullConcept.getPrefLabel().getIdTerm())) {
            return fullConcept.getPrefLabel().getIdTerm();
        }
        return conceptQueryRepository.findPreferredTermId(conceptId, thesaurusId, lang).orElse("");
    }

    private ConceptIdentifiers buildIdentifiers(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptSummary summary,
            ThesaurusPreferences preferences,
            ConceptDetailEnrichmentService.ConceptExportIds exportIds,
            String conceptUri
    ) {
        String baseUrl = applicationUriService.resolveApplicationBaseUrl();
        String internalPermalink = baseUrl + "/?idc=" + conceptId + "&idt=" + thesaurusId;
        String originalUri = StringUtils.isNotBlank(conceptUri)
                ? conceptUri
                : ConceptUriBuilder.buildConceptUri(
                        preferences,
                        baseUrl,
                        conceptId,
                        thesaurusId,
                        exportIds.arkId(),
                        exportIds.handleId(),
                        exportIds.doiId()
                );
        String permanentId = ConceptUriBuilder.resolvePermanentId(
                preferences,
                exportIds.arkId(),
                exportIds.handleId(),
                exportIds.doiId()
        );
        if (StringUtils.isBlank(permanentId)) {
            permanentId = StringUtils.defaultString(summary.arkId());
        }
        return buildIdentifiersValues(preferences, internalPermalink, originalUri, permanentId);
    }

    private ConceptIdentifiers buildIdentifiersValues(
            ThesaurusPreferences preferences,
            String internalPermalink,
            String originalUri,
            String permanentId
    ) {
        boolean showOriginalUri = preferences != null && (
                preferences.exportUriType() == ExportUriType.ARK
                        || preferences.exportUriType() == ExportUriType.HANDLE
                        || preferences.exportUriType() == ExportUriType.DOI
                        || StringUtils.isNotBlank(originalUri)
        );
        String qrCodeValue = "";
        if (preferences != null && StringUtils.isNotBlank(preferences.originalUri()) && StringUtils.isNotBlank(permanentId)) {
            qrCodeValue = preferences.originalUri().replaceAll("/$", "") + "/" + permanentId.replaceAll("^/", "");
        }
        return new ConceptIdentifiers(
                internalPermalink,
                originalUri,
                permanentId,
                qrCodeValue,
                showOriginalUri,
                StringUtils.isNotBlank(qrCodeValue)
        );
    }

    private boolean isConceptNodeType(String parentType) {
        return "concept".equals(parentType)
                || "file".equals(parentType)
                || "deprecated".equals(parentType)
                || "facetMember".equals(parentType);
    }

    private boolean isSortByNotation(String thesaurusId, String lang) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, lang);
        return preferences != null && preferences.sortByNotation();
    }

    @Transactional(readOnly = true)
    public int countBranchConcepts(String thesaurusId, String conceptId) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return 0;
        }
        return conceptQueryRepository.countBranchConcepts(thesaurusId, conceptId);
    }
}
