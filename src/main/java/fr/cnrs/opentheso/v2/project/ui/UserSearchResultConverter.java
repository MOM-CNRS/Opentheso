package fr.cnrs.opentheso.v2.project.ui;

import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("userSearchResultConverter")
public class UserSearchResultConverter implements Converter<UserSearchResult> {

    @Override
    public UserSearchResult getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new UserSearchResult(Integer.parseInt(value), null, null);
        } catch (NumberFormatException e) {
            throw new ConverterException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Conversion Error",
                    "Not a valid user id"
            ));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, UserSearchResult value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value.userId());
    }
}
