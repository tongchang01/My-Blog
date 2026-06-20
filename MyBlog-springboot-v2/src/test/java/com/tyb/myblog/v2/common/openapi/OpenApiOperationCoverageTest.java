package com.tyb.myblog.v2.common.openapi;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiOperationCoverageTest {

    private static final String BASE_PACKAGE = "com.tyb.myblog.v2";

    @Test
    void everyHttpOperationHasNonBlankSummary() {
        List<String> missing = restControllers()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(this::isHttpOperation)
                .filter(method -> {
                    Operation operation = method.getAnnotation(Operation.class);
                    return operation == null || operation.summary().isBlank();
                })
                .map(method -> method.getDeclaringClass().getName()
                        + "#" + method.getName())
                .sorted()
                .toList();

        assertThat(missing)
                .as("以下公开 HTTP operation 缺少非空 @Operation.summary: %s",
                        String.join(", ", missing))
                .isEmpty();
    }

    private Stream<Class<?>> restControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                Controller.class));
        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .<Class<?>>map(definition -> ClassUtils.resolveClassName(
                        definition.getBeanClassName(),
                        ClassUtils.getDefaultClassLoader()))
                .filter(type -> !type.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toString()
                        .contains("test-classes"))
                .filter(type -> type.isAnnotationPresent(RestController.class));
    }

    private boolean isHttpOperation(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
