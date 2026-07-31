package fr.cnrs.opentheso.v2.concept.mapper;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptHeaderRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptUriLabel;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ConceptMapper {

    private ConceptMapper() {
    }

    public static ConceptTreeNodeData toGroupNode(Object[] row, String nodeType) {
        return new ConceptTreeNodeData(
                stringAt(row, 0),
                stringAt(row, 2),
                stringAt(row, 3),
                nodeType,
                booleanAt(row, 4)
        );
    }

    public static ConceptTreeNodeData toConceptNode(ConceptTreeRow row) {
        String nodeType = resolveConceptNodeType(row.status(), row.hasChildren());
        return new ConceptTreeNodeData(
                row.conceptId(),
                StringUtils.isBlank(row.label()) ? "(" + row.conceptId() + ")" : row.label(),
                row.notation(),
                nodeType,
                row.hasChildren()
        );
    }

    public static ConceptTreeNodeData toCollectionConceptNode(Object[] row) {
        return new ConceptTreeNodeData(
                stringAt(row, 0),
                stringAt(row, 2),
                stringAt(row, 3),
                "file",
                false
        );
    }

    public static ConceptTreeNodeData toIndexSearchNode(Object[] row) {
        String conceptId = stringAt(row, 0);
        return new ConceptTreeNodeData(
                conceptId,
                defaultLabel(conceptId, stringAt(row, 1)),
                "",
                "file",
                false
        );
    }

    private static String defaultLabel(String id, String label) {
        return StringUtils.isBlank(label) ? "(" + id + ")" : label;
    }

    public static ConceptSummary toSummary(ConceptHeaderRow row) {
        return new ConceptSummary(
                row.conceptId(),
                row.thesaurusId(),
                row.prefLabel(),
                row.lang(),
                row.status(),
                row.idArk(),
                row.conceptType(),
                row.notation(),
                row.created(),
                row.modified(),
                row.creatorName()
        );
    }

    public static List<BreadcrumbStep> toBreadcrumb(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new BreadcrumbStep(
                        stringAt(row, 0),
                        stringAt(row, 1),
                        intAt(row, 2)
                ))
                .toList();
    }

    public static ConceptDetail toDetailFromFullConcept(
            ConceptFullSnapshot fullConcept,
            String thesaurusId,
            String lang,
            List<BreadcrumbStep> breadcrumb
    ) {
        String preferredLabel = fullConcept.getPrefLabel() != null
                ? StringUtils.defaultString(fullConcept.getPrefLabel().getLabel())
                : "";
        if (StringUtils.isBlank(preferredLabel)) {
            preferredLabel = "(" + fullConcept.getIdentifier() + ")";
        }
        var summary = new ConceptSummary(
                fullConcept.getIdentifier(),
                thesaurusId,
                preferredLabel,
                lang,
                ConceptResourceStatus.toDbStatus(fullConcept.getResourceStatus()),
                StringUtils.defaultString(fullConcept.getPermanentId()),
                StringUtils.defaultString(fullConcept.getConceptType()),
                StringUtils.defaultString(fullConcept.getNotation()),
                StringUtils.defaultString(fullConcept.getCreated()),
                StringUtils.defaultString(fullConcept.getModified()),
                StringUtils.defaultString(fullConcept.getCreatorName())
        );
        return new ConceptDetail(
                summary,
                breadcrumb,
                mapRelations(fullConcept.getBroaders()),
                mapRelations(fullConcept.getNarrowers()),
                mapRelations(fullConcept.getRelateds()),
                mapSynonyms(fullConcept.getAltLabels()),
                mapSynonyms(fullConcept.getHiddenLabels()),
                mapTranslations(fullConcept),
                mapNotes(fullConcept, lang),
                mapAlignmentGroupsFromFullConcept(fullConcept),
                mapUriLabels(fullConcept.getMembres()),
                mapUriLabels(fullConcept.getFacets()),
                mapUriLabels(fullConcept.getReplacedBy()),
                mapUriLabels(fullConcept.getReplaces())
        );
    }

    public static List<ConceptNote> mapNotes(ConceptFullSnapshot fullConcept, String lang, boolean allLangs) {
        var notes = new ArrayList<ConceptNote>();
        addNotes(notes, fullConcept.getNotes(), "note", lang, allLangs);
        addNotes(notes, fullConcept.getDefinitions(), "definition", lang, allLangs);
        addNotes(notes, fullConcept.getScopeNotes(), "scopeNote", lang, allLangs);
        addNotes(notes, fullConcept.getExamples(), "example", lang, allLangs);
        addNotes(notes, fullConcept.getEditorialNotes(), "editorialNote", lang, allLangs);
        addNotes(notes, fullConcept.getChangeNotes(), "changeNote", lang, allLangs);
        addNotes(notes, fullConcept.getHistoryNotes(), "historyNote", lang, allLangs);
        return List.copyOf(notes);
    }

    public static List<ConceptLabel> mapSynonymLabels(ConceptFullSnapshot fullConcept, String lang, boolean allLangs) {
        var labels = new ArrayList<ConceptLabel>();
        addSynonymLabels(labels, fullConcept.getAltLabels(), false, lang, allLangs);
        addSynonymLabels(labels, fullConcept.getHiddenLabels(), true, lang, allLangs);
        addSynonymLabels(labels, fullConcept.getAltLabelTraduction(), false, lang, allLangs);
        addSynonymLabels(labels, fullConcept.getHiddenLabelTraduction(), true, lang, allLangs);
        return List.copyOf(labels);
    }

    public static ConceptNote toNote(Object[] row) {
        return new ConceptNote(
                stringAt(row, 0),
                stringAt(row, 1),
                stringAt(row, 2),
                stringAt(row, 3)
        );
    }

    private static List<ConceptRelation> mapRelations(List<ConceptHierarchicalRelation> relations) {
        if (CollectionUtils.isEmpty(relations)) {
            return List.of();
        }
        return relations.stream()
                .map(relation -> new ConceptRelation(
                        StringUtils.defaultString(relation.getIdConcept()),
                        StringUtils.defaultString(relation.getLabel()),
                        StringUtils.defaultString(relation.getUri()),
                        StringUtils.defaultString(relation.getRole())
                ))
                .toList();
    }

    private static List<ConceptRelation> mapUriLabels(List<ConceptUriLabel> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .map(item -> new ConceptRelation(
                        StringUtils.defaultString(item.getIdentifier()),
                        StringUtils.defaultString(item.getLabel()),
                        StringUtils.defaultString(item.getUri())
                ))
                .toList();
    }

    private static List<String> mapSynonyms(List<ConceptTermLabel> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return List.of();
        }
        return labels.stream()
                .map(ConceptTermLabel::getLabel)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private static List<ConceptLabel> mapTranslations(ConceptFullSnapshot fullConcept) {
        var translations = new ArrayList<ConceptLabel>();
        addTranslationLabels(translations, fullConcept.getPrefLabelsTraduction(), true, false);
        addTranslationLabels(translations, fullConcept.getAltLabelTraduction(), false, false);
        addTranslationLabels(translations, fullConcept.getHiddenLabelTraduction(), false, true);
        return List.copyOf(translations);
    }

    private static void addTranslationLabels(
            List<ConceptLabel> target,
            List<ConceptTermLabel> labels,
            boolean preferred,
            boolean hidden
    ) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        labels.stream()
                .map(label -> new ConceptLabel(
                        StringUtils.defaultString(label.getIdLang()),
                        StringUtils.defaultString(label.getLabel()),
                        preferred,
                        hidden,
                        StringUtils.defaultString(label.getCodeFlag())
                ))
                .forEach(target::add);
    }

    private static List<ConceptNote> mapNotes(ConceptFullSnapshot fullConcept, String lang) {
        return mapNotes(fullConcept, lang, false);
    }

    private static void addNotes(
            List<ConceptNote> target,
            List<ConceptSnapshotNote> source,
            String typeCode,
            String lang,
            boolean allLangs
    ) {
        if (CollectionUtils.isEmpty(source)) {
            return;
        }
        source.stream()
                .filter(note -> allLangs || StringUtils.equalsIgnoreCase(note.getIdLang(), lang))
                .map(note -> new ConceptNote(
                        String.valueOf(note.getIdNote()),
                        typeCode,
                        StringUtils.defaultString(note.getIdLang()),
                        StringUtils.defaultString(note.getLabel()),
                        StringUtils.trimToNull(note.getNoteSource())
                ))
                .forEach(target::add);
    }

    private static void addSynonymLabels(
            List<ConceptLabel> target,
            List<ConceptTermLabel> source,
            boolean hidden,
            String lang,
            boolean allLangs
    ) {
        if (CollectionUtils.isEmpty(source)) {
            return;
        }
        source.stream()
                .filter(label -> allLangs || StringUtils.equalsIgnoreCase(label.getIdLang(), lang))
                .map(label -> new ConceptLabel(
                        StringUtils.defaultString(label.getIdLang()),
                        StringUtils.defaultString(label.getLabel()),
                        false,
                        hidden
                ))
                .forEach(target::add);
    }

    /**
     * Les identifiants numériques (1..5) correspondent aux lignes de seed de la table
     * {@code alignement_type} et doivent rester alignés avec le switch de
     * {@code ConceptFullAssembler.mapAlignments}, qui répartit les alignements par type dans
     * ces mêmes 5 catégories SKOS.
     */
    private static List<ConceptAlignmentGroup> mapAlignmentGroupsFromFullConcept(ConceptFullSnapshot fullConcept) {
        var groups = new ArrayList<ConceptAlignmentGroup>();
        addAlignmentGroup(groups, "exactMatch", "skos:exactMatch", 1, fullConcept.getExactMatchs());
        addAlignmentGroup(groups, "closeMatch", "skos:closeMatch", 2, fullConcept.getCloseMatchs());
        addAlignmentGroup(groups, "broadMatch", "skos:broadMatch", 3, fullConcept.getBroadMatchs());
        addAlignmentGroup(groups, "relatedMatch", "skos:relatedMatch", 4, fullConcept.getRelatedMatchs());
        addAlignmentGroup(groups, "narrowMatch", "skos:narrowMatch", 5, fullConcept.getNarrowMatchs());
        return List.copyOf(groups);
    }

    private static void addAlignmentGroup(
            List<ConceptAlignmentGroup> groups,
            String typeKey,
            String typeLabel,
            int typeId,
            List<ConceptUriLabel> items
    ) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        var alignments = items.stream()
                .map(item -> toAlignmentFromUriLabel(item, typeLabel, typeId))
                .toList();
        groups.add(new ConceptAlignmentGroup(typeKey, typeLabel, alignments));
    }

    private static ConceptAlignment toAlignmentFromUriLabel(ConceptUriLabel item, String typeLabel, int typeId) {
        String uri = StringUtils.defaultString(item.getUri());
        return new ConceptAlignment(
                StringUtils.defaultString(item.getIdentifier()),
                uri,
                typeLabel,
                StringUtils.defaultString(item.getLabel()),
                StringUtils.startsWithIgnoreCase(uri, "http"),
                typeId
        );
    }

    private static String resolveConceptNodeType(String status, boolean hasChildren) {
        if (ConceptStatusPolicy.isDeprecated(status)) {
            return "deprecated";
        }
        return hasChildren ? "concept" : "file";
    }

    public static String stringAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]);
    }

    private static boolean booleanAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return false;
        }
        Object value = row[index];
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static int intAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0;
        }
        Object value = row[index];
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
