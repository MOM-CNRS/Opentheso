package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.services.imports.csv.CsvReadHelper;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public final class ThesaurusCsvConceptObjectBridge {

    private ThesaurusCsvConceptObjectBridge() {
    }

    public static List<CsvReadHelper.ConceptObject> toLegacyList(List<ThesaurusCsvConceptObject> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return List.of();
        }
        CsvReadHelper helper = new CsvReadHelper(',');
        return sources.stream().map(source -> toLegacy(source, helper)).toList();
    }

    public static CsvReadHelper.ConceptObject toLegacy(ThesaurusCsvConceptObject source, CsvReadHelper helper) {
        var target = helper.new ConceptObject();
        target.setIdConcept(source.getIdConcept());
        target.setUri(source.getUri());
        target.setLocalId(source.getLocalId());
        target.setArkId(source.getArkId());
        target.setIdTerm(source.getIdTerm());
        target.setType(source.getType());
        target.setConceptType(source.getConceptType());
        target.setDeprecated(source.isDeprecated());
        target.setNotation(source.getNotation());

        target.setPrefLabels(copyLabels(source.getPrefLabels(), helper));
        target.setAltLabels(copyLabels(source.getAltLabels(), helper));
        target.setHiddenLabels(copyLabels(source.getHiddenLabels(), helper));
        target.setNote(copyLabels(source.getNote(), helper));
        target.setDefinitions(copyLabels(source.getDefinitions(), helper));
        target.setScopeNotes(copyLabels(source.getScopeNotes(), helper));
        target.setExamples(copyLabels(source.getExamples(), helper));
        target.setHistoryNotes(copyLabels(source.getHistoryNotes(), helper));
        target.setChangeNotes(copyLabels(source.getChangeNotes(), helper));
        target.setEditorialNotes(copyLabels(source.getEditorialNotes(), helper));

        target.setBroaders(copyStrings(source.getBroaders()));
        target.setNarrowers(copyStrings(source.getNarrowers()));
        target.setRelateds(copyStrings(source.getRelateds()));
        target.setCustomRelations(copyNodeIdValues(source.getCustomRelations()));

        target.setExactMatchs(copyStrings(source.getExactMatchs()));
        target.setCloseMatchs(copyStrings(source.getCloseMatchs()));
        target.setBroadMatchs(copyStrings(source.getBroadMatchs()));
        target.setNarrowMatchs(copyStrings(source.getNarrowMatchs()));
        target.setRelatedMatchs(copyStrings(source.getRelatedMatchs()));

        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
        target.setGps(source.getGps());

        target.setMembers(copyStrings(source.getMembers()));
        target.setSuperOrdinate(source.getSuperOrdinate());
        target.setSubGroups(copyStrings(source.getSubGroups()));
        target.setReplacedBy(copyStrings(source.getReplacedBy()));

        target.setImages(copyImages(source.getImages()));
        target.setExternalResources(copyStrings(source.getExternalResources()));
        target.setMemberOfFacets(copyStrings(source.getMemberOfFacets()));

        target.setCreated(source.getCreated());
        target.setModified(source.getModified());
        target.setAlignments(copyNodeIdValues(source.getAlignments()));
        return target;
    }

    private static ArrayList<CsvReadHelper.Label> copyLabels(
            List<ThesaurusCsvConceptLabel> labels,
            CsvReadHelper helper) {
        var result = new ArrayList<CsvReadHelper.Label>();
        if (labels == null) {
            return result;
        }
        for (ThesaurusCsvConceptLabel label : labels) {
            var legacy = helper.new Label();
            legacy.setLabel(label.getLabel());
            legacy.setLang(label.getLang());
            result.add(legacy);
        }
        return result;
    }

    private static ArrayList<String> copyStrings(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private static ArrayList<fr.cnrs.opentheso.models.nodes.NodeIdValue> copyNodeIdValues(
            List<fr.cnrs.opentheso.models.nodes.NodeIdValue> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private static ArrayList<fr.cnrs.opentheso.models.nodes.NodeImage> copyImages(
            List<fr.cnrs.opentheso.models.nodes.NodeImage> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
