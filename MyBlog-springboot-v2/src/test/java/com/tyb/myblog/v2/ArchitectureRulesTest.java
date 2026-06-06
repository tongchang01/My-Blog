package com.tyb.myblog.v2;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatCode;

@AnalyzeClasses(packages = "com.tyb.myblog.v2", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    // 业务模块可以依赖 common 的抽象能力，但不能直接依赖 common.security 的具体安全实现。
    @ArchTest
    static final ArchRule business_modules_do_not_depend_on_common_security_implementation =
            noClasses()
                    .that().resideInAnyPackage("..identity..", "..content..", "..comment..")
                    .should().dependOnClassesThat().resideInAPackage("..common.security..")
                    .allowEmptyShould(true);

    // common 必须保持全局通用，不能反向依赖任何业务模块。
    @ArchTest
    static final ArchRule common_does_not_depend_on_business_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAnyPackage("..identity..", "..content..", "..comment..");

    // domain 只表达业务语义和规则，不能依赖接入层、编排层或技术实现层。
    @ArchTest
    static final ArchRule domain_does_not_depend_on_upper_or_infrastructure_layers =
            noClasses()
                    .that().resideInAnyPackage("..identity.domain..", "..content.domain..", "..comment.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..identity.web..", "..identity.application..", "..identity.infrastructure..",
                            "..content.web..", "..content.application..", "..content.infrastructure..",
                            "..comment.web..", "..comment.application..", "..comment.infrastructure..")
                    .allowEmptyShould(true);

    // domain 不能直接依赖 Spring Security，认证细节应通过应用层或基础设施适配。
    @ArchTest
    static final ArchRule domain_does_not_depend_on_spring_security =
            noClasses()
                    .that().resideInAnyPackage("..identity.domain..", "..content.domain..", "..comment.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.security..")
                    .allowEmptyShould(true);

    // web 层只处理 HTTP 入参出参，不能绕过应用层直接访问持久化 Mapper。
    @ArchTest
    static final ArchRule web_does_not_depend_on_persistence_mappers =
            noClasses()
                    .that().resideInAnyPackage("..identity.web..", "..content.web..", "..comment.web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.mapper..")
                    .allowEmptyShould(true);

    // application 层可以依赖 domain，但不能直接依赖 MyBatis-Plus Mapper。
    @ArchTest
    static final ArchRule application_does_not_depend_on_persistence_mappers =
            noClasses()
                    .that().resideInAnyPackage("..identity.application..", "..content.application..", "..comment.application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.mapper..")
                    .allowEmptyShould(true);

    // 业务模块之间不能直接访问对方的 infrastructure，跨模块协作必须通过更稳定的用例或领域抽象。
    @ArchTest
    static final ArchRule identity_does_not_depend_on_other_module_infrastructure =
            noClasses()
                    .that().resideInAPackage("..identity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..content.infrastructure..", "..comment.infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule content_does_not_depend_on_other_module_infrastructure =
            noClasses()
                    .that().resideInAPackage("..content..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..identity.infrastructure..", "..comment.infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule comment_does_not_depend_on_other_module_infrastructure =
            noClasses()
                    .that().resideInAPackage("..comment..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..identity.infrastructure..", "..content.infrastructure..")
                    .allowEmptyShould(true);

    // common.security 不能绑定 identity 的持久化实现，否则认证基础设施会被具体用户表结构拖住。
    @ArchTest
    static final ArchRule common_security_does_not_depend_on_identity_infrastructure =
            noClasses()
                    .that().resideInAPackage("..common.security..")
                    .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..");

    @Test
    void declaresInitialArchitectureBoundaryPackages() {
        assertThatCode(() -> Class.forName("com.tyb.myblog.v2.package-info"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.tyb.myblog.v2.infrastructure.persistence.package-info"))
                .doesNotThrowAnyException();
    }
}
