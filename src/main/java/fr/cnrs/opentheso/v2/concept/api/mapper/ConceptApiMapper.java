package fr.cnrs.opentheso.v2.concept.api.mapper;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptBreadcrumbResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptSummaryResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptTreeNodeResponse;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptDetailResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptLabelResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptNoteResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptRelationResponse;

public final class ConceptApiMapper {

    private ConceptApiMapper() {
    }

    public static ConceptTreeNodeResponse toTreeNode(ConceptTreeNodeData node) {
        return new ConceptTreeNodeResponse(
                node.nodeId(),
                node.label(),
                node.notation(),
                node.nodeType(),
                node.hasChildren()
        );
    }

    public static ConceptSummaryResponse toSummary(ConceptSummary summary) {
        return new ConceptSummaryResponse(
                summary.conceptId(),
                summary.thesaurusId(),
                summary.preferredLabel(),
                summary.lang(),
                summary.status(),
                summary.arkId(),
                summary.conceptType(),
                summary.created(),
                summary.modified()
        );
    }

    public static ConceptBreadcrumbResponse toBreadcrumb(BreadcrumbStep step) {
        return new ConceptBreadcrumbResponse(step.conceptId(), step.label(), String.valueOf(step.depth()));
    }

    public static ConceptDetailResponse toDetail(ConceptDetail detail) {
        return new ConceptDetailResponse(
                toSummary(detail.summary()),
                detail.breadcrumb().stream().map(ConceptApiMapper::toBreadcrumb).toList(),
                detail.broaderTerms().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.narrowerTerms().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.relatedTerms().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.synonyms(),
                detail.hiddenSynonyms(),
                detail.translations().stream().map(ConceptApiMapper::toLabel).toList(),
                detail.notes().stream().map(ConceptApiMapper::toNote).toList(),
                detail.collections().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.facets().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.replacedBy().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.replaces().stream().map(ConceptApiMapper::toRelation).toList(),
                detail.preferredTermId()
        );
    }

    public static ConceptRelationResponse toRelation(ConceptRelation relation) {
        return new ConceptRelationResponse(relation.conceptId(), relation.label(), relation.arkId());
    }

    public static ConceptLabelResponse toLabel(ConceptLabel label) {
        return new ConceptLabelResponse(label.lang(), label.value(), label.hidden(), label.preferred());
    }

    public static ConceptNoteResponse toNote(ConceptNote note) {
        return new ConceptNoteResponse(note.id(), note.typeCode(), note.lang(), note.value());
    }
}
