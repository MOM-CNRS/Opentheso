package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateNote {

    private String type;

    private String value;

    private String lang;

    private String source;
}
