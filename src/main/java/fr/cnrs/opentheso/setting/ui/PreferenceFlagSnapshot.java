package fr.cnrs.opentheso.setting.ui;

/**
 * Les cases à cocher absentes d'une requête ajax partielle sont décodées à faux.
 * L'état réel, lu dans la page au moment d'enregistrer, est réappliqué ici.
 */
public final class PreferenceFlagSnapshot {

    private PreferenceFlagSnapshot() {
    }

    public static void apply(PreferenceEditor editor, String payload) {
        if (editor == null || payload == null || payload.isBlank()) {
            return;
        }
        for (String line : payload.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            boolean value = "true".equals(line.substring(eq + 1).trim());
            if (!value && !"false".equals(line.substring(eq + 1).trim())) {
                continue;
            }
            switch (key) {
                case "numericIdentifier" -> editor.setNumericIdentifier(value);
                case "autoExpandTree" -> editor.setAutoExpandTree(value);
                case "treeCache" -> editor.setTreeCache(value);
                case "sortByNotation" -> editor.setSortByNotation(value);
                case "breadcrumb" -> editor.setBreadcrumb(value);
                case "useConceptTree" -> editor.setUseConceptTree(value);
                case "displayUserName" -> editor.setDisplayUserName(value);
                case "suggestion" -> editor.setSuggestion(value);
                case "useCustomRelation" -> editor.setUseCustomRelation(value);
                case "synchronisation" -> editor.setSynchronisation(value);
                case "portail" -> editor.setPortail(value);
                case "showCandidatesToGuests" -> editor.setShowCandidatesToGuests(value);
                case "showHistoryNote" -> editor.setShowHistoryNote(value);
                case "showEditorialNote" -> editor.setShowEditorialNote(value);
                case "webservices" -> editor.setWebservices(value);
                case "kohaLink" -> editor.setKohaLink(value);
                case "useDeeplTranslation" -> editor.setUseDeeplTranslation(value);
                case "useArk" -> editor.setUseArk(value);
                case "uppercaseForArk" -> editor.setUppercaseForArk(value);
                case "useArkLocal" -> editor.setUseArkLocal(value);
                case "useHandle" -> editor.setUseHandle(value);
                case "useHandleWithCertificat" -> editor.setUseHandleWithCertificat(value);
                case "useOpenArk" -> editor.setUseOpenArk(value);
                default -> {
                }
            }
        }
    }
}
