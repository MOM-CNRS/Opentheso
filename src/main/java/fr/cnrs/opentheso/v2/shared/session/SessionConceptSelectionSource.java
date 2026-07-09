package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class SessionConceptSelectionSource implements ConceptSelectionSource {

    private final ConceptSelectionContext conceptSelectionContext;

    @Override
    public Optional<String> getSelectedConceptId() {
        return Optional.ofNullable(conceptSelectionContext.getConceptId()).filter(StringUtils::isNotBlank);
    }
}
