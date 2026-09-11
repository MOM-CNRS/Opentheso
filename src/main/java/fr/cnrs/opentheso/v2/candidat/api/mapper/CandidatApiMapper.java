package fr.cnrs.opentheso.v2.candidat.api.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.api.dto.CandidateSummaryResponse;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class CandidatApiMapper {

    private CandidatApiMapper() {
    }

    public static List<CandidateSummaryResponse> toSummaries(List<CandidatDto> candidates) {
        return candidates.stream().map(CandidatApiMapper::toSummary).toList();
    }

    public static CandidateSummaryResponse toSummary(CandidatDto candidat) {
        return new CandidateSummaryResponse(
                candidat.getIdConcepte(),
                candidat.getNomPref(),
                candidat.getLang(),
                resolveStatusLabel(candidat.getStatut()),
                candidat.getCreatedBy(),
                V2Dates.toInstant(candidat.getCreationDate()),
                Math.max(0, candidat.getNbrVote()),
                Math.max(0, candidat.getNbrDownVote()),
                candidat.getAdminMessage()
        );
    }

    private static String resolveStatusLabel(String status) {
        if (StringUtils.isBlank(status)) {
            return "pending";
        }
        return switch (status.trim()) {
            case "1", "pending" -> "pending";
            case "2", "accepted" -> "accepted";
            case "3", "rejected" -> "rejected";
            default -> status;
        };
    }

    public static int toStatusCode(String status) {
        if (status == null) {
            return CandidatStatusCode.PENDING;
        }
        return switch (status.toLowerCase()) {
            case "accepted", "2" -> CandidatStatusCode.ACCEPTED;
            case "rejected", "3" -> CandidatStatusCode.REJECTED;
            default -> CandidatStatusCode.PENDING;
        };
    }
}
