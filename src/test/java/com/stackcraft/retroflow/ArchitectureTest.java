package com.stackcraft.retroflow;

import com.stackcraft.retroflow.controller.GlobalExceptionHandler;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

/**
 * The structure decided in Block 2 (house rules, D12, P8, P9), enforced as tests so a later change
 * can't quietly undo it. Production code only: tests may use field injection and touch anything.
 */
@AnalyzeClasses(packages = "com.stackcraft.retroflow", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String WEB = "com.stackcraft.retroflow.controller..";

    /** Business rules live in the service layer; a controller that talks to a repository bypasses them. */
    @ArchTest
    static final ArchRule controllersMustNotAccessRepositories = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage("com.stackcraft.retroflow.repository..")
            .because("business rules are enforced in the service layer (house rules: thin controllers)");

    /** Dependencies point inwards: web → service → repository/entity, never back to the web layer. */
    @ArchTest
    static final ArchRule innerLayersMustNotDependOnTheWebLayer = noClasses()
            .that().resideInAnyPackage("com.stackcraft.retroflow.service..", "com.stackcraft.retroflow.repository..",
                    "com.stackcraft.retroflow.entity..", "com.stackcraft.retroflow.exception..")
            .should().dependOnClassesThat().resideInAPackage(WEB)
            .because("the service layer must be usable without HTTP (and tested without it)");

    /** The API contract is the DTOs: an entity in a signature leaks the database model and lazy proxies. */
    @ArchTest
    static final ArchRule controllerEndpointsMustNotAcceptOrReturnEntities = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should(notAcceptOrReturnEntities())
            .because("requests and responses are DTOs (P9); entities stay behind the service layer");

    /** The DTO records are the JSON contract; mapping from entities belongs in ResponseMapper only. */
    @ArchTest
    static final ArchRule dtosMustNotDependOnEntityClasses = noClasses()
            .that().resideInAPackage("com.stackcraft.retroflow.controller.dto..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .because("entity→DTO mapping lives in controller.mapper.ResponseMapper (P9)");

    /** One error format needs exactly one place that writes errors. */
    @ArchTest
    static final ArchRule onlyGlobalExceptionHandlerIsAControllerAdvice = classes()
            .that().areMetaAnnotatedWith(ControllerAdvice.class)
            .should().haveFullyQualifiedName(GlobalExceptionHandler.class.getName())
            .because("all errors use one format from one @RestControllerAdvice (P8)");

    /** Constructor injection only: dependencies are explicit, final, and the class is testable without Spring. */
    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
            .because("the house rules require constructor injection");

    /** Checks the return type and every parameter type, including generics such as {@code List<Team>}. */
    private static ArchCondition<JavaMethod> notAcceptOrReturnEntities() {
        return new ArchCondition<>("not accept or return @Entity classes") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Stream.concat(Stream.of(method.getReturnType()), method.getParameterTypes().stream())
                        .flatMap(type -> type.getAllInvolvedRawTypes().stream())
                        .filter(type -> type.isAnnotatedWith(Entity.class))
                        .map(JavaClass::getName)
                        .distinct()
                        .forEach(entity -> events.add(SimpleConditionEvent.violated(method,
                                "%s uses entity %s in its signature".formatted(method.getFullName(), entity))));
            }
        };
    }

}

