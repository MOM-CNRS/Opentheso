package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.primefaces.extensions.event.ClipboardErrorEvent;
import org.primefaces.extensions.event.ClipboardSuccessEvent;

import java.io.Serializable;

@Named("clipboardFeedbackBean")
@ApplicationScoped
@RequiredArgsConstructor
public class ClipboardFeedbackBean implements Serializable {

    private final V2LocaleBean localeBean;

    public void onSuccess(final ClipboardSuccessEvent successEvent) {
        MessageUtils.showInformationMessage(localeBean.getMsg("copied"));
    }

    public void onError(final ClipboardErrorEvent errorEvent) {
        MessageUtils.showErrorMessage("Component id: " + errorEvent.getComponent().getId()
                + " Action: " + errorEvent.getAction());
    }
}
