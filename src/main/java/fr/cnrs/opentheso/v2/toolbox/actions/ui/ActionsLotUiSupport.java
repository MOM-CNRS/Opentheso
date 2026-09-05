package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCheckOutcome;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportPanelState;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Part;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

final class ActionsLotUiSupport {

    private ActionsLotUiSupport() {
    }

    static boolean isAvailable(ToolboxAccessPolicy policy, UserSession session, ThesaurusContext context) {
        return policy.canAccessWorkshop(session) && policy.hasSelectedThesaurus(context.resolveThesaurusId());
    }

    static String thesaurusTitle(ThesaurusContext context) {
        return StringUtils.defaultIfBlank(context.getCurrentThesaurusTitle(), "thésaurus courant");
    }

    static boolean guardAccess(ToolboxAccessPolicy policy, UserSession session, ThesaurusContext context) {
        if (!policy.canAccessWorkshop(session)) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return false;
        }
        if (StringUtils.isBlank(context.resolveThesaurusId())) {
            MessageUtils.showErrorMessage("Vous devez choisir un thésaurus avant !");
            return false;
        }
        return true;
    }

    static void loadFile(Part upload, ActionsLotImportPanelState<?> panel, Runnable update, String successToast) {
        try {
            byte[] bytes = readPart(upload);
            if (bytes == null) {
                panel.setGlobalError("Impossible de lire le fichier.");
                return;
            }
            panel.acceptFile(fileNameOf(upload), bytes);
            toast(successToast);
        } catch (Exception ex) {
            panel.setGlobalError(ex.getMessage());
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Upload impossible"));
        } finally {
            update.run();
        }
    }

    static byte[] readPart(Part part) throws IOException {
        if (part == null || part.getSize() <= 0) {
            return null;
        }
        return part.getInputStream().readAllBytes();
    }

    static String fileNameOf(Part part) {
        if (part == null || StringUtils.isBlank(part.getSubmittedFileName())) {
            return "fichier.csv";
        }
        return Paths.get(part.getSubmittedFileName()).getFileName().toString();
    }

    static void writeDownload(String filename, byte[] content) {
        FacesContext faces = FacesContext.getCurrentInstance();
        ExternalContext ext = faces.getExternalContext();
        ext.responseReset();
        ext.setResponseContentType("text/csv; charset=UTF-8");
        ext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        ext.setResponseContentLength(content.length);
        try (OutputStream out = ext.getResponseOutputStream()) {
            out.write(content);
            out.flush();
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Téléchargement impossible : " + ex.getMessage());
        }
        faces.responseComplete();
    }

    static void syncPanelClasses(String feature, String op, String cssClasses) {
        String safe = StringUtils.defaultString(cssClasses)
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('" + feature + "','" + op + "','" + safe + "')"
        );
    }

    static void toast(String message) {
        toast(message, false);
    }

    static void toast(String message, boolean error) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        String safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        String opts = error ? "{error:true}" : "{}";
        PrimeFaces.current().executeScript("window.toast && window.toast('" + safe + "', " + opts + ")");
    }

    static <R extends ActionsLotCheckOutcome> void validateFile(
            ActionsLotImportPanelState<?> panel,
            Runnable update,
            java.util.function.Supplier<R> validator,
            java.util.function.Consumer<R> applyToPanel,
            String readyLabel
    ) {
        if (!panel.isHasFile() || panel.getFileBytes() == null) {
            panel.setGlobalError("Déposez un fichier CSV avant de valider.");
            update.run();
            return;
        }
        panel.setBusy(true);
        try {
            R result = validator.get();
            applyToPanel.accept(result);
            if (!result.success()) {
                MessageUtils.showErrorMessage(result.errorMessage());
                toast(result.errorMessage(), true);
            } else if (result.hasErrors()) {
                toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
            } else {
                String msg = result.validCount() + " " + readyLabel;
                if (result.ignoredCount() > 0) {
                    msg += " (" + result.ignoredCount() + " ignorée(s))";
                }
                toast(msg);
            }
        } finally {
            panel.setBusy(false);
            update.run();
        }
    }

    static void applyFile(
            ActionsLotImportPanelState<?> panel,
            Runnable update,
            java.util.function.Supplier<ActionsLotApplyResult> applier,
            String emptyMessage
    ) {
        if (panel.getValidCandidates().isEmpty()) {
            MessageUtils.showErrorMessage(emptyMessage);
            return;
        }
        panel.setBusy(true);
        try {
            ActionsLotApplyResult result = applier.get();
            panel.applyResult(result);
            if (result.success()) {
                MessageUtils.showInformationMessage(result.message());
                toast(result.message());
            } else {
                MessageUtils.showErrorMessage(result.message());
                toast(result.message(), true);
            }
        } finally {
            panel.setBusy(false);
            update.run();
        }
    }
}
