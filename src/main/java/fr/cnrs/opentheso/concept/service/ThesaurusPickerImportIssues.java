package fr.cnrs.opentheso.concept.service;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lignes rejetées ou incomplètes détectées pendant l'analyse, avant l'écriture.
 */
public final class ThesaurusPickerImportIssues {

    public static final int STORE_LIMIT = 200;

    public static final String MISSING_ID = "missingId";
    public static final String MISSING_LABEL = "missingLabel";
    public static final String WEAK_LABEL = "weakLabel";
    public static final String DROPPED_PARENT = "droppedParent";
    public static final String RAW = "raw";

    private static final Pattern RECORD_NUMBER = Pattern.compile("recordNumber=(\\d+)");

    private ThesaurusPickerImportIssues() {
    }

    public static Result fromCsv(String warning, List<ThesaurusCsvConceptObject> concepts) {
        List<ImportIssue> issues = new ArrayList<>();
        int total = 0;
        if (StringUtils.isNotBlank(warning)) {
            for (String rawLine : warning.split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                total++;
                if (issues.size() < STORE_LIMIT) {
                    int record = recordNumber(line);
                    if (line.toLowerCase(Locale.ROOT).contains("sans id")) {
                        issues.add(new ImportIssue(record, MISSING_ID, ""));
                    } else {
                        issues.add(new ImportIssue(record, RAW, StringUtils.abbreviate(line, 240)));
                    }
                }
            }
        }
        if (concepts != null) {
            for (ThesaurusCsvConceptObject concept : concepts) {
                if (concept == null || hasPrefLabel(concept)) {
                    continue;
                }
                total++;
                if (issues.size() < STORE_LIMIT) {
                    String ref = StringUtils.firstNonBlank(concept.getIdConcept(), concept.getUri(), "");
                    issues.add(new ImportIssue(0, WEAK_LABEL, StringUtils.abbreviate(ref, 120)));
                }
            }
        }
        return new Result(issues, total);
    }

    public static Result fromSkos(SKOSXmlDocument document) {
        List<ImportIssue> issues = new ArrayList<>();
        int total = 0;
        if (document == null || document.getConceptList() == null) {
            return new Result(issues, 0);
        }
        int index = 0;
        for (SKOSResource resource : document.getConceptList()) {
            index++;
            if (resource == null || hasAnyLabel(resource)) {
                continue;
            }
            total++;
            if (issues.size() < STORE_LIMIT) {
                String ref = StringUtils.firstNonBlank(resource.getIdentifier(), resource.getUri(), "");
                issues.add(new ImportIssue(index, MISSING_LABEL, StringUtils.abbreviate(ref, 120)));
            }
        }
        return new Result(issues, total);
    }

    public static Result fromStructured(NodeTree root) {
        List<ImportIssue> issues = new ArrayList<>();
        int[] total = {0};
        if (root != null && root.getChildrens() != null) {
            int[] seen = {0};
            for (NodeTree child : root.getChildrens()) {
                walk(child, false, issues, seen, total);
            }
        }
        return new Result(issues, total[0]);
    }

    private static void walk(NodeTree node, boolean ancestorDropped, List<ImportIssue> issues, int[] seen, int[] total) {
        if (node == null) {
            return;
        }
        seen[0]++;
        boolean blank = StringUtils.isBlank(node.getPreferredTerm());
        boolean dropped = ancestorDropped || blank;
        if (dropped) {
            total[0]++;
            if (issues.size() < STORE_LIMIT) {
                if (blank) {
                    issues.add(new ImportIssue(seen[0], MISSING_LABEL, ""));
                } else {
                    issues.add(new ImportIssue(seen[0], DROPPED_PARENT,
                            StringUtils.abbreviate(node.getPreferredTerm().trim(), 120)));
                }
            }
        }
        if (node.getChildrens() == null) {
            return;
        }
        for (NodeTree child : node.getChildrens()) {
            walk(child, dropped, issues, seen, total);
        }
    }

    private static boolean hasPrefLabel(ThesaurusCsvConceptObject concept) {
        if (concept.getPrefLabels() == null) {
            return false;
        }
        for (ThesaurusCsvConceptLabel label : concept.getPrefLabels()) {
            if (label != null && StringUtils.isNotBlank(label.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyLabel(SKOSResource resource) {
        if (resource.getLabelsList() == null || resource.getLabelsList().isEmpty()) {
            return false;
        }
        for (SKOSLabel label : resource.getLabelsList()) {
            if (label != null && StringUtils.isNotBlank(label.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private static int recordNumber(String line) {
        Matcher matcher = RECORD_NUMBER.matcher(line);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public record ImportIssue(int line, String code, String ref) implements Serializable {
    }

    public record Result(List<ImportIssue> issues, int total) implements Serializable {
    }
}
