/**
 * Ensamblado de la aplicación: es el único módulo que conoce a todos los demás.
 *
 * <p>Aquí se declaran los beans de los casos de uso, el tratamiento global de errores, el registro
 * de arranque y el punto donde se enchufará el componente de Seguridad. Es deliberadamente delgado:
 * no contiene reglas de negocio, solo cableado.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Bootstrap",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"adapters", "application", "core", "shared"})
package co.edu.uco.notification.bootstrap;
