/**
 * Núcleo de dominio del componente de notificaciones.
 *
 * <p>Contiene el agregado, sus entidades, los objetos de valor, los eventos de dominio y las
 * políticas de negocio. No conoce la API REST, la base de datos ni la mensajería: la única
 * dependencia declarada hacia el exterior es el kernel compartido.
 *
 * <p>El módulo se declara abierto porque su API la componen los subpaquetes
 * {@code aggregate}, {@code valueobject}, {@code event} y {@code repository}, que la capa de
 * aplicación necesita referenciar de forma directa.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Domain Core",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"shared"})
package co.edu.uco.notification.core;
