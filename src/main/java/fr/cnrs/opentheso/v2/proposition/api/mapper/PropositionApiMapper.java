package fr.cnrs.opentheso.v2.proposition.api.mapper;

import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import fr.cnrs.opentheso.v2.proposition.api.dto.PropositionDetailResponse;
import fr.cnrs.opentheso.v2.proposition.api.dto.PropositionSummaryResponse;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class PropositionApiMapper {

    private PropositionApiMapper() {
    }

    public static List<PropositionSummaryResponse> toSummaries(List<PropositionSummary> summaries) {
        return summaries.stream().map(PropositionApiMapper::toSummary).toList();
    }

    public static PropositionSummaryResponse toSummary(PropositionSummary summary) {
        return new PropositionSummaryResponse(
                summary.id(),
                summary.conceptId(),
                summary.conceptLabel(),
                summary.lang(),
                toStatusLabel(summary.status()),
                summary.authorName(),
                summary.authorEmail(),
                summary.publishedAt()
        );
    }

    public static PropositionDetailResponse toDetail(PropositionDetail detail) {
        return new PropositionDetailResponse(
                detail.id(),
                detail.thesaurusId(),
                detail.conceptId(),
                detail.conceptLabel(),
                detail.lang(),
                toStatusLabel(detail.status()),
                detail.authorName(),
                detail.authorEmail(),
                detail.comment(),
                detail.publishedAt(),
                detail.reviewedBy(),
                detail.adminComment()
        );
    }

    public static String toStatusLabel(String status) {
        if (StringUtils.isBlank(status)) {
            return "pending";
        }
        return switch (status.trim().toUpperCase()) {
            case "ENVOYER" -> "pending";
            case "LU" -> "read";
            case "APPROUVER" -> "approved";
            case "REFUSER" -> "refused";
            default -> status.toLowerCase();
        };
    }

    public static boolean isPendingFilter(String status) {
        if (StringUtils.isBlank(status)) {
            return true;
        }
        String normalized = status.trim().toLowerCase();
        return "pending".equals(normalized)
                || "unread".equals(normalized)
                || "new".equals(normalized)
                || PropositionStatusEnum.ENVOYER.name().equalsIgnoreCase(normalized);
    }
}
