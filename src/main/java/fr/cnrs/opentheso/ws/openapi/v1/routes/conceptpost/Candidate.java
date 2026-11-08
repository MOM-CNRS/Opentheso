package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate implements Serializable {

    private List<Element> terme;

    private String thesoId;

    private String collectionId;

    private String conceptGenericId;

    private List<Element> definition;

    private List<Element> note;

    private List<CandidateNote> notes;

    private String source;

    private List<Element> synonymes;

    private String comment;
}
