package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import org.apache.commons.lang3.StringUtils;

public final class IdentifierServerSelection {

    private IdentifierServerSelection() {
    }

    public static void applyExclusive(PreferenceEditor editor, String componentId) {
        if (editor == null || StringUtils.isBlank(componentId)) {
            return;
        }
        boolean enabled = switch (componentId) {
            case "previewUseArk" -> editor.isUseArk();
            case "previewUseArkLocal" -> editor.isUseArkLocal();
            case "previewUseHandle" -> editor.isUseHandle();
            case "previewUseOpenArk" -> editor.isUseOpenArk();
            default -> false;
        };
        if (!enabled) {
            syncType(editor);
            return;
        }
        editor.setUseArk("previewUseArk".equals(componentId));
        editor.setUseArkLocal("previewUseArkLocal".equals(componentId));
        editor.setUseHandle("previewUseHandle".equals(componentId));
        editor.setUseOpenArk("previewUseOpenArk".equals(componentId));
        syncType(editor);
    }

    public static void syncType(PreferenceEditor editor) {
        if (editor == null) {
            return;
        }
        if (editor.isUseOpenArk()) {
            editor.setIdentifierServerType(IdentifierServerType.OPENARK);
        } else if (editor.isUseHandle()) {
            editor.setIdentifierServerType(IdentifierServerType.HANDLE);
        } else if (editor.isUseArkLocal()) {
            editor.setIdentifierServerType(IdentifierServerType.ARK_LOCAL);
        } else if (editor.isUseArk()) {
            editor.setIdentifierServerType(IdentifierServerType.ARK);
        } else {
            editor.setIdentifierServerType(IdentifierServerType.NONE);
        }
    }
}
