package fr.cnrs.opentheso.v2.concept.write.ui.converter;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("v2ConceptSearchConverter")
public class ConceptSearchSuggestionConverter implements Converter<ConceptSearchSuggestion> {

    @Override
    public ConceptSearchSuggestion getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new ConceptSearchSuggestion(value, value, null, false);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ConceptSearchSuggestion value) {
        if (value == null) {
            return null;
        }
        return value.conceptId();
    }
}
