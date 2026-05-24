package com.aurora.myblog.v2;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatCode;

@AnalyzeClasses(packages = "com.aurora.myblog.v2", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule modules_do_not_depend_on_common_security_implementation =
            noClasses()
                    .that().resideInAPackage("..modules..")
                    .should().dependOnClassesThat().resideInAPackage("..common.security..");

    @ArchTest
    static final ArchRule common_does_not_depend_on_business_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..");

    @ArchTest
    static final ArchRule identity_domain_does_not_depend_on_spring_security =
            noClasses()
                    .that().resideInAPackage("..modules.identity.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.security..");

    @ArchTest
    static final ArchRule common_security_does_not_depend_on_identity_infrastructure =
            noClasses()
                    .that().resideInAPackage("..common.security..")
                    .should().dependOnClassesThat().resideInAPackage("..modules.identity.infrastructure..");

    @Test
    void declaresInitialArchitectureBoundaryPackages() {
        assertThatCode(() -> Class.forName("com.aurora.myblog.v2.modules.package-info"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.aurora.myblog.v2.infrastructure.persistence.package-info"))
                .doesNotThrowAnyException();
    }
}
