package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.*;

import java.util.ArrayList;
import java.util.List;

class ConceptMapper {

    static ConceptTreeNode toTreeNode(Object[] row) {
        return new ConceptTreeNode(
                str(row[0]),
                str(row[1]),
                str(row[2]),
                str(row[3]),
                str(row[4]),
                bool(row[5])
        );
    }

    static ConceptGroup toGroup(Object[] row) {
        return new ConceptGroup(
                str(row[0]),
                str(row[1]),
                str(row[2]),
                str(row[3]),
                row[4] == null ? null : ((Number) row[4]).intValue()
        );
    }

    static ConceptDetail toDetail(
            Object[] header,
            List<Object[]> labels,
            List<Object[]> relations,
            List<Object[]> breadcrumbRows,
            List<Object[]> notes,
            List<Object[]> alignments) {

        List<ConceptLabel> labelList = new ArrayList<>();
        for (Object[] r : labels) {
            labelList.add(new ConceptLabel(str(r[0]), str(r[1]), bool(r[2]), bool(r[3])));
        }

        List<ConceptRelationLink> relationList = new ArrayList<>();
        for (Object[] r : relations) {
            relationList.add(new ConceptRelationLink(str(r[0]), str(r[1]), str(r[2]), str(r[3])));
        }

        List<BreadcrumbStep> breadcrumb = new ArrayList<>();
        for (Object[] r : breadcrumbRows) {
            breadcrumb.add(new BreadcrumbStep(str(r[0]), str(r[1]), ((Number) r[2]).intValue()));
        }
        breadcrumb.sort((a, b) -> Integer.compare(b.depth(), a.depth()));

        List<ConceptNote> noteList = new ArrayList<>();
        for (Object[] r : notes) {
            noteList.add(new ConceptNote(((Number) r[0]).intValue(), str(r[1]), str(r[2]), str(r[3]), str(r[4])));
        }

        List<ConceptAlignment> alignmentList = new ArrayList<>();
        for (Object[] r : alignments) {
            alignmentList.add(new ConceptAlignment(
                    ((Number) r[0]).intValue(), str(r[1]), str(r[2]), str(r[3]), str(r[4]), bool(r[5])));
        }

        return new ConceptDetail(
                str(header[0]), str(header[1]), str(header[2]), str(header[3]),
                str(header[4]), str(header[5]), str(header[6]), str(header[7]), str(header[8]),
                breadcrumb, labelList, relationList, noteList, alignmentList
        );
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static boolean bool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(o.toString());
    }
}
