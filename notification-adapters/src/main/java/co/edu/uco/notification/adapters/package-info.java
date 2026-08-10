/**
 * Adaptadores que conectan el componente con el mundo exterior.
 *
 * <p>{@code in} agrupa los que provocan trabajo —API REST, consumidor de cola, tareas
 * programadas— y {@code out} los que el componente invoca —persistencia, mensajería y
 * proveedores de notificación—. Toda la tecnología vive aquí: si mañana se cambia MongoDB o el
 * proveedor de correo, el cambio no cruza esta frontera.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Adapters",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"application", "core", "shared"})
package co.edu.uco.notification.adapters;
