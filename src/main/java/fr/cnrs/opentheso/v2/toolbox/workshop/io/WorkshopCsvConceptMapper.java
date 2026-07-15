package fr.cnrs.opentheso.v2.toolbox.workshop.io;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;

public final class WorkshopCsvConceptMapper {

    private WorkshopCsvConceptMapper() {
    }

    public static ThesaurusCsvConceptObject toEditionModel(WorkshopCsvReader.ConceptObject source) {
        if (source == null) {
            return null;
        }
        ThesaurusCsvConceptObject target = new ThesaurusCsvConceptObject();
        target.setIdConcept(source.getIdConcept());
        target.setUri(source.getUri());
        target.setLocalId(source.getLocalId());
        target.setArkId(source.getArkId());
        target.setIdTerm(source.getIdTerm());
        target.setType(source.getType());
        target.setConceptType(source.getConceptType());
        target.setDeprecated(source.isDeprecated());
        target.setNotation(source.getNotation());
        target.setPrefLabels(copyLabels(source.getPrefLabels()));
        target.setAltLabels(copyLabels(source.getAltLabels()));
        target.setHiddenLabels(copyLabels(source.getHiddenLabels()));
        target.setNote(copyLabels(source.getNote()));
        target.setDefinitions(copyLabels(source.getDefinitions()));
        target.setScopeNotes(copyLabels(source.getScopeNotes()));
        target.setExamples(copyLabels(source.getExamples()));
        target.setHistoryNotes(copyLabels(source.getHistoryNotes()));
        target.setChangeNotes(copyLabels(source.getChangeNotes()));
        target.setEditorialNotes(copyLabels(source.getEditorialNotes()));
        target.setBroaders(copyStrings(source.getBroaders()));
        target.setNarrowers(copyStrings(source.getNarrowers()));
        target.setRelateds(copyStrings(source.getRelateds()));
        target.setCustomRelations(source.getCustomRelations() == null ? new ArrayList<>() : new ArrayList<>(source.getCustomRelations()));
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
        target.setImages(source.getImages() == null ? new ArrayList<>() : new ArrayList<>(source.getImages()));
        target.setExternalResources(copyStrings(source.getExternalResources()));
        target.setMemberOfFacets(copyStrings(source.getMemberOfFacets()));
        target.setCreated(source.getCreated());
        target.setModified(source.getModified());
        target.setAlignments(source.getAlignments() == null ? new ArrayList<>() : new ArrayList<>(source.getAlignments()));
        return target;
    }

    private static ArrayList<ThesaurusCsvConceptLabel> copyLabels(ArrayList<WorkshopCsvReader.Label> labels) {
        ArrayList<ThesaurusCsvConceptLabel> copy = new ArrayList<>();
        if (CollectionUtils.isEmpty(labels)) {
            return copy;
        }
        for (WorkshopCsvReader.Label label : labels) {
            ThesaurusCsvConceptLabel target = new ThesaurusCsvConceptLabel();
            target.setLabel(label.getLabel());
            target.setLang(label.getLang());
            copy.add(target);
        }
        return copy;
    }

    private static ArrayList<String> copyStrings(ArrayList<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
