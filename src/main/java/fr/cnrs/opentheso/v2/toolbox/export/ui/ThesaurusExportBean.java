package fr.cnrs.opentheso.v2.toolbox.export.ui;

import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvDeprecatedExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvIdExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvStructuredExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionPdfExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionZipExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("v2ThesaurusExportBean")
public class ThesaurusExportBean implements Serializable {

    private final ThesaurusEditionSkosExportService thesaurusEditionSkosExportService;
    private final ThesaurusEditionCsvExportService thesaurusEditionCsvExportService;
    private final ThesaurusEditionCsvIdExportService thesaurusEditionCsvIdExportService;
    private final ThesaurusEditionCsvStructuredExportService thesaurusEditionCsvStructuredExportService;
    private final ThesaurusEditionPdfExportService thesaurusEditionPdfExportService;
    private final ThesaurusEditionCsvDeprecatedExportService thesaurusEditionCsvDeprecatedExportService;
    private final ThesaurusEditionZipExportService thesaurusEditionZipExportService;
    private final ToolboxExportPersistence toolboxExportPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    private String thesaurusId;
    private String thesaurusTitle;
    private String formatCode = "rdf";
    private String csvDelimiter = ",";
    private List<String> selectedLanguageCodes = new ArrayList<>();
    private List<NodeLangTheso> exportLanguages = Collections.emptyList();
    private List<NodeGroup> groupList = Collections.emptyList();
    private List<String> selectedGroupIds = new ArrayList<>();

    private boolean filterByGroup;
    private boolean exportByGroup;
    private boolean clearHtml;
    private boolean includeImages;

    private String pdfLanguage1;
    private String pdfLanguage2;
    private boolean pdfHierarchical = true;
    private String csvIdLanguage;
    private String structuredCsvLanguage;
    private String deprecatedLanguage;

    public ThesaurusExportBean(
            ThesaurusEditionSkosExportService thesaurusEditionSkosExportService,
            ThesaurusEditionCsvExportService thesaurusEditionCsvExportService,
            ThesaurusEditionCsvIdExportService thesaurusEditionCsvIdExportService,
            ThesaurusEditionCsvStructuredExportService thesaurusEditionCsvStructuredExportService,
            ThesaurusEditionPdfExportService thesaurusEditionPdfExportService,
            ThesaurusEditionCsvDeprecatedExportService thesaurusEditionCsvDeprecatedExportService,
            ThesaurusEditionZipExportService thesaurusEditionZipExportService,
            ToolboxExportPersistence toolboxExportPersistence,
            ToolboxPreferencePersistence toolboxPreferencePersistence
    ) {
        this.thesaurusEditionSkosExportService = thesaurusEditionSkosExportService;
        this.thesaurusEditionCsvExportService = thesaurusEditionCsvExportService;
        this.thesaurusEditionCsvIdExportService = thesaurusEditionCsvIdExportService;
        this.thesaurusEditionCsvStructuredExportService = thesaurusEditionCsvStructuredExportService;
        this.thesaurusEditionPdfExportService = thesaurusEditionPdfExportService;
        this.thesaurusEditionCsvDeprecatedExportService = thesaurusEditionCsvDeprecatedExportService;
        this.thesaurusEditionZipExportService = thesaurusEditionZipExportService;
        this.toolboxExportPersistence = toolboxExportPersistence;
        this.toolboxPreferencePersistence = toolboxPreferencePersistence;
    }

    public void init(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        this.formatCode = "rdf";
    }

    public void initCsv(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        this.csvDelimiter = ",";
        loadLanguagesAndGroups();
    }

    public void initCsvById(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        this.csvDelimiter = ",";
        loadLanguagesAndGroups();
        this.csvIdLanguage = resolveDefaultLanguage();
    }

    public void initCsvStructured(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        loadLanguagesAndGroups();
        this.structuredCsvLanguage = resolveDefaultLanguage();
    }

    public void initPdf(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        loadLanguagesAndGroups();
        this.pdfLanguage1 = resolveDefaultLanguage();
        this.pdfLanguage2 = null;
        this.pdfHierarchical = true;
        this.includeImages = false;
    }

