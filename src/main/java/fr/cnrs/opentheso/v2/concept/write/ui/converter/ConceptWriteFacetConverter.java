package fr.cnrs.opentheso.v2.concept.write.ui.converter;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import org.apache.commons.lang3.StringUtils;

@FacesConverter("v2ConceptFacetConverter")
public class ConceptWriteFacetConverter implements Converter<ConceptWriteFacet> {

    @Override
    public ConceptWriteFacet getAsObject(FacesContext context, UIComponent component, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new ConceptWriteFacet(value, value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ConceptWriteFacet value) {
        if (value == null) {
            return null;
        }
        return value.id();
    }
}
