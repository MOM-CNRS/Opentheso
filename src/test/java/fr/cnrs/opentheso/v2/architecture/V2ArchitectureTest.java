package fr.cnrs.opentheso.v2.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.bean..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacyServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
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
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.imports..");
        rule.check(v2Classes);
    }

    @Test
    void v2_mustNotDependOnLegacySkosExports() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("fr.cnrs.opentheso.v2..")
                .should().dependOnClassesThat().resideInAPackage("fr.cnrs.opentheso.skos.exports..");
        rule.check(v2Classes);
    }
}
