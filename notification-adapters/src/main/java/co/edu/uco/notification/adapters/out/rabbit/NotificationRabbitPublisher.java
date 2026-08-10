package co.edu.uco.notification.adapters.out.rabbit;

import co.edu.uco.notification.adapters.config.RabbitConfiguration;
import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Publica en RabbitMQ las órdenes de despacho y los eventos de dominio.
 *
 * <p>El cliente de Spring AMQP es bloqueante, así que la publicación se aparta al planificador
 * elástico para no retener un hilo del bucle de eventos. Lo que se mantiene no bloqueante es el
 * camino que atiende la petición HTTP.
 */
@Component
public class NotificationRabbitPublisher implements NotificationEventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationRabbitPublisher(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Mono<Void> enqueueForDispatch(final Notification notification) {
        final NotificationDispatchMessage message = new NotificationDispatchMessage(
                notification.id().value(),
                notification.channel().value(),
                notification.priority().weight());

        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(
                        RabbitConfiguration.EXCHANGE,
                        RabbitConfiguration.DISPATCH_ROUTING_KEY,
                        message,
                        amqpMessage -> {
                            amqpMessage.getMessageProperties()
                                    .setPriority(notification.priority().weight());
                            return amqpMessage;
                        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(ignored -> LOG.debug("Encolada {} con prioridad {}",
                        notification.id(), notification.priority()))
                .then();
    }

    @Override
    public Mono<Void> publish(final List<DomainEvent> events) {
        if (events.isEmpty()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> events.forEach(this::publishSingle))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void publishSingle(final DomainEvent event) {
        final String routingKey = "notification.event." + event.getClass().getSimpleName();
        rabbitTemplate.convertAndSend(RabbitConfiguration.EXCHANGE, routingKey, event);
        LOG.debug("Evento publicado {} para {}", event.getClass().getSimpleName(), event.notificationId());
    }
}
