package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;

import java.util.List;
import java.util.Optional;

public interface ConceptWriteMetadataPort {

    List<ConceptWriteLanguage> listUsedLanguages(String thesaurusId, String workLang);

    List<ConceptWriteNoteType> listNoteTypes();

    List<ConceptWriteNtRelationType> listNtRelationTypes();

    List<ConceptWriteConceptType> listConceptTypes(String thesaurusId);

    Optional<ConceptWriteNoteDraft> loadNoteDraft(
            String thesaurusId,
            String conceptId,
            String lang,
            String typeCode
    );
}
