package fr.cnrs.opentheso.admin.ui;

import fr.cnrs.opentheso.shared.model.PickerHeaderSettings;
import fr.cnrs.opentheso.shared.service.PickerHeaderService;
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

@Getter
@Setter
@ViewScoped
@Named("pickerHeaderEditorBean")
@RequiredArgsConstructor
public class PickerHeaderEditorBean implements Serializable {

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient PickerHeaderService pickerHeaderService;

    private boolean visible = true;
    private String label = PickerHeaderService.DEFAULT_LABEL;
    private String url = PickerHeaderService.DEFAULT_URL;
    private boolean publishedVisible = true;
    private String publishedLabel = PickerHeaderService.DEFAULT_LABEL;
    private String publishedUrl = PickerHeaderService.DEFAULT_URL;
    private boolean useDefaultImage;
    private boolean customImage;
    private long imageVersion;
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
        apply(pickerHeaderService.load());
        message = "";
        error = false;
    }

    public void save() {
        if (!isAccessAllowed()) {
            fail("admin.banner.error.denied");
            return;
        }
        byte[] image = null;
        try {
            if (imageUpload != null && imageUpload.getSize() > 0) {
                if (imageUpload.getSize() > PickerHeaderService.MAX_IMAGE_BYTES) {
                    fail("admin.banner.error.image");
                    return;
                }
                try (InputStream in = imageUpload.getInputStream()) {
                    image = in.readAllBytes();
                }
            }
        } catch (IOException ex) {
            fail("admin.banner.error.image");
            return;
        }
        boolean clearImage = useDefaultImage && (image == null || image.length == 0);
        PickerHeaderService.SaveResult result = pickerHeaderService.save(visible, label, url, image, clearImage);
        switch (result) {
            case INVALID_LABEL -> fail("admin.banner.error.label");
            case INVALID_URL -> fail("admin.banner.error.url");
            case INVALID_IMAGE -> fail("admin.banner.error.image");
            case SAVED -> {
                apply(pickerHeaderService.load());
                error = false;
                message = localeBean.getMsg("admin.banner.saved");
            }
        }
    }

    public String getPreviewUrl() {
        return contextPath() + PickerHeaderService.markPath(customImage, imageVersion);
    }

    private void apply(PickerHeaderSettings settings) {
        visible = settings.visible();
        label = settings.label();
        url = settings.url();
        publishedVisible = settings.visible();
        publishedLabel = settings.label();
        publishedUrl = settings.url();
        customImage = settings.customImage();
        imageVersion = settings.imageVersion();
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