    public void initDeprecated(String thesaurusId, String thesaurusTitle) {
        resetCommon(thesaurusId, thesaurusTitle);
        this.csvDelimiter = ";";
        exportLanguages = thesaurusEditionCsvExportService.listExportLanguages(thesaurusId);
        this.deprecatedLanguage = resolveDefaultLanguage();
    }

    public void prepare(String thesaurusId, String thesaurusTitle) {
        this.thesaurusId = thesaurusId;
        this.thesaurusTitle = thesaurusTitle;
    }

    public StreamedContent downloadSkos() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            if (exportByGroup) {
                return thesaurusEditionZipExportService.exportEachGroupAsSkosZip(
                        thesaurusId,
                        thesaurusTitle,
                        formatCode,
                        clearHtml
                );
            }
            return thesaurusEditionSkosExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    formatCode,
                    buildExportOptions()
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export SKOS impossible");
            return null;
        }
    }

    public StreamedContent downloadCsv() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            if (exportByGroup) {
                return thesaurusEditionZipExportService.exportEachGroupAsCsvZip(
                        thesaurusId,
                        thesaurusTitle,
                        CsvDelimiterSupport.resolveDelimiter(csvDelimiter),
                        selectedLanguageCodes,
                        clearHtml
                );
            }
            return thesaurusEditionCsvExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    CsvDelimiterSupport.resolveDelimiter(csvDelimiter),
                    selectedLanguageCodes,
                    buildExportOptions()
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export CSV impossible");
            return null;
        }
    }

    public StreamedContent downloadCsvById() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            return thesaurusEditionCsvIdExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    csvIdLanguage,
                    CsvDelimiterSupport.resolveDelimiter(csvDelimiter),
                    filterByGroup,
                    selectedGroupIds
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export CSV par ID impossible");
            return null;
        }
    }

    public StreamedContent downloadCsvStructured() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            return thesaurusEditionCsvStructuredExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    structuredCsvLanguage
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export CSV structuré impossible");
            return null;
        }
    }

    public StreamedContent downloadPdf() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            return thesaurusEditionPdfExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    pdfLanguage1,
                    pdfLanguage2,
                    pdfHierarchical,
                    includeImages,
                    buildExportOptions()
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export PDF impossible");
            return null;
        }
    }

    public StreamedContent downloadCsvDeprecated() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            return thesaurusEditionCsvDeprecatedExportService.exportThesaurus(
                    thesaurusId,
                    thesaurusTitle,
                    deprecatedLanguage,
                    CsvDelimiterSupport.resolveDelimiter(csvDelimiter)
            );
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export concepts dépréciés impossible");
            return null;
        }
    }

    private void resetCommon(String thesaurusId, String thesaurusTitle) {
        this.thesaurusId = thesaurusId;
        this.thesaurusTitle = thesaurusTitle;
        this.csvDelimiter = ",";
        this.exportLanguages = Collections.emptyList();
        this.selectedLanguageCodes = new ArrayList<>();
        this.groupList = Collections.emptyList();
        this.selectedGroupIds = new ArrayList<>();
        this.filterByGroup = false;
        this.exportByGroup = false;
        this.clearHtml = false;
        this.includeImages = false;
    }

    private void loadLanguagesAndGroups() {
        exportLanguages = thesaurusEditionCsvExportService.listExportLanguages(thesaurusId);
        selectedLanguageCodes = exportLanguages.stream().map(NodeLangTheso::getCode).toList();
        groupList = toolboxExportPersistence.loadConceptGroups(thesaurusId);
        selectedGroupIds = groupList.stream()
                .map(group -> group.getConceptGroup().getIdGroup())
                .toList();
    }

    private String resolveDefaultLanguage() {
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        if (StringUtils.isNotBlank(workLang)) {
            return workLang;
        }
        return exportLanguages.isEmpty() ? "fr" : exportLanguages.get(0).getCode();
    }

    private ThesaurusEditionExportOptions buildExportOptions() {
        List<String> groups = filterByGroup && CollectionUtils.isNotEmpty(selectedGroupIds)
                ? selectedGroupIds
                : List.of();
        return new ThesaurusEditionExportOptions(filterByGroup, groups, clearHtml);
    }
}
