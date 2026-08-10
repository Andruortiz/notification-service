package co.edu.uco.notification.shared.error;

/**
 * Catálogo cerrado de códigos de error del componente.
 *
 * <p>El código viaja hacia el exterior en la respuesta de error, de modo que un sistema cliente
 * pueda reaccionar de forma programática sin analizar el texto del mensaje.
 */
public enum ErrorCode {

    /** El canal indicado no existe en el catálogo o está deshabilitado. */
    CHANNEL_NOT_AVAILABLE("NOT-0001"),

    /** La solicitud no cumple el esquema declarado para su canal. */
    INVALID_NOTIFICATION_DATA("NOT-0002"),

    /** Se intentó una transición de estado que la máquina de estados no permite. */
    INVALID_STATUS_TRANSITION("NOT-0003"),

    /** No se encontró la notificación solicitada. */
    NOTIFICATION_NOT_FOUND("NOT-0004"),

    /** Ningún proveedor habilitado puede atender el canal solicitado. */
    PROVIDER_NOT_AVAILABLE("NOT-0005"),

    /** El proveedor rechazó el envío de forma definitiva. */
    PROVIDER_REJECTED("NOT-0006"),

    /** Fallo no clasificado. */
    UNEXPECTED_ERROR("NOT-9999");

    private final String code;

    ErrorCode(final String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
