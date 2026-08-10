/**
 * Tipos transversales al componente: códigos de error e identificador de correlación.
 *
 * <p>Es el único módulo del que pueden depender todos los demás. No contiene reglas de negocio.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Kernel",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package co.edu.uco.notification.shared;
