package co.edu.uco.notification.adapters.in.rabbit;

import co.edu.uco.notification.adapters.config.RabbitConfiguration;
import co.edu.uco.notification.adapters.out.rabbit.NotificationDispatchMessage;
import co.edu.uco.notification.application.port.in.DispatchNotificationUseCase;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Consume la cola de despacho y ejecuta el envío.
 *
 * <p><strong>Confirmación manual.</strong> El mensaje se confirma solo cuando el despacho ha
 * terminado. Con confirmación automática, una caída de la réplica entre la entrega del mensaje y el
 * final del proceso perdería la notificación sin dejar rastro.
 *
 * <p><strong>Sobre el bloqueo.</strong> El contenedor de Spring AMQP entrega el mensaje en un hilo
 * suyo, y aquí se espera a que la cadena reactiva termine para poder confirmar o rechazar con
 * conocimiento del resultado. El paralelismo lo gobierna la concurrencia del contenedor. Lo que
 * permanece no bloqueante es el camino que atiende la petición HTTP, la persistencia y la llamada
 * al proveedor.
 */
@Component
public class NotificationDispatchListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatchListener.class);

    private static final Duration DISPATCH_TIMEOUT = Duration.ofSeconds(30);

    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public NotificationDispatchListener(final DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    @RabbitListener(
            queues = RabbitConfiguration.DISPATCH_QUEUE,
            concurrency = "${notification.rabbit.concurrency:4}")
    public void onDispatchRequested(
            final NotificationDispatchMessage message,
            final Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) final long deliveryTag) {

        try {
            dispatchNotificationUseCase.dispatch(message.notificationId()).block(DISPATCH_TIMEOUT);
            channel.basicAck(deliveryTag, false);
        } catch (final Exception dispatchFailure) {
            LOG.error("Fallo al despachar {}; el mensaje se envía a la cola de fallidos",
                    message.notificationId(), dispatchFailure);
            rejectWithoutRequeue(channel, deliveryTag, message);
        }
    }

    /**
     * Se rechaza sin devolver a la cola: el estado de la notificación ya quedó registrado en la
     * base de datos, y reencolar aquí produciría un ciclo cerrado. El reintento lo gobierna la
     * política de reintentos, no el broker.
     */
    private void rejectWithoutRequeue(
            final Channel channel, final long deliveryTag, final NotificationDispatchMessage message) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (final IOException nackFailure) {
            LOG.error("No se pudo rechazar el mensaje de {}", message.notificationId(), nackFailure);
        }
    }
}
