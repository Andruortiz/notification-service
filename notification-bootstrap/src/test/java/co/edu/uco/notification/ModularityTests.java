package co.edu.uco.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifica la estructura modular y genera su documentación.
 *
 * <p>Conviene ser preciso sobre qué aporta cada mecanismo, para no vender más de lo que hay. La
 * separación entre capas ya la impone <strong>Maven</strong>: {@code notification-core} no declara
 * a {@code notification-adapters} entre sus dependencias, así que una violación no compila. Esa es
 * una garantía más fuerte que cualquier prueba.
 *
 * <p>Lo que Spring Modulith añade sobre eso es que las dependencias declaradas en cada
 * {@code package-info} se cumplan, y la generación de diagramas a partir del código, que por venir
 * del código no envejece como un dibujo hecho a mano.
 */
class ModularityTests {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(NotificationServiceApplication.class);

    @Test
    @DisplayName("La estructura de módulos respeta las dependencias declaradas")
    void verifiesModuleStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("Genera la documentación de módulos en docs/modulith")
    void writesDocumentation() {
        new Documenter(MODULES, "../docs/modulith")
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
