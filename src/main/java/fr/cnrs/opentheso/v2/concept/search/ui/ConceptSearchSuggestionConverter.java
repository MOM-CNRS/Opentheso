package fr.cnrs.opentheso.v2.concept.search.ui;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;

@Named("v2ConceptSearchSuggestionConverter")
@ApplicationScoped
@FacesConverter(value = "v2ConceptSearchSuggestionConverter", managed = true)
public class ConceptSearchSuggestionConverter implements Converter<ConceptSearchSuggestion> {

    @Override
    public ConceptSearchSuggestion getAsObject(FacesContext context, UIComponent component, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new ConceptSearchSuggestion(value, "", "", ConceptSearchKind.CONCEPT, false);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ConceptSearchSuggestion value) {
        return value == null ? null : value.getRefId();
    }
}
