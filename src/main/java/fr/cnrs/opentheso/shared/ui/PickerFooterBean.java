package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.shared.model.PickerFooterLink;
import fr.cnrs.opentheso.shared.model.PickerFooterSettings;
import fr.cnrs.opentheso.shared.service.PickerFooterService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

@RequestScoped
@Named("pickerFooterBean")
@RequiredArgsConstructor
public class PickerFooterBean implements Serializable {

    private final transient PickerFooterService pickerFooterService;

    private transient PickerFooterSettings settings;

    public String getBrandLabel() {
        return current().brandLabel();
    }

    public String getBrandUrl() {
        return current().brandUrl();
    }

    public String getMarkUrl() {
        PickerFooterSettings current = current();
        return contextPath() + PickerFooterService.markPath(current.customImage(), current.imageVersion());
    }

    public List<PickerFooterLink> getLinks() {
        return current().links();
    }

    private PickerFooterSettings current() {
        if (settings == null) {
            settings = pickerFooterService.load();
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
