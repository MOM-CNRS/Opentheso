package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.ConceptFacet;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.models.SkosConceptProjection;
import fr.cnrs.opentheso.models.SkosFacetProjection;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.group.NodeGroupLabel;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExportRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusSkosDocumentBuilder {

    private static final String SEPARATOR = "##";
    private static final String SUB_SEPARATOR = "@@";

    private final ExportRepository exportRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final RelationGroupRepository relationGroupRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final NoteRepository noteRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    private final HashMap<String, String> superGroupHashMap = new HashMap<>();

    public SKOSXmlDocument buildFullDocument(String thesaurusId) throws Exception {
        return buildDocument(thesaurusId, ThesaurusEditionExportOptions.full());
    }

    public SKOSXmlDocument buildDocument(String thesaurusId, ThesaurusEditionExportOptions options) throws Exception {
        var preferences = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (preferences == null) {
            throw new IllegalStateException("Préférences du thésaurus introuvables");
        }
        if (StringUtils.isEmpty(preferences.getOriginalUri())) {
            throw new IllegalStateException("Veuillez ajouter une URI valide dans les préférences du thésaurus !");
        }

        var resolvedOptions = options == null ? ThesaurusEditionExportOptions.full() : options;
        var document = new SKOSXmlDocument();
        var baseUrl = ThesaurusSkosUriSupport.resolveBaseUrl(preferences);
        document.setConceptScheme(exportConceptScheme(thesaurusId, preferences, baseUrl));

        List<SKOSResource> concepts = new ArrayList<>();
        if (!resolvedOptions.filterByGroup()) {
            for (SKOSResource group : exportCollections(thesaurusId, preferences, baseUrl)) {
                document.addGroup(group);
            }
            concepts = getAllConcepts(
                    thesaurusId, baseUrl, null, preferences.getOriginalUri(), preferences, resolvedOptions.clearHtml()
            );
            for (SKOSResource facet : getAllFacettes(thesaurusId, baseUrl, preferences.getOriginalUri(), preferences)) {
                document.addFacet(facet);
            }
        } else {
            for (String groupId : resolvedOptions.selectedGroupIds()) {
                document.addGroup(exportCollectionById(thesaurusId, groupId, preferences, baseUrl));
                concepts.addAll(getAllConcepts(
                        thesaurusId, baseUrl, groupId, preferences.getOriginalUri(), preferences, resolvedOptions.clearHtml()
                ));
            }
            for (SKOSResource facet : getAllFacettes(thesaurusId, baseUrl, preferences.getOriginalUri(), preferences)) {
                if (isFacetInGroups(thesaurusId, facet.getIdentifier(), resolvedOptions.selectedGroupIds())) {
                    document.addFacet(facet);
                }
            }
        }

        for (SKOSResource concept : concepts) {
            document.addconcept(concept);
        }

        return document;
    }

    public SKOSXmlDocument buildDocumentByGroup(String thesaurusId, String groupId, boolean clearHtml) throws Exception {
        return buildDocument(
                thesaurusId,
                new ThesaurusEditionExportOptions(true, List.of(groupId), clearHtml)
        );
    }

    private boolean isFacetInGroups(String thesaurusId, String facetId, List<String> groupIds) {
        if (CollectionUtils.isEmpty(groupIds) || StringUtils.isBlank(facetId)) {
            return false;
        }
        var normalizedGroups = groupIds.stream()
                .filter(StringUtils::isNotEmpty)
                .map(String::toLowerCase)
                .toList();
        return thesaurusArrayRepository.findConceptParentInGroups(facetId, thesaurusId, normalizedGroups).isPresent();
    }

    public SKOSResource exportCollectionById(String thesaurusId, String groupId, Preferences preferences, String baseUrl) {
        var groupLabel = loadGroupLabel(groupId, thesaurusId);
        if (groupLabel == null) {
            return new SKOSResource();
        }
        var resource = new SKOSResource(
                ThesaurusSkosUriSupport.uriFromGroup(groupLabel, preferences, baseUrl),
                SKOSProperty.CONCEPT_GROUP
        );
        resource.setIdentifier(groupId);
        resource.addRelation(
                groupLabel.getIdGroup(),
                ThesaurusSkosUriSupport.uriFromGroup(groupLabel, preferences, baseUrl),
                SKOSProperty.MICROTHESAURUS_OF
        );
        return exportCollectionRecursive(thesaurusId, groupId, resource, preferences, baseUrl);
    }

    public List<SKOSResource> getAllFacettes(String idThesaurus, String baseUrl, String originalUri, Preferences nodePreference) throws Exception {

        var projections = exportRepository.getAllFacettes(idThesaurus, baseUrl);
        List<SKOSResource> result = new ArrayList<>();

        for (SkosFacetProjection p : projections) {
            boolean isExist = false;
            for (SKOSResource skosResource : result) {
                var uri = getUriForFacette(p.getId_facet(), idThesaurus, originalUri);
                if (uri.equalsIgnoreCase(skosResource.getUri())) {
                    skosResource.addLabel(p.getLexicalvalue(), p.getLang(), SKOSProperty.PREF_LABEL);
                    isExist = true;
                    break;
                }
            }

            if (!isExist) {
                var resource = new SKOSResource(getUriForFacette(p.getId_facet(), idThesaurus, originalUri), SKOSProperty.FACET);
                resource.setIdentifier(p.getId_facet());
                resource.addRelation(p.getId_concept_parent(), p.getUri_value(), SKOSProperty.SUPER_ORDINATE);

                var members = conceptFacetRepository.findAllByIdFacetAndIdThesaurus(p.getId_facet(), idThesaurus);
                for (ConceptFacet member : members) {
                    var nodeUri = resolveConceptUri(member.getIdConcept(), idThesaurus);
                    resource.addRelation(nodeUri.getIdConcept(), buildUri(nodeUri, idThesaurus, member.getIdConcept(), originalUri, nodePreference), SKOSProperty.MEMBER);
                }

                resource.addLabel(p.getLexicalvalue(), p.getLang(), SKOSProperty.PREF_LABEL);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                resource.addDate(dateFormat.format(p.getCreated()), SKOSProperty.CREATED);
                resource.addDate(dateFormat.format(p.getModified()), SKOSProperty.MODIFIED);

                addDoc(p.getDefinition(), resource, SKOSProperty.DEFINITION);
                addDoc(p.getNote(), resource, SKOSProperty.NOTE);
                addDoc(p.getEditorialnote(), resource, SKOSProperty.EDITORIAL_NOTE);
                addDoc(p.getSecopenote(), resource, SKOSProperty.SCOPE_NOTE);
                addDoc(p.getHistorynote(), resource, SKOSProperty.HISTORY_NOTE);
                addDoc(p.getExample(), resource, SKOSProperty.EXAMPLE);
                addDoc(p.getChangenote(), resource, SKOSProperty.CHANGE_NOTE);

                result.add(resource);
            }
        }
        return result;
    }

    public List<SKOSResource> getAllConcepts(String idThesaurus, String baseUrl, String idGroup, String originalUri,
                                             Preferences nodePreference, boolean filterHtmlCharacter) throws Exception {

        List<SkosConceptProjection> projections = StringUtils.isEmpty(idGroup)
                ? exportRepository.getAllConcepts(idThesaurus, baseUrl)
                : exportRepository.getAllConceptsByGroup(idThesaurus, baseUrl, idGroup);

        List<SKOSResource> result = new ArrayList<>();

        for (SkosConceptProjection p : projections) {
            result.add(buildConceptFromProjection(
                    p, idThesaurus, baseUrl, originalUri, nodePreference, filterHtmlCharacter
            ));
        }
        return result;
    }

    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return exportConceptScheme(
                thesaurusId,
                preferences,
                ThesaurusSkosUriSupport.resolveBaseUrl(preferences)
        );
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId, Preferences preferences) throws Exception {
        return exportConcept(thesaurusId, conceptId, preferences, false);
    }

    public SKOSResource exportConcept(
            String thesaurusId,
            String conceptId,
            Preferences preferences,
            boolean filterHtmlCharacter
    ) throws Exception {
        var baseUrl = ThesaurusSkosUriSupport.resolveBaseUrl(preferences);
        for (SkosConceptProjection projection : exportRepository.getAllConcepts(thesaurusId, baseUrl)) {
            if (conceptId.equals(projection.getIdentifier())) {
                return buildConceptFromProjection(
                        projection,
                        thesaurusId,
                        baseUrl,
                        preferences.getOriginalUri(),
                        preferences,
                        filterHtmlCharacter
                );
            }
        }
        return null;
    }

    private SKOSResource buildConceptFromProjection(
            SkosConceptProjection p,
            String idThesaurus,
            String baseUrl,
            String originalUri,
            Preferences nodePreference,
            boolean filterHtmlCharacter
    ) throws Exception {
            SKOSResource sKOSResource = new SKOSResource();
            sKOSResource.setProperty(SKOSProperty.CONCEPT);
            sKOSResource.setUri(p.getUri());
            sKOSResource.setLocalUri(p.getLocal_uri());

            sKOSResource.addIdentifier(p.getIdentifier(), SKOSProperty.IDENTIFIER);

            if (!StringUtils.isEmpty(p.getArk_id())) {
                sKOSResource.setArkId(p.getArk_id());
            }

            setStatusOfConcept(p.getType(), sKOSResource);

            getLabels(p.getPrefLab(), sKOSResource, SKOSProperty.PREF_LABEL);
            getLabels(p.getAltLab_hiden(), sKOSResource, SKOSProperty.HIDDEN_LABEL);
            getLabels(p.getAltLab(), sKOSResource, SKOSProperty.ALT_LABEL);

            if (StringUtils.isEmpty(p.getBroader())) {
                sKOSResource.getRelationsList().add(new SKOSRelation(idThesaurus, getUriThesoFromId(idThesaurus, originalUri, nodePreference),
                        SKOSProperty.TOP_CONCEPT_OF));
            }

            addRelationsGiven(p.getRelated(), sKOSResource);

            var note = p.getDefinition();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.DEFINITION);
            }

            note = p.getNote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.NOTE);
            }

            note = p.getEditorialnote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.EDITORIAL_NOTE);
            }
            note = p.getSecopenote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.SCOPE_NOTE);
            }
            note = p.getHistorynote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.HISTORY_NOTE);
            }
            note = p.getExample();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.EXAMPLE);
            }
            note = p.getChangenote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.CHANGE_NOTE);
            }

            addAlignementGiven(p.getBroadMatch(), sKOSResource, SKOSProperty.BROAD_MATCH);
            addAlignementGiven(p.getCloseMatch(), sKOSResource, SKOSProperty.CLOSE_MATCH);
            addAlignementGiven(p.getExactMatch(), sKOSResource, SKOSProperty.EXACT_MATCH);
            addAlignementGiven(p.getNarrowMatch(), sKOSResource, SKOSProperty.NARROWER_MATCH);
            addAlignementGiven(p.getRelatedMatch(), sKOSResource, SKOSProperty.RELATED_MATCH);

            addRelationsGiven(p.getNarrower(), sKOSResource);

            if (p.getBroader() != null) {
                addRelationsGiven(p.getBroader(), sKOSResource);
            }

            sKOSResource.addRelation(idThesaurus, getUriThesoFromId(idThesaurus, originalUri, nodePreference), SKOSProperty.INSCHEME);

            addReplaced(p.getReplaces(), sKOSResource, SKOSProperty.REPLACES);

            addReplaced(p.getReplaced_by(), sKOSResource, SKOSProperty.IS_REPLACED_BY);

            if (!StringUtils.isEmpty(p.getNotation())) {
                sKOSResource.addNotation(p.getNotation());
            }

            addImages(sKOSResource, p.getImg());

            addMembres(sKOSResource, p.getMembre(), p.getIdentifier());

            addFacets(sKOSResource, p.getFacets(), idThesaurus, originalUri);

            addExternalResources(sKOSResource, p.getExternalResources());

            addGps(sKOSResource, p.getGpsData());

            if (p.getCreator() != null) {
                sKOSResource.addAgent(p.getCreator(), SKOSProperty.CREATOR);
            }

            if(!StringUtils.isEmpty(p.getContributor())){
                var contributors = p.getContributor().split(SEPARATOR);
                for (String contributor : contributors) {
                    sKOSResource.addAgent(contributor, SKOSProperty.CONTRIBUTOR);
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (ObjectUtils.isNotEmpty(p.getCreated())) {
                sKOSResource.addDate(dateFormat.format(p.getCreated()), SKOSProperty.CREATED);
            }

            if (ObjectUtils.isNotEmpty(p.getModified())) {
                sKOSResource.addDate(dateFormat.format(p.getModified()), SKOSProperty.MODIFIED);
            }

            return sKOSResource;
    }

    private String getUriForFacette(String idFacet, String idTheso, String originalUri) {
        return originalUri + "/?idf=" + idFacet + "&idt=" + idTheso;
    }

    private void addDoc(String content, SKOSResource resource, int type) {
        if (StringUtils.isNotEmpty(content)) {
            String[] tabs = content.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    String[] parts = tab.split(SUB_SEPARATOR);
                    if (parts.length == 2) {
                        String cleanText = Jsoup.parse(parts[0]).text();
                        resource.addDocumentation(cleanText, parts[1], type);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ doc = " + resource.getIdentifier() + "  " + content );
            }
        }
    }

    private void addGps(SKOSResource sKOSResource, String str) {
        if (StringUtils.isNotEmpty(str)) {
            String[] tabs = str.split(SEPARATOR);
            try {
                List<SKOSGPSCoordinates> tmp = new ArrayList<>();
                for (String tab : tabs) {
                    String[] element = tab.split(SUB_SEPARATOR);
                    tmp.add(new SKOSGPSCoordinates(Double.parseDouble(element[0]), Double.parseDouble(element[1])));
                }
                sKOSResource.setGpsCoordinates(tmp);
            } catch (Exception e) {
                log.error("Erreur export Concept _ gps = " + sKOSResource.getIdentifier() + "  " + str );
            }
        }
    }

    private void addImages(SKOSResource resource, String textBrut) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] images = textBrut.split(SEPARATOR);
            try {
                ArrayList<NodeImage> nodeImages = new ArrayList<>();
                for (String image : images) {
                    String[] imageDetail = image.split(SUB_SEPARATOR);
                    // if(imageDetail.length != 4) return;

                    NodeImage nodeImage = new NodeImage();
                    nodeImage.setImageName(imageDetail[0]);
                    nodeImage.setCopyRight(imageDetail[1]);
                    nodeImage.setUri(imageDetail[2]);
                    if (imageDetail.length >= 4)
                        nodeImage.setCreator(imageDetail[3]);
                    nodeImages.add(nodeImage);
                }
                resource.setNodeImages(nodeImages);
            } catch (Exception e) {
                log.error("Erreur export Concept _ images = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addFacets(SKOSResource resource, String textBrut, String idTheso, String originalUri) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] idFacettes = textBrut.split(SEPARATOR);
            try {
                for (String idFacette : idFacettes) {
                    String url = originalUri + "/?idf=" + idFacette + "&idt=" + idTheso;
                    resource.addRelation(idFacette, url, SKOSProperty.SUB_ORDINATE_ARRAY);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ facets = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }
    private void addExternalResources(SKOSResource resource, String textBrut) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] externalResources = textBrut.split(SEPARATOR);
            try {
                for (String externalResource : externalResources) {
                    resource.addExternalResource(externalResource);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ externalResources = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void setStatusOfConcept(String status, SKOSResource sKOSResource) {
        switch (status.toLowerCase()) {
            case "ca":
                sKOSResource.setStatus(SKOSProperty.CANDIDATE);
                break;
            case "dep":
                sKOSResource.setStatus(SKOSProperty.DEPRECATED);
                break;
            default:
                sKOSResource.setStatus(SKOSProperty.CONCEPT);
                break;
        }

    }

    private void addRelationsGiven(String textBrut, SKOSResource sKOSResource) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    String[] element = tab.split(SUB_SEPARATOR);
                    sKOSResource.addRelation(element[2], element[0], getType(element[1]));
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ relations = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private int getType(String role) {
        switch (role) {
            case "RHP":
                return SKOSProperty.RELATED_HAS_PART;
            case "RPO":
                return SKOSProperty.RELATED_PART_OF;
            case "RT":
                return SKOSProperty.RELATED;
            case "NTG":
                return SKOSProperty.NARROWER_GENERIC;
            case "NTP":
                return SKOSProperty.NARROWER_PARTITIVE;
            case "NTI":
                return SKOSProperty.NARROWER_INSTANTIAL;
            case "NT":
                return SKOSProperty.NARROWER;
            case "BTG":
                return SKOSProperty.BROADER_GENERIC;
            case "BTP":
                return SKOSProperty.BROADER_PARTITIVE;
            case "BTI":
                return SKOSProperty.BROADER_INSTANTIAL;
            default:
                return SKOSProperty.BROADER;
        }
    }

    private void addReplaced(String textBrut, SKOSResource sKOSResource, int type) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addReplaces(tab, type);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ replaced = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addAlignementGiven(String textBrut, SKOSResource sKOSResource, int type) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addMatch(tab.trim(), type);
                    //    log.info(textBrut);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ alignement = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addDocumentation(String textBrut, SKOSResource sKOSResource, int type) throws SQLException {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    String str = fr.cnrs.opentheso.utils.StringUtils.normalizeStringForXml(element[0]);
                    sKOSResource.addDocumentation(str, element[1], type);
                } catch (Exception e) {
                    System.out.println("Erreur export Concept _ note = " + sKOSResource.getIdentifier() + " _____ " + textBrut );
                }

            }
        }
    }

    private void getLabels(String labelBrut, SKOSResource sKOSResource, int type) {
        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                String[] element = tab.split(SUB_SEPARATOR);
                try {
                    sKOSResource.addLabel(element[0], element[1], type);
                } catch (Exception e) {
                    System.out.println("Erreur export Concept = " + sKOSResource.getIdentifier() + "  " + labelBrut );
                }

            }
        }
    }

    private void addMembres(SKOSResource sKOSResource, String textBrut, String idConcept) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addRelation(idConcept, tab, SKOSProperty.MEMBER_OF);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ membres = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private String getUriFromNodeUri(String idTheso, String originalUri, String idConcept, Preferences nodePreference,
                                     NodeUri nodeUri) {

        if(nodePreference.isOriginalUriIsArk() && nodeUri.getIdArk()!= null && !StringUtils.isEmpty(nodeUri.getIdArk())) {
            return originalUri + '/' + nodeUri.getIdArk();
        } else if(nodePreference.isOriginalUriIsArk() && (nodeUri.getIdArk() == null || StringUtils.isEmpty(nodeUri.getIdArk())) ) {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        } else if(nodePreference.isOriginalUriIsHandle() && !StringUtils.isEmpty(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        } else if (nodePreference.isOriginalUriIsDoi() && !StringUtils.isEmpty(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        } else if (!StringUtils.isEmpty(originalUri)) {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        } else {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        }
    }

    private String buildUri(NodeUri nodeUri, String idTheso, String idConcept, String originalUri, Preferences pref) {
        if (pref.isOriginalUriIsArk() && StringUtils.isNotEmpty(nodeUri.getIdArk())) {
            return originalUri + '/' + nodeUri.getIdArk();
        } else if (pref.isOriginalUriIsHandle() && StringUtils.isNotEmpty(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        } else if (pref.isOriginalUriIsDoi() && StringUtils.isNotEmpty(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        } else {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        }
    }

    private String getUriThesoFromId(String id, String originalUri, Preferences pref) {
        if (pref.isOriginalUriIsArk()) return pref.getOriginalUri() + "/" + pref.getIdNaan() + "/" + id;
        if (pref.isOriginalUriIsHandle()) return "https://hdl.handle.net/" + id;
        if (StringUtils.isNotEmpty(originalUri)) return originalUri + "/?idt=" + id;
        return originalUri + "/?idt=" + id;
    }



    private NodeUri resolveConceptUri(String conceptId, String thesaurusId) {
        var nodeUri = new NodeUri();
        nodeUri.setIdConcept(conceptId);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        if (concept.isPresent()) {
            nodeUri.setIdArk(concept.get().getIdArk());
            nodeUri.setIdHandle(concept.get().getIdHandle());
            nodeUri.setIdDoi(concept.get().getIdDoi());
        }
        return nodeUri;
    }

    private SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences, String baseUrl) {
        var conceptScheme = new SKOSResource(
                ThesaurusSkosUriSupport.uriFromId(thesaurusId, preferences, baseUrl),
                SKOSProperty.CONCEPT_SCHEME
        );

        for (String lang : thesaurusLabelRepository.findDistinctLangByIdThesaurus(thesaurusId)) {
            thesaurusLabelRepository.findByIdThesaurusAndLang(thesaurusId, lang).ifPresent(label -> {
                if (label.getCreator() != null && !"null".equalsIgnoreCase(label.getCreator())) {
                    conceptScheme.addAgent(label.getCreator(), SKOSProperty.CREATOR);
                }
                if (label.getContributor() != null && !"null".equalsIgnoreCase(label.getContributor())) {
                    conceptScheme.addAgent(label.getContributor(), SKOSProperty.CONTRIBUTOR);
                }
                if (label.getTitle() != null && label.getLang() != null) {
                    conceptScheme.addLabel(label.getTitle(), label.getLang(), SKOSProperty.PREF_LABEL);
                }
                if (label.getCreated() != null) {
                    conceptScheme.addDate(label.getCreated().toString(), SKOSProperty.CREATED);
                }
                if (label.getModified() != null) {
                    conceptScheme.addDate(label.getModified().toString(), SKOSProperty.MODIFIED);
                }
                conceptScheme.setThesaurus(toThesaurusModel(label));
            });
        }

        var dcTerms = thesaurusDcTermRepository.findAllByIdThesaurus(thesaurusId);
        if (!dcTerms.isEmpty()) {
            conceptScheme.getThesaurus().setDcElement(dcTerms.stream()
                    .map(element -> DcElement.builder()
                            .id(element.getId().intValue())
                            .name(element.getName())
                            .value(element.getValue())
                            .language(element.getLanguage())
                            .type(element.getDataType())
                            .build())
                    .toList());
        }

        conceptRepository.findAllTopConceptsWithUris(thesaurusId).forEach(topConcept ->
                conceptScheme.addRelation(
                        topConcept.getIdConcept(),
                        ThesaurusSkosUriSupport.uriFromNodeUri(
                                topConcept,
                                thesaurusId,
                                topConcept.getIdConcept(),
                                preferences.getOriginalUri(),
                                preferences,
                                baseUrl
                        ),
                        SKOSProperty.HAS_TOP_CONCEPT
                )
        );

        return conceptScheme;
    }

    private Thesaurus toThesaurusModel(ThesaurusLabel label) {
        var thesaurus = new Thesaurus();
        thesaurus.setId_thesaurus(label.getIdThesaurus());
        thesaurus.setTitle(label.getTitle());
        thesaurus.setLanguage(label.getLang());
        thesaurus.setCreator(label.getCreator());
        thesaurus.setContributor(label.getContributor());
        thesaurus.setCoverage(label.getCoverage());
        thesaurus.setDescription(label.getDescription());
        thesaurus.setFormat(label.getFormat());
        thesaurus.setPublisher(label.getPublisher());
        thesaurus.setRelation(label.getRelation());
        thesaurus.setRights(label.getRights());
        thesaurus.setSource(label.getSource());
        thesaurus.setSubject(label.getSubject());
        thesaurus.setType(label.getType());
        return thesaurus;
    }

    private List<SKOSResource> exportCollections(String thesaurusId, Preferences preferences, String baseUrl) {
        var resources = new ArrayList<SKOSResource>();
        for (String rootGroupId : conceptGroupRepository.findRootGroups(thesaurusId)) {
            var groupLabel = loadGroupLabel(rootGroupId, thesaurusId);
            if (groupLabel == null) {
                continue;
            }
            var resource = new SKOSResource(
                    ThesaurusSkosUriSupport.uriFromGroup(groupLabel, preferences, baseUrl),
                    SKOSProperty.CONCEPT_GROUP
            );
            resource.setIdentifier(rootGroupId);
            resource.addRelation(
                    groupLabel.getIdGroup(),
                    ThesaurusSkosUriSupport.uriFromGroup(groupLabel, preferences, baseUrl),
                    SKOSProperty.MICROTHESAURUS_OF
            );
            resources.add(exportCollectionRecursive(thesaurusId, rootGroupId, resource, preferences, baseUrl));
        }
        return resources;
    }

    private SKOSResource exportCollectionRecursive(String thesaurusId, String groupId, SKOSResource resource,
                                                   Preferences preferences, String baseUrl) {
        var written = writeGroupInfo(resource, thesaurusId, groupId, preferences, baseUrl);
        for (String childGroupId : relationGroupRepository.findChildGroupIds(thesaurusId, groupId)) {
            var child = new SKOSResource();
            child.setIdentifier(childGroupId);
            exportCollectionRecursive(thesaurusId, childGroupId, child, preferences, baseUrl);
        }
        return written;
    }

    private SKOSResource writeGroupInfo(SKOSResource resource, String thesaurusId, String groupId,
                                        Preferences preferences, String baseUrl) {
        var groupLabel = loadGroupLabel(groupId, thesaurusId);
        if (groupLabel == null) {
            return resource;
        }

        resource.setUri(ThesaurusSkosUriSupport.uriFromGroup(groupLabel, preferences, baseUrl));
        resource.setProperty(SKOSProperty.CONCEPT_GROUP);

        if (groupLabel.getCreated() != null) {
            resource.addDate(groupLabel.getCreated().toString(), SKOSProperty.CREATED);
        }
        if (groupLabel.getModified() != null) {
            resource.addDate(groupLabel.getModified().toString(), SKOSProperty.MODIFIED);
        }

        for (NodeGroupTraductions translation : groupLabel.getNodeGroupTraductionses()) {
            resource.addLabel(translation.getTitle(), translation.getIdLang(), SKOSProperty.PREF_LABEL);
        }

        for (NodeUri conceptUri : conceptRepository.findConceptsByThesaurusAndGroup(thesaurusId, groupId)) {
            resource.addRelation(
                    conceptUri.getIdConcept(),
                    ThesaurusSkosUriSupport.uriFromNodeUri(
                            conceptUri, thesaurusId, conceptUri.getIdConcept(),
                            preferences.getOriginalUri(), preferences, baseUrl
                    ),
                    SKOSProperty.MEMBER
            );
        }

        for (Object[] child : relationGroupRepository.findChildGroupDetails(thesaurusId, groupId)) {
            var childUri = NodeUri.builder()
                    .idConcept((String) child[0])
                    .idArk(child[1] != null ? (String) child[1] : "")
                    .idHandle(child[2] != null ? (String) child[2] : "")
                    .idDoi(child[3] != null ? (String) child[3] : "")
                    .build();
            resource.addRelation(
                    childUri.getIdConcept(),
                    ThesaurusSkosUriSupport.uriGroupFromNodeUri(childUri, thesaurusId, preferences, baseUrl),
                    SKOSProperty.SUBGROUP
            );
            superGroupHashMap.put(childUri.getIdConcept(), groupId);
        }

        var parentGroupId = superGroupHashMap.get(groupId);
        if (parentGroupId != null) {
            conceptGroupRepository.findByIdGroupAndIdThesaurus(parentGroupId, thesaurusId).ifPresent(parent -> {
                var parentUri = NodeUri.builder()
                        .idConcept(parent.getIdGroup())
                        .idArk(parent.getIdArk())
                        .idHandle(parent.getIdHandle())
                        .idDoi(parent.getIdDoi())
                        .build();
                resource.addRelation(
                        parentGroupId,
                        ThesaurusSkosUriSupport.uriGroupFromNodeUri(parentUri, thesaurusId, preferences, baseUrl),
                        SKOSProperty.SUPERGROUP
                );
                superGroupHashMap.remove(groupId);
            });
        }

        if (StringUtils.isNotBlank(groupLabel.getNotation()) && !"null".equalsIgnoreCase(groupLabel.getNotation())) {
            resource.addNotation(groupLabel.getNotation());
        }

        noteRepository.findAllByIdentifierAndIdThesaurus(groupId, thesaurusId).forEach(note -> {
            int prop = switch (note.getNoteTypeCode()) {
                case "note" -> SKOSProperty.NOTE;
                case "scopeNote" -> SKOSProperty.SCOPE_NOTE;
                case "historyNote" -> SKOSProperty.HISTORY_NOTE;
                case "example" -> SKOSProperty.EXAMPLE;
                case "definition" -> SKOSProperty.DEFINITION;
                case "editorialNote" -> SKOSProperty.EDITORIAL_NOTE;
                case "changeNote" -> SKOSProperty.CHANGE_NOTE;
                default -> SKOSProperty.NOTE;
            };
            resource.addDocumentation(note.getLexicalValue(), note.getLang(), prop);
        });

        return resource;
    }

    private NodeGroupLabel loadGroupLabel(String groupId, String thesaurusId) {
        return conceptGroupRepository.findByIdGroupAndIdThesaurus(groupId, thesaurusId)
                .map(group -> NodeGroupLabel.builder()
                        .idGroup(groupId)
                        .idThesaurus(thesaurusId)
                        .idArk(group.getIdArk())
                        .idHandle(group.getIdHandle())
                        .idDoi(group.getIdDoi())
                        .notation(group.getNotation())
                        .created(group.getCreated())
                        .modified(group.getModified())
                        .nodeGroupTraductionses(conceptGroupLabelRepository
                                .findAllByIdThesaurusAndIdGroup(thesaurusId, groupId)
                                .stream()
                                .map(element -> NodeGroupTraductions.builder()
                                        .idLang(element.getLang())
                                        .title(element.getLexicalValue())
                                        .created(element.getCreated())
                                        .modified(element.getModified())
                                        .build())
                                .toList())
                        .build())
                .orElse(null);
    }
}
