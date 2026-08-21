package fr.cnrs.opentheso.v2.collection.read;

import fr.cnrs.opentheso.v2.collection.model.CollectionDetail;
import fr.cnrs.opentheso.v2.collection.model.CollectionNoteItem;
import fr.cnrs.opentheso.v2.collection.model.CollectionTranslationItem;
import fr.cnrs.opentheso.v2.collection.model.CollectionTreeNode;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabelSort;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.shared.repository.CollectionTreeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CollectionTreeConsultationService {

    static final int MAX_MEMBER_CONCEPTS = 4_000;

    private final CollectionTreeQueryRepository collectionTreeQueryRepository;
    private final AuthenticatedUserSource authenticatedUserSource;

    public CollectionTreeConsultationService(
            CollectionTreeQueryRepository collectionTreeQueryRepository,
            AuthenticatedUserSource authenticatedUserSource
    ) {
        this.collectionTreeQueryRepository = collectionTreeQueryRepository;
        this.authenticatedUserSource = authenticatedUserSource;
    }

    @Transactional(readOnly = true)
    public List<CollectionTreeNode> loadRoots(String thesaurusId, String lang, boolean sortByNotation) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return List.of();
        }
        boolean includePrivate = authenticatedUserSource.isLoggedIn();
        List<CollectionTreeNode> nodes = new ArrayList<>();
        for (Object[] row : collectionTreeQueryRepository.findRootGroups(thesaurusId, lang, includePrivate)) {
            nodes.add(toGroupNode(row, "group"));
        }
        return sortNodes(nodes, sortByNotation);
    }

    @Transactional(readOnly = true)
    public List<CollectionTreeNode> loadChildren(
            String parentId,
            String thesaurusId,
            String lang,
            boolean sortByNotation
    ) {
        if (StringUtils.isAnyBlank(parentId, thesaurusId, lang)) {
            return List.of();
        }
        boolean includePrivate = authenticatedUserSource.isLoggedIn();
        List<CollectionTreeNode> groups = new ArrayList<>();
        for (Object[] row : collectionTreeQueryRepository.findChildGroups(
                parentId, thesaurusId, lang, includePrivate)) {
            groups.add(toGroupNode(row, "subGroup"));
        }
        List<CollectionTreeNode> concepts = new ArrayList<>();
        List<Object[]> members = collectionTreeQueryRepository.findMemberConcepts(
                parentId, thesaurusId, lang, MAX_MEMBER_CONCEPTS + 1);
        boolean truncated = members.size() > MAX_MEMBER_CONCEPTS;
        int limit = truncated ? MAX_MEMBER_CONCEPTS : members.size();
        for (int i = 0; i < limit; i++) {
            concepts.add(toConceptNode(members.get(i)));
        }
        List<CollectionTreeNode> children = new ArrayList<>(sortNodes(groups, sortByNotation));
        children.addAll(sortNodes(concepts, sortByNotation));
        if (truncated) {
            children.add(new CollectionTreeNode("....", "…", "", "more", false, ""));
        }
        return List.copyOf(children);
    }

    @Transactional(readOnly = true)
    public CollectionDetail loadDetail(String thesaurusId, String groupId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, groupId, lang)) {
            return CollectionDetail.empty();
        }
        boolean includePrivate = authenticatedUserSource.isLoggedIn();
        return collectionTreeQueryRepository.findGroupHeader(groupId, thesaurusId, lang, includePrivate)
                .map(header -> {
                    String typeCode = str(header, 5);
                    var type = collectionTreeQueryRepository.findGroupType(typeCode);
                    return new CollectionDetail(
                            str(header, 0),
                            str(header, 1),
                            lang,
                            typeCode,
                            type.map(row -> str(row, 0)).orElse(""),
                            type.map(row -> str(row, 1)).orElse(""),
                            collectionTreeQueryRepository.countMemberConcepts(thesaurusId, groupId),
                            str(header, 2),
                            str(header, 3),
                            str(header, 4),
                            str(header, 6),
                            str(header, 7),
                            collectionTreeQueryRepository.findGroupTranslations(groupId, thesaurusId, lang).stream()
                                    .map(row -> new CollectionTranslationItem(str(row, 0), str(row, 1)))
                                    .toList(),
                            collectionTreeQueryRepository.findGroupNotes(groupId, thesaurusId, lang).stream()
                                    .map(row -> new CollectionNoteItem(
                                            str(row, 0),
                                            noteTypeLabel(str(row, 0)),
                                            str(row, 1),
                                            str(row, 2)
                                    ))
                                    .toList()
                    );
                })
                .orElse(CollectionDetail.empty());
    }

    private static CollectionTreeNode toGroupNode(Object[] row, String nodeType) {
        String id = str(row, 0);
        String label = str(row, 1);
        if (label.isEmpty()) {
            label = id;
        }
        return new CollectionTreeNode(id, label, str(row, 2), nodeType, boolAt(row, 3), "");
    }

    private static CollectionTreeNode toConceptNode(Object[] row) {
        String id = str(row, 0);
        String label = str(row, 1);
        if (label.isEmpty()) {
            label = id;
        }
        String status = ConceptStatusPolicy.isDeprecated(str(row, 3)) ? "deprecie" : "valide";
        return new CollectionTreeNode(id, label, str(row, 2), "file", false, status);
    }

    static List<CollectionTreeNode> sortNodes(List<CollectionTreeNode> nodes, boolean sortByNotation) {
        if (nodes == null || nodes.size() <= 1) {
            return nodes == null ? List.of() : List.copyOf(nodes);
        }
        var sorted = new ArrayList<>(nodes);
        if (sortByNotation) {
            sorted.sort(Comparator
                    .comparing((CollectionTreeNode node) -> StringUtils.defaultString(node.notation()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(node -> StringUtils.defaultString(node.label()), ConceptLabelSort::compareLabels));
        } else {
            sorted.sort(Comparator.comparing(
                    (CollectionTreeNode node) -> StringUtils.defaultString(node.label()),
                    ConceptLabelSort::compareLabels));
        }
        return List.copyOf(sorted);
    }

    static String noteTypeLabel(String typeCode) {
        return switch (StringUtils.defaultString(typeCode)) {
            case "definition" -> "Définition";
            case "scopeNote" -> "Note d'application";
            case "example" -> "Exemple";
            case "historyNote" -> "Note historique";
            case "editorialNote" -> "Note éditoriale";
            case "changeNote" -> "Note de changement";
            default -> "Note";
        };
    }

    private static String str(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]).trim();
    }

    private static boolean boolAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return false;
        }
        Object value = row[index];
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
