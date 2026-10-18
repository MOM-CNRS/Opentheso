package fr.cnrs.opentheso.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import fr.cnrs.opentheso.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.concept.io.rdf.ConceptSkosImportPersistence;
import fr.cnrs.opentheso.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.concept.write.ui.ConceptListCsvImportBean;
import fr.cnrs.opentheso.concept.write.ui.ConceptMaintenanceEditorBean;
import fr.cnrs.opentheso.concept.write.ui.ConceptTypeManagerBean;
import fr.cnrs.opentheso.toolbox.edition.ui.ThesaurusEditionSkosImportBean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Les modules d'application ne dépendent pas de l'ancienne interface JSF.
 * Les classes listées en exclusion sont des ponts encore branchés sur {@code services} ou SKOS.
 */
class ArchitectureTest {

    private static final String[] APPLICATION_PACKAGES = {
            "fr.cnrs.opentheso.admin",
            "fr.cnrs.opentheso.candidat",
            "fr.cnrs.opentheso.collection",
            "fr.cnrs.opentheso.concept",
            "fr.cnrs.opentheso.consultation",
            "fr.cnrs.opentheso.facet",
            "fr.cnrs.opentheso.graph",
            "fr.cnrs.opentheso.portal",
            "fr.cnrs.opentheso.project",
            "fr.cnrs.opentheso.proposition",
            "fr.cnrs.opentheso.publicapi",
            "fr.cnrs.opentheso.rights",
            "fr.cnrs.opentheso.setting",
            "fr.cnrs.opentheso.shared",
            "fr.cnrs.opentheso.stats",
            "fr.cnrs.opentheso.sync",
            "fr.cnrs.opentheso.toolbox",
            "fr.cnrs.opentheso.user"
    };

    private static JavaClasses applicationClasses;

    @BeforeAll
    static void importApplicationClasses() {
        applicationClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(APPLICATION_PACKAGES);
    }

    private static String[] applicationPackagePatterns() {
        String[] patterns = new String[APPLICATION_PACKAGES.length];
        for (int i = 0; i < APPLICATION_PACKAGES.length; i++) {
            patterns[i] = APPLICATION_PACKAGES[i] + "..";
        }
        return patterns;
    }

    @Test
    void applicationMustNotDependOnLegacyBeans() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(applicationPackagePatterns())
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.bean..");
        rule.check(applicationClasses);
    }

    @Test
    void applicationMustNotDependOnLegacyServices() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(applicationPackagePatterns())
                .and().doNotBelongToAnyOf(
                        ConceptWriteMetadataService.class,
                        ConceptListCsvImportBean.class,
                        ConceptMaintenanceEditorBean.class,
                        ConceptTypeManagerBean.class,
                        ThesaurusEditionSkosImportBean.class
                )
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.services..");
        rule.check(applicationClasses);
    }

    @Test
    void applicationMustNotDependOnLegacyBridge() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(applicationPackagePatterns())
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.legacybridge..");
        rule.check(applicationClasses);
    }

    @Test
    void applicationMustNotDependOnLegacySkosImports() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(applicationPackagePatterns())
                .and().doNotBelongToAnyOf(ConceptSkosImportPersistence.class)
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.imports..");
        rule.check(applicationClasses);
    }

    @Test
    void applicationMustNotDependOnLegacySkosExports() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(applicationPackagePatterns())
                .and().doNotBelongToAnyOf(CandidatExportService.class)
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.exports..");
        rule.check(applicationClasses);
    }
}
