package fr.cnrs.opentheso.v2.concept.alignment.service;

import fr.cnrs.opentheso.client.alignement.AgrovocHelper;
import fr.cnrs.opentheso.client.alignement.GemetHelper;
import fr.cnrs.opentheso.client.alignement.GeoNamesHelper;
import fr.cnrs.opentheso.client.alignement.WikidataHelper;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Enrichit une proposition (traductions / définitions / images) à la validation,
 * en réconciliant avec les données locales du concept.
 */
@Component
@RequiredArgsConstructor
public class AlignmentPropositionEnricher {

    private static final List<String> ALL_OPTIONS = List.of("langues", "notes", "images");

    private final CandidatAutoAlignmentPersistence persistence;

    public void enrich(
            AlignmentProposition proposition,
            AlignementSource source,
            String thesaurusId,
            String currentLang
    ) {
        if (proposition == null || source == null || StringUtils.isAnyBlank(thesaurusId, proposition.getConceptId())) {
            return;
        }
        if (proposition.isEnriched()) {
            return;
        }

        List<String> allLangs = persistence.loadThesaurusLanguages(thesaurusId);
        List<String> otherLangs = new ArrayList<>(allLangs);
        otherLangs.removeIf(lang -> StringUtils.equalsIgnoreCase(lang, currentLang));

        List<NodeTermTraduction> localTraductions = persistence.loadTranslations(thesaurusId, proposition.getConceptId());
        List<NodeNote> localNotes = persistence.loadNotes(proposition.getConceptId(), thesaurusId);
        List<NodeImage> localImages = persistence.loadImages(proposition.getConceptId(), thesaurusId);

        NodeAlignment node = toNodeAlignment(proposition);
        List<SelectedResource> remoteTraductions = List.of();
        List<SelectedResource> remoteDefinitions = List.of();
        List<SelectedResource> remoteImages = List.of();

        String filter = StringUtils.defaultString(source.getSource_filter()).toUpperCase();
        switch (filter) {
            case "WIKIDATA_SPARQL", "WIKIDATA_REST" -> {
                WikidataHelper helper = new WikidataHelper();
                helper.setOptionsFromWikidata(node, ALL_OPTIONS, otherLangs, allLangs);
                remoteTraductions = nullToEmpty(helper.getResourceWikidataTraductions());
                remoteDefinitions = nullToEmpty(helper.getResourceWikidataDefinitions());
                remoteImages = nullToEmpty(helper.getResourceWikidataImages());
            }
            case "GEMET", "GETTY_AAT" -> {
                GemetHelper helper = new GemetHelper();
                helper.setOptions(node, ALL_OPTIONS, otherLangs, allLangs);
                remoteTraductions = nullToEmpty(helper.getResourceTraductions());
                remoteDefinitions = nullToEmpty(helper.getResourceDefinitions());
                remoteImages = nullToEmpty(helper.getResourceImages());
            }
            case "AGROVOC" -> {
                AgrovocHelper helper = new AgrovocHelper();
                helper.setOptions(node, ALL_OPTIONS, otherLangs, currentLang);
                remoteTraductions = nullToEmpty(helper.getResourceTraductions());
                remoteDefinitions = nullToEmpty(helper.getResourceDefinitions());
                remoteImages = nullToEmpty(helper.getResourceImages());
            }
            case "GEONAMES" -> {
                GeoNamesHelper helper = new GeoNamesHelper();
                helper.setOptions(node, ALL_OPTIONS, otherLangs);
                remoteTraductions = nullToEmpty(helper.getResourceTraductions());
                remoteDefinitions = nullToEmpty(helper.getResourceDefinitions());
                remoteImages = nullToEmpty(helper.getResourceImages());
                proposition.setLatitude(node.getLat());
                proposition.setLongitude(node.getLng());
            }
            default -> {
                // IdRef / Opentheso / Ontome : pas d'enrichissement distant standard
            }
        }

        proposition.setTraductions(reconcileTraductions(remoteTraductions, localTraductions));
        proposition.setDefinitions(reconcileDefinitions(remoteDefinitions, localNotes));
        proposition.setImages(reconcileImages(remoteImages, localImages));
        proposition.setEnriched(true);
    }

    private static NodeAlignment toNodeAlignment(AlignmentProposition proposition) {
        NodeAlignment node = new NodeAlignment();
        node.setInternal_id_concept(proposition.getConceptId());
        node.setConcept_target(proposition.getTargetLabel());
        node.setUri_target(proposition.getTargetUri());
        node.setDef_target(proposition.getTargetDefinition());
        node.setThesaurus_target(proposition.getSourceName());
        node.setAlignement_id_type(proposition.getAlignmentTypeId());
        node.setLat(proposition.getLatitude());
        node.setLng(proposition.getLongitude());
        return node;
    }

    private static List<SelectedResource> reconcileTraductions(
            List<SelectedResource> remote,
            List<NodeTermTraduction> local
    ) {
        List<SelectedResource> result = new ArrayList<>();
        for (SelectedResource candidate : remote) {
            if (candidate == null || StringUtils.isBlank(candidate.getGettedValue())) {
                continue;
            }
            boolean skip = false;
            for (NodeTermTraduction localTraduction : local) {
                if (!StringUtils.equalsIgnoreCase(candidate.getIdLang(), localTraduction.getLang())) {
                    continue;
                }
                if (StringUtils.equalsIgnoreCase(
                        candidate.getGettedValue().trim(),
                        StringUtils.defaultString(localTraduction.getLexicalValue()).trim())) {
                    skip = true;
                } else {
                    candidate.setLocalValue(localTraduction.getLexicalValue());
                }
                break;
            }
            if (!skip) {
                candidate.setSelected(true);
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<SelectedResource> reconcileDefinitions(
            List<SelectedResource> remote,
            List<NodeNote> localNotes
    ) {
        List<SelectedResource> result = new ArrayList<>();
        for (SelectedResource candidate : remote) {
            if (candidate == null || StringUtils.isBlank(candidate.getGettedValue())) {
                continue;
            }
            boolean skip = false;
            for (NodeNote note : localNotes) {
                if (!"definition".equalsIgnoreCase(note.getNoteTypeCode())) {
                    continue;
                }
                if (StringUtils.isNotBlank(candidate.getIdLang())
                        && !StringUtils.equalsIgnoreCase(candidate.getIdLang(), note.getLang())) {
                    continue;
                }
                if (StringUtils.equalsIgnoreCase(
                        candidate.getGettedValue().trim(),
                        StringUtils.defaultString(note.getLexicalValue()).trim())) {
                    skip = true;
                } else {
                    candidate.setLocalValue(note.getLexicalValue());
                }
                break;
            }
            if (!skip) {
                candidate.setSelected(true);
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<SelectedResource> reconcileImages(
            List<SelectedResource> remote,
            List<NodeImage> localImages
    ) {
        List<SelectedResource> result = new ArrayList<>();
        for (SelectedResource candidate : remote) {
            if (candidate == null || StringUtils.isBlank(candidate.getGettedValue())) {
                continue;
            }
            boolean exists = localImages.stream().anyMatch(image ->
                    StringUtils.equalsIgnoreCase(
                            StringUtils.defaultString(image.getUri()).trim(),
                            candidate.getGettedValue().trim()));
            if (!exists) {
                candidate.setSelected(true);
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<SelectedResource> nullToEmpty(List<SelectedResource> values) {
        return CollectionUtils.isEmpty(values) ? List.of() : values;
    }
}
