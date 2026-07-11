package com.ar.laboratory.asyncjobengine.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de arquitectura hexagonal con ArchUnit.
 *
 * <p>Verifica en tiempo de compilación (ejecución de tests) que las capas de la arquitectura
 * hexagonal se respetan:
 *
 * <ul>
 *   <li><b>domain</b> — modelos y excepciones de negocio. No depende de ninguna capa externa.
 *   <li><b>application</b> — casos de uso y puertos. Solo puede depender de {@code domain}.
 *   <li><b>infrastructure</b> — adaptadores, controllers, config. Puede depender de todas las capas
 *       anteriores.
 * </ul>
 */
@DisplayName("Arquitectura Hexagonal — ArchUnit")
class HexagonalArchitectureTest {

    private static final String BASE_PACKAGE = "com.ar.laboratory.asyncjobengine";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(BASE_PACKAGE);
    }

    // =========================================================================
    // Reglas de capas
    // =========================================================================

    @Test
    @DisplayName("La capa domain no debe depender de application ni de infrastructure")
    void domainMustNotDependOnApplicationOrInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..")
                .because(
                        "El dominio es el núcleo; no puede conocer casos de uso ni"
                                + " adaptadores")
                .check(importedClasses);
    }

    @Test
    @DisplayName("La capa application no debe depender de infrastructure")
    void applicationMustNotDependOnInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because(
                        "Los casos de uso solo pueden hablar con el dominio; "
                                + "la infraestructura se inyecta vía puertos")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Arquitectura en capas: domain ← application ← infrastructure")
    void layeredArchitectureIsRespected() {
        layeredArchitecture()
                // Solo evaluamos dependencias cuyo destino vive en una de las capas definidas
                // (Domain/Application/Infrastructure). Con consideringAllDependencies() ArchUnit
                // contaba también las dependencias a clases fuera de la arquitectura
                // (java.lang.Object, RuntimeException, @RestController, Lombok, Swagger, etc.),
                // generando ~1271 falsos positivos. El layering hexagonal en sí se respeta.
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("..domain..")
                .layer("Application")
                .definedBy("..application..")
                .layer("Infrastructure")
                .definedBy("..infrastructure..")
                .whereLayer("Domain")
                .mayNotAccessAnyLayer()
                .whereLayer("Application")
                .mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure")
                .mayOnlyAccessLayers("Application", "Domain")
                .check(importedClasses);
    }

    // =========================================================================
    // Reglas de naming y estereotipos
    // =========================================================================

    @Test
    @DisplayName(
            "Los controllers deben terminar en 'Controller' y estar en el paquete web.controller")
    void controllerNamingConvention() {
        classes()
                .that()
                .resideInAPackage("..web.controller..")
                .should()
                .haveSimpleNameEndingWith("Controller")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Los use cases deben terminar en 'UseCase'")
    void useCaseNamingConvention() {
        classes()
                .that()
                .resideInAPackage("..application.usecase..")
                .should()
                .haveSimpleNameEndingWith("UseCase")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Los adaptadores de persistencia deben terminar en 'Adapter'")
    void persistenceAdapterNamingConvention() {
        classes()
                .that()
                .resideInAPackage("..persistence.adapter..")
                .should()
                .haveSimpleNameEndingWith("Adapter")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Los puertos de salida deben terminar en 'Port'")
    void outboundPortNamingConvention() {
        classes()
                .that()
                .resideInAPackage("..outbound.port..")
                .should()
                .haveSimpleNameEndingWith("Port")
                .check(importedClasses);
    }

    // =========================================================================
    // Reglas de buenas prácticas
    // =========================================================================

    @Test
    @DisplayName("No se debe usar inyección por campo (preferir constructor)")
    void noFieldInjection() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(importedClasses);
    }
}
