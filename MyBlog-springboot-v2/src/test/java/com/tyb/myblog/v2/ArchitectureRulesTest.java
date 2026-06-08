package com.tyb.myblog.v2;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@AnalyzeClasses(packages = "com.tyb.myblog.v2", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String[] BUSINESS_MODULES = {
            "identity",
            "content",
            "comment",
            "system",
            "stats"
    };
    private static final String[] BUSINESS_MODULE_PACKAGES = Arrays.stream(BUSINESS_MODULES)
            .map(module -> ".." + module + "..")
            .toArray(String[]::new);

    // 业务模块可以依赖 common 的抽象能力，但不能直接依赖 common.security 的具体安全实现。
    @ArchTest
    static final ArchRule business_modules_do_not_depend_on_common_security_implementation =
            noClasses()
                    .that().resideInAnyPackage(BUSINESS_MODULE_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage("..common.security..")
                    .allowEmptyShould(true);

    // common 必须保持全局通用，不能反向依赖任何业务模块。
    @ArchTest
    static final ArchRule common_does_not_depend_on_business_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAnyPackage(BUSINESS_MODULE_PACKAGES);

    // domain 只表达业务语义和规则，不能依赖接入层、编排层或技术实现层。
    @ArchTest
    static final ArchRule domain_does_not_depend_on_upper_or_infrastructure_layers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..web..", "..application..", "..infrastructure..")
                    .allowEmptyShould(true);

    // domain 不能直接依赖 Spring Security，认证细节应通过应用层或基础设施适配。
    @ArchTest
    static final ArchRule domain_does_not_depend_on_spring_security =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.security..")
                    .allowEmptyShould(true);

    // domain 获取当前时间必须使用注入的 Clock，禁止直接读取系统时间。
    @ArchTest
    static final ArchRule domain_does_not_call_local_date_time_now =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().callMethod(LocalDateTime.class, "now")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_does_not_create_legacy_date =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().callConstructor(Date.class)
                    .allowEmptyShould(true);

    // web 层只处理 HTTP 入参出参，不能绕过应用层直接访问持久化 Mapper。
    @ArchTest
    static final ArchRule web_does_not_depend_on_persistence_mappers =
            noClasses()
                    .that().resideInAPackage("..web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.mapper..")
                    .allowEmptyShould(true);

    // application 层可以依赖 domain，但不能直接依赖 MyBatis-Plus Mapper。
    @ArchTest
    static final ArchRule application_does_not_depend_on_persistence_mappers =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence.mapper..")
                    .allowEmptyShould(true);

    // 跨模块只允许依赖对方 application 暴露的接口，禁止访问 domain、web 或 infrastructure。
    @ArchTest
    static final ArchRule identity_only_depends_on_other_module_applications =
            moduleOnlyDependsOnOtherModuleApplications("identity");

    @ArchTest
    static final ArchRule content_only_depends_on_other_module_applications =
            moduleOnlyDependsOnOtherModuleApplications("content");

    @ArchTest
    static final ArchRule comment_only_depends_on_other_module_applications =
            moduleOnlyDependsOnOtherModuleApplications("comment");

    @ArchTest
    static final ArchRule system_only_depends_on_other_module_applications =
            moduleOnlyDependsOnOtherModuleApplications("system");

    @ArchTest
    static final ArchRule stats_only_depends_on_other_module_applications =
            moduleOnlyDependsOnOtherModuleApplications("stats");

    // common.security 不能绑定 identity 的持久化实现，否则认证基础设施会被具体用户表结构拖住。
    @ArchTest
    static final ArchRule common_security_does_not_depend_on_identity_infrastructure =
            noClasses()
                    .that().resideInAPackage("..common.security..")
                    .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..");

    // common-infra 已收口到 common 包，不再允许旧的顶层 infrastructure 技术模块。
    @ArchTest
    static final ArchRule no_legacy_top_level_infrastructure_package =
            noClasses()
                    .should().resideInAPackage("com.tyb.myblog.v2.infrastructure..");

    private static ArchRule moduleOnlyDependsOnOtherModuleApplications(String module) {
        String[] forbiddenPackages = Arrays.stream(BUSINESS_MODULES)
                .filter(otherModule -> !otherModule.equals(module))
                .flatMap(otherModule -> Arrays.stream(new String[]{
                        ".." + otherModule + ".domain..",
                        ".." + otherModule + ".web..",
                        ".." + otherModule + ".infrastructure.."
                }))
                .toArray(String[]::new);

        return noClasses()
                .that().resideInAPackage(".." + module + "..")
                .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages)
                .allowEmptyShould(true);
    }

    @Test
    void declaresCurrentArchitectureBoundaryPackages() {
        assertThatCode(() -> Class.forName("com.tyb.myblog.v2.package-info"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.tyb.myblog.v2.infrastructure.persistence.package-info"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void guardsAllFiveBusinessModules() {
        assertThat(BUSINESS_MODULES)
                .containsExactly("identity", "content", "comment", "system", "stats");
    }
}
