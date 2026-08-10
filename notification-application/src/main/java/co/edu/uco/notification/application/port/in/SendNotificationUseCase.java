package co.edu.uco.notification.application.port.in;

import co.edu.uco.notification.application.command.SendNotificationCommand;
import co.edu.uco.notification.application.dto.SendNotificationResult;
import reactor.core.publisher.Mono;

/**
 * Puerto de entrada del caso de uso principal: aceptar una solicitud de envío.
 *
 * <p>Quien lo invoca puede ser la API REST, un consumidor de cola o una prueba. El caso de uso no
 * cambia según quién lo llame.
 */
public interface SendNotificationUseCase {

    Mono<SendNotificationResult> send(SendNotificationCommand command);
}
