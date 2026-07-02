package com.tyb.myblog.v2;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private static final String[] TOKEN_PORT_FORBIDDEN_PACKAGES = Stream.concat(
                    Stream.of("org.springframework.security.."),
                    Arrays.stream(BUSINESS_MODULE_PACKAGES))
            .toArray(String[]::new);
    private static final Set<String> WEB_DOMAIN_ENUM_WHITELIST = Set.of(
            "com.tyb.myblog.v2.identity.domain.account.AccountType",
            "com.tyb.myblog.v2.content.domain.article.ArticleStatus",
            "com.tyb.myblog.v2.content.domain.article.HomepageSlot",
            "com.tyb.myblog.v2.comment.domain.CommentAuditStatus",
            "com.tyb.myblog.v2.comment.domain.CommentTargetType",
            "com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus");

    // 业务模块可以依赖 common 的抽象能力，但不能直接依赖 common.security 的具体安全实现。
    @ArchTest
    static final ArchRule business_modules_do_not_depend_on_common_security_implementation =
            noClasses()
                    .that().resideInAnyPackage(BUSINESS_MODULE_PACKAGES)
                    .should().dependOnClassesThat().resideInAPackage("..common.security..")
                    .allowEmptyShould(true);

    // token 端口属于稳定 common API，不能泄漏 Spring Security 或业务模块实现。
    @ArchTest
    static final ArchRule common_token_ports_remain_framework_and_business_independent =
            noClasses()
                    .that().resideInAPackage("..common.auth.token..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(TOKEN_PORT_FORBIDDEN_PACKAGES);

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

    // web 层只处理 HTTP 入参出参，不能直接依赖 Entity 或其他基础设施实现。
    @ArchTest
    static final ArchRule web_does_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..web..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .allowEmptyShould(true);

    // Web 只允许复用经过裁决的稳定领域枚举，其他领域对象必须由 application contract 隔离。
    @ArchTest
    static final ArchRule web_only_depends_on_whitelisted_domain_enums =
            noClasses()
                    .that().resideInAPackage("..web..")
                    .should().dependOnClassesThat(domainTypesOutsideWebWhitelist())
                    .allowEmptyShould(true);

    // application 层只编排用例，不能反向依赖 Web DTO、Mapper 或 Entity。
    @ArchTest
    static final ArchRule application_does_not_depend_on_web_or_infrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..web..", "..infrastructure..")
                    .allowEmptyShould(true);

    // application 层只接收与传递应用命令，不能直接绑定 Servlet 请求对象。
    @ArchTest
    static final ArchRule application_does_not_depend_on_servlet_api =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.servlet..")
                    .allowEmptyShould(true);

    // domain 必须保持框架无关，禁止依赖 Web、MyBatis 和 Servlet API。
    @ArchTest
    static final ArchRule domain_does_not_depend_on_framework_apis =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.mybatis..",
                            "com.baomidou.mybatisplus..",
                            "jakarta.servlet..")
                    .allowEmptyShould(true);

    // identity domain 的限流端口不能泄漏缓存实现或 HTTP 错误类型。
    @ArchTest
    static final ArchRule identity_domain_does_not_depend_on_rate_limit_implementation =
            noClasses()
                    .that().resideInAPackage("..identity.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.github.benmanes.caffeine..",
                            "..common.error..")
                    .allowEmptyShould(true);

    // infrastructure 可以适配 application/domain，但不能反向依赖 HTTP 接入层。
    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_web =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..web..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule business_modules_are_free_of_cycles =
            moduleCyclesAreForbidden("com.tyb.myblog.v2.(*)..");

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

    private static ArchRule moduleCyclesAreForbidden(String packagePattern) {
        return slices()
                .matching(packagePattern)
                .should().beFreeOfCycles();
    }

    private static DescribedPredicate<JavaClass> domainTypesOutsideWebWhitelist() {
        return new DescribedPredicate<>("domain types outside the Web whitelist") {
            @Override
            public boolean test(JavaClass input) {
                return input.getPackageName().contains(".domain")
                        && !WEB_DOMAIN_ENUM_WHITELIST.contains(input.getName());
            }
        };
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

    @Test
    void applicationRuleRejectsDeliberateLayerViolationFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture");

        assertThatThrownBy(() -> application_does_not_depend_on_web_or_infrastructure.check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("InvalidApplicationService")
                .hasMessageContaining("InvalidMapper")
                .hasMessageContaining("InvalidEntity")
                .hasMessageContaining("InvalidWebDto");
    }

    @Test
    void webRuleRejectsDeliberateInfrastructureViolationFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture");

        assertThatThrownBy(() -> web_does_not_depend_on_infrastructure.check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("InvalidWebController")
                .hasMessageContaining("InvalidEntity");
    }

    @Test
    void webRuleRejectsDeliberateDomainViolationFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture");

        assertThatThrownBy(() ->
                web_only_depends_on_whitelisted_domain_enums.check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("InvalidWebController")
                .hasMessageContaining("InvalidDomainDto");
    }

    @Test
    void domainRuleRejectsDeliberateFrameworkViolationFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture");

        assertThatThrownBy(() -> domain_does_not_depend_on_framework_apis.check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("InvalidDomainModel")
                .hasMessageContaining("HttpServletRequest")
                .hasMessageContaining("WebRequest")
                .hasMessageContaining("BaseMapper");
    }

    @Test
    void infrastructureRuleRejectsDeliberateWebViolationFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture");

        assertThatThrownBy(() -> infrastructure_does_not_depend_on_web.check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("InvalidInfrastructureAdapter")
                .hasMessageContaining("InvalidWebDto");
    }

    @Test
    void moduleCycleRuleRejectsDeliberateCycleFixture() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture.cycle");

        assertThatThrownBy(() -> moduleCyclesAreForbidden(
                "com.tyb.myblog.v2.architecture.fixture.cycle.(*)..").check(fixtureClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("alpha")
                .hasMessageContaining("beta");
    }

    @Test
    void identityApplicationMayDependOnCommonTokenIssuerPort() {
        var fixtureClasses = new ClassFileImporter()
                .importPackages("com.tyb.myblog.v2.architecture.fixture.identity");

        assertThatCode(() ->
                business_modules_do_not_depend_on_common_security_implementation.check(fixtureClasses))
                .doesNotThrowAnyException();
    }
}
