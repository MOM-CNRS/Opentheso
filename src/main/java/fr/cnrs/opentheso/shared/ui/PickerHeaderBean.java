package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.shared.model.PickerHeaderSettings;
import fr.cnrs.opentheso.shared.service.PickerHeaderService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@RequestScoped
@Named("pickerHeaderBean")
@RequiredArgsConstructor
public class PickerHeaderBean implements Serializable {

    private final transient PickerHeaderService pickerHeaderService;

    private transient PickerHeaderSettings settings;

    public boolean isVisible() {
        return current().visible();
    }

    public String getLinkLabel() {
        return current().label();
    }

    public String getLinkUrl() {
        return current().url();
    }

    public String getMarkUrl() {
        PickerHeaderSettings current = current();
        return contextPath() + PickerHeaderService.markPath(current.customImage(), current.imageVersion());
    }

    private PickerHeaderSettings current() {
        if (settings == null) {
            settings = pickerHeaderService.load();
        }
        return settings;
    }

    private static String contextPath() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return "";
        }
        return StringUtils.defaultString(faces.getExternalContext().getRequestContextPath());
    }
}
