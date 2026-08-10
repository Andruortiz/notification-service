package co.edu.uco.notification.core.exception;

import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.shared.error.ErrorCode;

/** El canal solicitado no existe en el catálogo o está deshabilitado. */
public class ChannelNotAvailableException extends DomainException {

    public ChannelNotAvailableException(final ChannelType channel) {
        super(ErrorCode.CHANNEL_NOT_AVAILABLE,
                "No hay ningún proveedor habilitado para el canal: " + channel);
    }
}
