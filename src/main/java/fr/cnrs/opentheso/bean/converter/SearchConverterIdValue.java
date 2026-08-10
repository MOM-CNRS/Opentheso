package fr.cnrs.opentheso.bean.converter;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;


@FacesConverter("searchConverterIdValue")
public class SearchConverterIdValue implements Converter<NodeIdValue> {

    @Override
    public NodeIdValue getAsObject(FacesContext context, UIComponent component, String value) {
        if (value != null && value.trim().length() > 0) {
            NodeIdValue niv = new NodeIdValue();
            niv.setId(value);
            return niv;
        }
        else {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, NodeIdValue value) {
        if (value != null) {
            return String.valueOf(value.getId());
        }
        else {
            return null;
        }
    }
}
