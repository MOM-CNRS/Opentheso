package fr.cnrs.opentheso.v2.project.ui;

import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("projectSummaryConverter")
public class ProjectSummaryConverter implements Converter<ProjectSummary> {

    @Override
    public ProjectSummary getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new ProjectSummary(Integer.parseInt(value), null);
        } catch (NumberFormatException e) {
            throw new ConverterException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Conversion Error",
                    "Not a valid project id"
            ));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ProjectSummary value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value.id());
    }
}
