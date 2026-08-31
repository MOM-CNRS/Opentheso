package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.concept.model.ConceptTableRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptTableRowsResponse;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ConceptTableQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConceptTableConsultationService {

    static final int MAX_ROWS = 15_000;
    static final int PATH_DEPTH_CAP = 32;

    private final ConceptTableQueryRepository conceptTableQueryRepository;
    private final AuthenticatedUserSource authenticatedUserSource;

    public ConceptTableConsultationService(
            ConceptTableQueryRepository conceptTableQueryRepository,
            AuthenticatedUserSource authenticatedUserSource
    ) {
        this.conceptTableQueryRepository = conceptTableQueryRepository;
        this.authenticatedUserSource = authenticatedUserSource;
    }

    @Transactional(readOnly = true)
    public ConceptTableRowsResponse loadRows(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return new ConceptTableRowsResponse(List.of(), false);
        }
        boolean includeCandidates = authenticatedUserSource.isLoggedIn();
        List<Object[]> raw = conceptTableQueryRepository.findTableConceptRows(
                thesaurusId, lang, includeCandidates, MAX_ROWS + 1);
        boolean truncated = raw.size() > MAX_ROWS;
        if (truncated) {
            raw = new ArrayList<>(raw.subList(0, MAX_ROWS));
        }
        Map<String, String> parentOf = new HashMap<>();
        Map<String, String> parentLabel = new HashMap<>();
        for (Object[] edge : conceptTableQueryRepository.findBroaderEdges(thesaurusId, lang)) {
            String childId = str(edge, 0);
            if (childId.isEmpty() || parentOf.containsKey(childId)) {
                continue;
            }
            String parentId = str(edge, 1);
            parentOf.put(childId, parentId);
            parentLabel.putIfAbsent(parentId, str(edge, 2));
        }
        List<ConceptTableRow> rows = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            String id = str(row, 0);
            String label = str(row, 1);
            if (label.isEmpty()) {
                label = id;
            }
            String notation = str(row, 2);
            String dbStatus = str(row, 3);
            String typeCode = str(row, 4);
            String typeLabel = typeLabel(str(row, 5), typeCode);
            int candidatStatus = toInt(row, 6);
            String uiStatus = uiStatus(dbStatus, candidatStatus);
            rows.add(new ConceptTableRow(
                    id,
                    label,
                    uiStatus,
                    statusLabel(uiStatus),
                    typeLabel,
                    notation,
                    pathFor(id, parentOf, parentLabel),
                    str(row, 7),
                    str(row, 8)
            ));
        }
        return new ConceptTableRowsResponse(List.copyOf(rows), truncated);
    }

    static String uiStatus(String dbStatus, int candidatStatus) {
        if (candidatStatus == CandidatStatusCode.REJECTED) {
            return "rejete";
        }
        if (candidatStatus == CandidatStatusCode.ACCEPTED) {
            return ConceptStatusPolicy.isDeprecated(dbStatus) ? "deprecie" : "insere";
        }
        if ("CA".equalsIgnoreCase(StringUtils.trimToEmpty(dbStatus))) {
            return "candidat";
        }
        if (ConceptStatusPolicy.isDeprecated(dbStatus)) {
            return "deprecie";
        }
        return "valide";
    }

    static String statusLabel(String uiStatus) {
        return switch (uiStatus) {
            case "candidat" -> "Candidat";
            case "insere" -> "Inséré";
            case "rejete" -> "Rejeté";
            case "deprecie" -> "Déprécié";
            default -> "Normal";
        };
    }

    static String pathFor(String conceptId, Map<String, String> parentOf, Map<String, String> parentLabel) {
        if (StringUtils.isBlank(conceptId)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String current = parentOf.get(conceptId);
        int depth = 0;
        while (StringUtils.isNotBlank(current) && depth++ < PATH_DEPTH_CAP && seen.add(current)) {
            String label = parentLabel.get(current);
            parts.add(StringUtils.isBlank(label) ? current : label);
            current = parentOf.get(current);
        }
        if (parts.isEmpty()) {
            return "";
        }
        StringBuilder path = new StringBuilder(parts.get(parts.size() - 1));
        for (int i = parts.size() - 2; i >= 0; i--) {
            path.append(" › ").append(parts.get(i));
        }
        return path.toString();
    }

    private static String typeLabel(String dbLabel, String typeCode) {
        if (StringUtils.isNotBlank(dbLabel)) {
            return dbLabel;
        }
        if (StringUtils.isBlank(typeCode) || "concept".equalsIgnoreCase(typeCode)) {
            return "Concept";
        }
        return typeCode;
    }

    private static String str(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    private static int toInt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0;
        }
        if (row[index] instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(row[index].toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
