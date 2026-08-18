package fr.cnrs.opentheso.v2.concept.alignment.service;

import fr.cnrs.opentheso.entites.ThesaurusAlignementSource;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.ThesaurusAlignementSourceRepository;
import fr.cnrs.opentheso.v2.candidat.alignment.AlignmentAutoExternalSearch;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentAdminRow;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.support.AlignmentUrlProbe;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptAlignmentAdminService {

    public static final int BRANCH_LIMIT = 2000;

    private final BranchConceptSupport branchConceptSupport;
    private final ConceptSearchQueryRepository conceptSearchQueryRepository;
    private final AlignementRepository alignementRepository;
    private final AlignementSourceRepository alignementSourceRepository;
    private final ThesaurusAlignementSourceRepository thesaurusAlignementSourceRepository;
    private final AlignmentAutoExternalSearch alignmentAutoExternalSearch;
    private final ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final CandidatAutoAlignmentPersistence candidatAutoAlignmentPersistence;
    private final AlignmentPropositionEnricher alignmentPropositionEnricher;

    @Transactional(readOnly = true)
    public List<AlignmentAdminRow> loadBranchSummary(String thesaurusId, String rootConceptId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, rootConceptId, lang)) {
            return List.of();
        }
        List<String> branchIds = limitBranch(branchConceptSupport.collectBranchConceptIds(thesaurusId, rootConceptId));
        if (branchIds.isEmpty()) {
            return List.of();
        }
        Map<String, String> labels = conceptSearchQueryRepository.findPreferredLabelsByIds(branchIds, thesaurusId, lang);
        Map<String, List<AlignmentAdminRow>> alignmentsByConcept = loadAlignmentsGrouped(thesaurusId, branchIds, labels);

        List<AlignmentAdminRow> rows = new ArrayList<>();
        for (String conceptId : branchIds) {
            String label = labels.getOrDefault(conceptId, conceptId);
            List<AlignmentAdminRow> conceptRows = alignmentsByConcept.getOrDefault(conceptId, List.of());
            rows.addAll(conceptRows);
            // Placeholder pour afficher le groupe même sans alignement (comme legacy)
            rows.add(new AlignmentAdminRow(conceptId, label, null, null, null, 0, null, true));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public int countAlignments(List<AlignmentAdminRow> rows) {
        if (rows == null) {
            return 0;
        }
        return (int) rows.stream().filter(row -> !row.isPlaceholder()).count();
    }

    @Transactional(readOnly = true)
    public int countAlignmentsForConcept(List<AlignmentAdminRow> rows, String conceptId) {
        if (rows == null || StringUtils.isBlank(conceptId)) {
            return 0;
        }
        return (int) rows.stream()
                .filter(row -> conceptId.equals(row.conceptId()) && !row.isPlaceholder())
                .count();
    }

    @Transactional
    public int checkUrlsForConcept(String thesaurusId, String conceptId, List<AlignmentAdminRow> rows) {
        if (rows == null || StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return 0;
        }
        int invalid = 0;
        for (AlignmentAdminRow row : rows) {
            if (row.isPlaceholder() || !conceptId.equals(row.conceptId()) || row.alignmentId() == null) {
                continue;
            }
            boolean reachable = AlignmentUrlProbe.isReachable(row.targetUri());
            if (!reachable) {
                invalid++;
            }
            if (reachable != row.urlAvailable()) {
                alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId(
                                thesaurusId, conceptId, row.alignmentId())
                        .ifPresent(alignement -> {
                            alignement.setUrlAvailable(reachable);
                            alignementRepository.save(alignement);
                        });
            }
        }
        return invalid;
    }

    @Transactional(readOnly = true)
    public List<AlignmentSourceItem> listSourcesForManagement(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        Set<Integer> selectedIds = new HashSet<>();
        alignementRepository.findSelectedAlignmentsByThesaurus(thesaurusId)
                .forEach(projection -> selectedIds.add(projection.getId_alignement_source()));

        return alignementSourceRepository.findByIsGlobalTrueOrIdThesaurusOwner(thesaurusId).stream()
                .map(source -> new AlignmentSourceItem(
                        source.getId(),
                        source.getSource(),
                        StringUtils.defaultString(source.getDescription()),
                        selectedIds.contains(source.getId()),
                        Boolean.TRUE.equals(source.getIsGlobal()),
                        StringUtils.defaultString(source.getSourceFilter()),
                        StringUtils.defaultString(source.getRequete()),
                        source.getIdThesaurusOwner()
                ))
                .toList();
    }

    @Transactional
    public void setSourceSelected(String thesaurusId, int sourceId, boolean selected) {
        if (StringUtils.isBlank(thesaurusId) || sourceId <= 0) {
            return;
        }
        if (selected) {
            thesaurusAlignementSourceRepository.save(ThesaurusAlignementSource.builder()
                    .idAlignementSource(sourceId)
                    .idThesaurus(thesaurusId)
                    .build());
        } else {
            thesaurusAlignementSourceRepository.deleteByIdThesaurusAndIdAlignementSource(thesaurusId, sourceId);
        }
    }

    @Transactional
    public boolean deleteLocalSource(int sourceId) {
        try {
            return alignementSourceRepository.deleteByIdAlignementSource(sourceId) > 0;
        } catch (Exception ex) {
            log.error("Erreur lors de la suppression de la source d'alignement {}", sourceId, ex);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<AlignementSource> listActiveSources(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        var projections = alignementSourceRepository.findAllByThesaurus(thesaurusId);
        if (CollectionUtils.isEmpty(projections)) {
            return List.of();
        }
        return projections.stream()
                .map(element -> AlignementSource.builder()
                        .id(element.getId())
                        .source(element.getSource())
                        .requete(element.getRequete())
                        .typeRequete(element.getTypeRequete())
                        .alignement_format(element.getAlignement_format())
                        .description(element.getDescription())
                        .source_filter(element.getSource_filter())
                        .isGps(element.getGps())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public AlignementSource findActiveSource(String thesaurusId, int sourceId) {
        return listActiveSources(thesaurusId).stream()
                .filter(source -> source.getId() == sourceId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Recherche automatique sur la branche (mode propositions), en parallèle.
     * L'enrichissement (traductions / notes / images) est différé à la validation.
     */
    public List<AlignmentProposition> searchPropositions(
            String thesaurusId,
            String lang,
            List<AlignmentAdminRow> summaryRows,
            AlignementSource source
    ) {
        if (source == null || summaryRows == null || StringUtils.isAnyBlank(thesaurusId, lang)) {
            return List.of();
        }
        Map<String, String> concepts = new LinkedHashMap<>();
        for (AlignmentAdminRow row : summaryRows) {
            concepts.putIfAbsent(row.conceptId(), row.conceptLabel());
        }
        if (concepts.isEmpty()) {
            return List.of();
        }

        int poolSize = Math.min(8, Math.max(1, concepts.size()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Callable<List<AlignmentProposition>>> tasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : concepts.entrySet()) {
            String conceptId = entry.getKey();
            String label = entry.getValue();
            if (StringUtils.isBlank(label)) {
                continue;
            }
            tasks.add(() -> searchPropositionsForConcept(thesaurusId, lang, conceptId, label, source));
        }

        List<AlignmentProposition> propositions = new ArrayList<>();
        try {
            List<Future<List<AlignmentProposition>>> futures = executor.invokeAll(tasks);
            for (Future<List<AlignmentProposition>> future : futures) {
                propositions.addAll(future.get());
            }
        } catch (Exception ex) {
            log.error("Recherche automatique d'alignements interrompue", ex);
        } finally {
            executor.shutdownNow();
        }
        return propositions;
    }

    public void enrichProposition(
            AlignmentProposition proposition,
            AlignementSource source,
            String thesaurusId,
            String lang
    ) {
        alignmentPropositionEnricher.enrich(proposition, source, thesaurusId, lang);
    }

    /**
     * Persiste l'alignement choisi et les enrichissements cochés.
     */
    @Transactional
    public boolean acceptProposition(
            String thesaurusId,
            AlignmentProposition proposition,
            int userId,
            String contributorName
    ) {
        if (proposition == null
                || StringUtils.isAnyBlank(thesaurusId, proposition.getConceptId(), proposition.getTargetUri())) {
            return false;
        }

        int typeId = proposition.getAlignmentTypeId() > 0 ? proposition.getAlignmentTypeId() : 1;
        if (proposition.getSourceId() > 0) {
            if (!candidatAutoAlignmentPersistence.addAlignment(
                    userId,
                    proposition.getTargetLabel(),
                    proposition.getSourceName(),
                    proposition.getTargetUri(),
                    typeId,
                    proposition.getConceptId(),
                    thesaurusId,
                    proposition.getSourceId())) {
                return false;
            }
        } else {
            MutationResult alignmentResult = conceptAlignmentMutationService.addManualAlignment(
                    new AddManualAlignmentCommand(
                            thesaurusId,
                            proposition.getConceptId(),
                            typeId,
                            proposition.getTargetUri(),
                            proposition.getSourceName(),
                            userId,
                            StringUtils.defaultString(contributorName)
                    )
            );
            if (alignmentResult == null || !alignmentResult.success()) {
                return false;
            }
        }

        if (!candidatAutoAlignmentPersistence.addSelectedTranslations(
                thesaurusId, proposition.getConceptId(), userId, proposition.getTraductions())) {
            return false;
        }
        if (!candidatAutoAlignmentPersistence.addSelectedDefinitions(
                proposition.getConceptId(),
                thesaurusId,
                userId,
                proposition.getSourceName(),
                proposition.getDefinitions())) {
            return false;
        }
        if (!candidatAutoAlignmentPersistence.addSelectedImages(
                proposition.getConceptId(),
                thesaurusId,
                userId,
                proposition.getLocalLabel(),
                proposition.getSourceName(),
                proposition.getImages())) {
            return false;
        }
        if ("GeoNames".equalsIgnoreCase(proposition.getSourceName())
                && (proposition.getLatitude() != 0 || proposition.getLongitude() != 0)) {
            candidatAutoAlignmentPersistence.insertGpsCoordinates(
                    proposition.getConceptId(),
                    thesaurusId,
                    proposition.getLatitude(),
                    proposition.getLongitude());
        }
        candidatAutoAlignmentPersistence.touchConcept(thesaurusId, proposition.getConceptId(), userId);
        return true;
    }

    private List<AlignmentProposition> searchPropositionsForConcept(
            String thesaurusId,
            String lang,
            String conceptId,
            String label,
            AlignementSource source
    ) {
        var outcome = alignmentAutoExternalSearch.search(
                source,
                new AlignmentAutoExternalSearch.SearchContext(
                        thesaurusId, conceptId, label, lang, "", ""
                )
        );
        if (outcome.results() == null || outcome.results().isEmpty()) {
            return List.of();
        }
        List<AlignmentProposition> hits = new ArrayList<>();
        for (NodeAlignment hit : outcome.results()) {
            hits.add(AlignmentProposition.builder()
                    .conceptId(conceptId)
                    .localLabel(label)
                    .targetLabel(StringUtils.defaultString(hit.getConcept_target()))
                    .targetUri(StringUtils.defaultString(hit.getUri_target()))
                    .targetDefinition(StringUtils.defaultString(hit.getDef_target()))
                    .sourceName(source.getSource())
                    .sourceId(source.getId())
                    .alignmentTypeId(hit.getAlignement_id_type() > 0 ? hit.getAlignement_id_type() : 1)
                    .alreadyAligned(false)
                    .latitude(hit.getLat())
                    .longitude(hit.getLng())
                    .build());
        }
        return hits;
    }

    /**
     * Comparaison : concepts déjà alignés vers la source, avec proposition distante.
     */
    @Transactional(readOnly = true)
    public List<AlignmentProposition> searchComparisons(
            String thesaurusId,
            String lang,
            List<AlignmentAdminRow> summaryRows,
            AlignementSource source
    ) {
        if (source == null || summaryRows == null || StringUtils.isAnyBlank(thesaurusId, lang)) {
            return List.of();
        }
        String sourceHost = hostOf(source.getRequete());
        Map<String, AlignmentAdminRow> existingByConcept = new LinkedHashMap<>();
        for (AlignmentAdminRow row : summaryRows) {
            if (row.isPlaceholder()) {
                continue;
            }
            boolean sameSource = StringUtils.equalsIgnoreCase(row.sourceName(), source.getSource())
                    || (StringUtils.isNotBlank(sourceHost) && sourceHost.equalsIgnoreCase(hostOf(row.targetUri())));
            if (sameSource) {
                existingByConcept.putIfAbsent(row.conceptId(), row);
            }
        }

        List<AlignmentProposition> comparisons = new ArrayList<>();
        for (AlignmentAdminRow existing : existingByConcept.values()) {
            var outcome = alignmentAutoExternalSearch.search(
                    source,
                    new AlignmentAutoExternalSearch.SearchContext(
                            thesaurusId, existing.conceptId(), existing.conceptLabel(), lang, "", ""
                    )
            );
            NodeAlignment best = (outcome.results() == null || outcome.results().isEmpty())
                    ? null
                    : outcome.results().get(0);
            comparisons.add(AlignmentProposition.builder()
                    .conceptId(existing.conceptId())
                    .localLabel(existing.conceptLabel())
                    .localDefinition("")
                    .targetLabel(best != null ? StringUtils.defaultString(best.getConcept_target()) : "")
                    .targetUri(best != null
                            ? StringUtils.defaultString(best.getUri_target())
                            : StringUtils.defaultString(existing.targetUri()))
                    .targetDefinition(best != null ? StringUtils.defaultString(best.getDef_target()) : "")
                    .sourceName(source.getSource())
                    .sourceId(source.getId())
                    .alignmentTypeId(existing.typeId() > 0 ? existing.typeId() : 1)
                    .alreadyAligned(true)
                    .build());
        }
        return comparisons;
    }

    private Map<String, List<AlignmentAdminRow>> loadAlignmentsGrouped(
            String thesaurusId,
            List<String> conceptIds,
            Map<String, String> labels
    ) {
        Map<String, List<AlignmentAdminRow>> grouped = new LinkedHashMap<>();
        for (String conceptId : conceptIds) {
            var projections = alignementRepository.findAllAlignmentsByConceptAndThesaurus(conceptId, thesaurusId);
            if (projections.isEmpty()) {
                continue;
            }
            String label = labels.getOrDefault(conceptId, conceptId);
            List<AlignmentAdminRow> rows = new ArrayList<>();
            for (var projection : projections) {
                rows.add(new AlignmentAdminRow(
                        conceptId,
                        label,
                        projection.getId(),
                        projection.getUri_target(),
                        StringUtils.defaultIfBlank(projection.getLabel(), projection.getLabel_skos()),
                        projection.getAlignement_id_type(),
                        StringUtils.defaultString(projection.getThesaurus_target()),
                        projection.getUrl_available()
                ));
            }
            grouped.put(conceptId, rows);
        }
        return grouped;
    }

    @Transactional
    public boolean replaceAlignmentFromProposition(
            String thesaurusId,
            AlignmentProposition proposition,
            int userId,
            String contributorName
    ) {
        if (proposition == null || StringUtils.isAnyBlank(thesaurusId, proposition.getConceptId(), proposition.getTargetUri())) {
            return false;
        }
        // supprimer les alignements existants vers la même source (nom ou host)
        String sourceHost = hostOf(proposition.getTargetUri());
        var existing = alignementRepository.findAllAlignmentsByConceptAndThesaurus(
                proposition.getConceptId(), thesaurusId);
        for (var projection : existing) {
            boolean sameSource = StringUtils.equalsIgnoreCase(
                    StringUtils.defaultString(projection.getThesaurus_target()),
                    StringUtils.defaultString(proposition.getSourceName()))
                    || (StringUtils.isNotBlank(sourceHost)
                    && sourceHost.equalsIgnoreCase(hostOf(projection.getUri_target())));
            if (sameSource) {
                alignementRepository.deleteByIdAndThesaurus(projection.getId(), thesaurusId);
            }
        }
        MutationResult result = conceptAlignmentMutationService.addManualAlignment(
                new AddManualAlignmentCommand(
                        thesaurusId,
                        proposition.getConceptId(),
                        proposition.getAlignmentTypeId() > 0 ? proposition.getAlignmentTypeId() : 1,
                        proposition.getTargetUri(),
                        proposition.getSourceName(),
                        userId,
                        contributorName
                )
        );
        return result != null && result.success();
    }

    @Transactional
    public String addOpenthesoSource(
            String thesaurusId,
            int userId,
            String sourceName,
            String sourceUri,
            String sourceThesaurusId,
            String description
    ) {
        String error = validateOpenthesoSource(sourceName, sourceUri, sourceThesaurusId);
        if (error != null) {
            return error;
        }
        String uri = normalizeOpenthesoUri(sourceUri);
        try {
            var saved = alignementSourceRepository.save(fr.cnrs.opentheso.entites.AlignementSource.builder()
                    .source(sourceName.trim())
                    .requete(uri + "/api/search?q=##value##&lang=##lang##&theso=" + sourceThesaurusId.trim())
                    .typeRqt("REST")
                    .alignementFormat("skos")
                    .description(StringUtils.defaultString(description))
                    .idUser(userId)
                    .gps(false)
                    .sourceFilter("Opentheso")
                    .isGlobal(false)
                    .idThesaurusOwner(thesaurusId)
                    .build());
            if (StringUtils.isNotBlank(thesaurusId) && saved.getId() != null) {
                thesaurusAlignementSourceRepository.save(ThesaurusAlignementSource.builder()
                        .idAlignementSource(saved.getId())
                        .idThesaurus(thesaurusId)
                        .build());
            }
        } catch (Exception ex) {
            log.error("Erreur lors de l'ajout de la source Opentheso", ex);
            return "Erreur côté base de données !";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public String validateOpenthesoSource(String sourceName, String sourceUri, String sourceThesaurusId) {
        if (StringUtils.isBlank(sourceName)) {
            return "Le nom de la source est obligatoire !";
        }
        if (StringUtils.isBlank(sourceUri)) {
            return "L'URL est obligatoire !";
        }
        if (StringUtils.isBlank(sourceThesaurusId)) {
            return "L'Id. du thésaurus est obligatoire !";
        }
        if (!isPingOk(normalizeOpenthesoUri(sourceUri))) {
            return "Uri du serveur non valide !";
        }
        return null;
    }

    @Transactional
    public String updateLocalSource(int sourceId, String sourceName, String requete, String description) {
        return updateLocalSource(sourceId, sourceName, requete, description, null);
    }

    @Transactional
    public String updateLocalSource(
            int sourceId,
            String sourceName,
            String requete,
            String description,
            String sourceFilter
    ) {
        if (sourceId <= 0) {
            return "Source introuvable";
        }
        if (StringUtils.isBlank(sourceName)) {
            return "Le nom de la source est obligatoire !";
        }
        if (StringUtils.isBlank(requete)) {
            return "L'URL est obligatoire !";
        }
        var found = alignementSourceRepository.findById(sourceId);
        if (found.isEmpty()) {
            return "Source introuvable";
        }
        found.get().setSource(sourceName.trim());
        found.get().setRequete(requete.trim());
        found.get().setDescription(StringUtils.defaultString(description));
        if (sourceFilter != null) {
            found.get().setSourceFilter(StringUtils.defaultString(sourceFilter).trim());
        }
        alignementSourceRepository.save(found.get());
        return null;
    }

    @Transactional
    public String addLocalSource(
            String thesaurusId,
            int userId,
            String sourceName,
            String requete,
            String description,
            String sourceFilter,
            boolean selected
    ) {
        if (StringUtils.isBlank(sourceName)) {
            return "Le nom de la source est obligatoire !";
        }
        if (StringUtils.isBlank(requete)) {
            return "L'URL est obligatoire !";
        }
        try {
            var saved = alignementSourceRepository.save(fr.cnrs.opentheso.entites.AlignementSource.builder()
                    .source(sourceName.trim())
                    .requete(requete.trim())
                    .typeRqt("REST")
                    .alignementFormat("skos")
                    .description(StringUtils.defaultString(description))
                    .idUser(userId)
                    .gps(false)
                    .sourceFilter(StringUtils.defaultIfBlank(sourceFilter, "Opentheso").trim())
                    .isGlobal(false)
                    .idThesaurusOwner(thesaurusId)
                    .build());
            if (selected && StringUtils.isNotBlank(thesaurusId) && saved.getId() != null) {
                thesaurusAlignementSourceRepository.save(ThesaurusAlignementSource.builder()
                        .idAlignementSource(saved.getId())
                        .idThesaurus(thesaurusId)
                        .build());
            }
        } catch (Exception ex) {
            log.error("Erreur lors de l'ajout de la source d'alignement", ex);
            return "Erreur côté base de données !";
        }
        return null;
    }

    private String normalizeOpenthesoUri(String sourceUri) {
        String uri = sourceUri.trim();
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private List<String> limitBranch(List<String> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return List.of();
        }
        if (branchIds.size() <= BRANCH_LIMIT) {
            return branchIds;
        }
        return List.copyOf(branchIds.subList(0, BRANCH_LIMIT));
    }

    private boolean isPingOk(String baseUri) {
        try {
            URL url = URI.create(baseUri + "/api/ping").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            log.debug("Ping source échoué: {}", baseUri, ex);
            return false;
        }
    }

    private String hostOf(String urlString) {
        if (StringUtils.isBlank(urlString)) {
            return "";
        }
        try {
            return URI.create(urlString.trim()).toURL().getHost();
        } catch (Exception ex) {
            return "";
        }
    }
}
