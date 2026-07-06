package fr.cnrs.opentheso.v2.concept.write.ui.converter;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import org.apache.commons.lang3.StringUtils;

@FacesConverter("v2ConceptCollectionConverter")
public class ConceptWriteCollectionConverter implements Converter<ConceptWriteCollection> {

    @Override
    public ConceptWriteCollection getAsObject(FacesContext context, UIComponent component, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new ConceptWriteCollection(value, value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ConceptWriteCollection value) {
        if (value == null) {
            return null;
        }
        return value.id();
    }
}
