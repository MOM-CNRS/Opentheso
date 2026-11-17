package fr.cnrs.opentheso.admin.ui;

import fr.cnrs.opentheso.shared.model.PickerFooterLink;
import fr.cnrs.opentheso.shared.model.PickerFooterSettings;
import fr.cnrs.opentheso.shared.service.PickerFooterService;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("pickerFooterEditorBean")
@RequiredArgsConstructor
public class PickerFooterEditorBean implements Serializable {

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient PickerFooterService pickerFooterService;

    private String brandLabel = PickerFooterService.DEFAULT_BRAND_LABEL;
    private String brandUrl = PickerFooterService.DEFAULT_BRAND_URL;
    private boolean useDefaultImage;
    private boolean customImage;
    private long imageVersion;
    private List<PickerFooterLink> links = new ArrayList<>();
    private String linksPayload = "";
    private transient Part imageUpload;
    private String message = "";
    private boolean error;

    public boolean isAccessAllowed() {
        return userSession.canAccessSuperAdminScreen();
    }

    @PostConstruct
    public void load() {
        if (!isAccessAllowed()) {
            return;
        }
        apply(pickerFooterService.load());
        message = "";
        error = false;
    }

    public void save() {
        if (!isAccessAllowed()) {
            fail("admin.footer.error.denied");
            return;
        }
        links = new ArrayList<>(PickerFooterService.decode(linksPayload));
        byte[] image = null;
        try {
            if (imageUpload != null && imageUpload.getSize() > 0) {
                if (imageUpload.getSize() > PickerFooterService.MAX_IMAGE_BYTES) {
                    fail("admin.footer.error.image");
                    return;
                }
                try (InputStream in = imageUpload.getInputStream()) {
                    image = in.readAllBytes();
                }
            }
        } catch (IOException ex) {
            fail("admin.footer.error.image");
            return;
        }
        boolean clearImage = useDefaultImage && (image == null || image.length == 0);
        PickerFooterService.SaveResult result = pickerFooterService.save(
                brandLabel, brandUrl, linksPayload, image, clearImage);
        switch (result) {
            case INVALID_LABEL -> fail("admin.footer.error.name");
            case INVALID_URL -> fail("admin.footer.error.url");
            case INVALID_LINK -> fail("admin.footer.error.link");
            case TOO_MANY_LINKS -> fail("admin.footer.error.count");
            case INVALID_IMAGE -> fail("admin.footer.error.image");
            case SAVED -> {
                apply(pickerFooterService.load());
                error = false;
                message = localeBean.getMsg("admin.footer.saved");
            }
        }
    }

    public String getPreviewUrl() {
        return contextPath() + PickerFooterService.markPath(customImage, imageVersion);
    }

    private void apply(PickerFooterSettings settings) {
        brandLabel = settings.brandLabel();
        brandUrl = settings.brandUrl();
        customImage = settings.customImage();
        imageVersion = settings.imageVersion();
        links = new ArrayList<>(settings.links());
        linksPayload = PickerFooterService.encode(links);
        useDefaultImage = false;
    }

    private void fail(String key) {
        error = true;
        message = localeBean.getMsg(key);
    }

    private static String contextPath() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return "";
        }
        return StringUtils.defaultString(faces.getExternalContext().getRequestContextPath());
    }
}
