package co.edu.uco.notification;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hace cumplir la arquitectura hexagonal.
 *
 * <p>Un principio que no se verifica se erosiona en pocas semanas: basta un {@code import} cómodo
 * bajo presión de entrega. Estas reglas fallan la construcción, así que la erosión no llega a
 * integrarse.
 */
class HexagonalArchitectureTest {

    private static final String BASE = "co.edu.uco.notification";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("El dominio no depende del framework ni de la infraestructura")
    void domainStaysFree() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.boot..",
                        "org.springframework.context..",
                        "org.springframework.data..",
                        "org.springframework.web..",
                        "org.springframework.amqp..",
                        "org.springframework.http..",
                        "com.mongodb..",
                        "com.rabbitmq..",
                        "jakarta.servlet..")
                .because("el nucleo de dominio debe poder compilarse y probarse sin infraestructura. "
                        + "Se admite org.springframework.modulith porque son solo anotaciones de "
                        + "metadatos de arquitectura en los package-info, sin comportamiento");

        rule.check(classes);
    }

    @Test
    @DisplayName("La capa de aplicación no conoce la infraestructura")
    void applicationStaysFree() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".adapters..",
                        BASE + ".bootstrap..",
                        "org.springframework.boot..",
                        "org.springframework.context..",
                        "org.springframework.data..",
                        "org.springframework.web..",
                        "org.springframework.amqp..",
                        "com.mongodb..",
                        "com.rabbitmq..")
                .because("los casos de uso se declaran como beans desde bootstrap, de modo que este "
                        + "modulo no necesita el contenedor y se prueba sin levantarlo");

        rule.check(classes);
    }

    @Test
    @DisplayName("El dominio no depende de las capas que lo rodean")
    void domainDoesNotDependOnOuterLayers() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".application..",
                        BASE + ".adapters..",
                        BASE + ".bootstrap..")
                .because("la dependencia apunta siempre hacia el dominio, nunca al contrario");

        rule.check(classes);
    }

    @Test
    @DisplayName("Los adaptadores de entrada no invocan directamente a los de salida")
    void inboundDoesNotReachOutbound() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".adapters.in.rest..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".adapters.out.mongo..",
                        BASE + ".adapters.out.provider..")
                .because("un adaptador de entrada debe pasar por un caso de uso; si alcanza la "
                        + "persistencia o el proveedor, la logica se escapa del nucleo");

        rule.check(classes);
    }

    @Test
    @DisplayName("No hay ciclos entre los paquetes del componente")
    void noPackageCycles() {
        slices()
                .matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }
}
