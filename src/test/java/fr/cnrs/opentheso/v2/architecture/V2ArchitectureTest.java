package fr.cnrs.opentheso.v2.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosImportPersistence;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.ui.ConceptListCsvImportBean;
import fr.cnrs.opentheso.v2.concept.write.ui.ConceptMaintenanceEditorBean;
import fr.cnrs.opentheso.v2.concept.write.ui.ConceptTypeManagerBean;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.v2.toolbox.edition.ui.ThesaurusEditionSkosImportBean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous V2 → legacy. Les classes listées en exclusion sont des ponts
 * migration explicitement tolérés tant qu'un port V2 n'existe pas.
 */
class V2ArchitectureTest {

    private static JavaClasses v2Classes;

    @BeforeAll
    static void importV2Classes() {
        v2Classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("fr.cnrs.opentheso.v2");
    }

    @Test
    void v2_mustNotDependOnLegacyBeans() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                // Sync langue de consultation V2 ↔ sélecteur legacy (SelectedTheso / RoleOnThesaurusBean).
                .and().doNotBelongToAnyOf(ThesaurusSearchLanguageSync.class)
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.bean..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacyServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                // Ponts temporairement branchés sur services legacy (CSV, types, restore, groups, prefs).
                .and().doNotBelongToAnyOf(
                        ConceptWriteMetadataService.class,
                        ConceptListCsvImportBean.class,
                        ConceptMaintenanceEditorBean.class,
                        ConceptTypeManagerBean.class,
                        ThesaurusEditionSkosImportBean.class
                )
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.services..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacyBridge() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.legacybridge..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacySkosImports() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                // Import SKOS concept encore délégué au moteur legacy.
                .and().doNotBelongToAnyOf(ConceptSkosImportPersistence.class)
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.imports..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacySkosExports() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                // Export candidats SKOS encore délégué au moteur legacy.
                .and().doNotBelongToAnyOf(CandidatExportService.class)
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.exports..");
        rule.check(v2Classes);
    }
}
