package co.edu.uco.notification.application.usecase;

import co.edu.uco.notification.application.command.SendNotificationCommand;
import co.edu.uco.notification.application.dto.SendNotificationResult;
import co.edu.uco.notification.application.port.in.SendNotificationUseCase;
import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.exception.ChannelNotAvailableException;
import co.edu.uco.notification.core.repository.NotificationRepository;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationContent;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.Recipient;
import co.edu.uco.notification.core.valueobject.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso principal: acepta una solicitud de envío.
 *
 * <p>El recorrido es: validar el canal contra el catálogo, resolver la idempotencia, construir el
 * agregado, persistirlo, encolarlo para su despacho y devolver el identificador de seguimiento. La
 * entrega ocurre después y de forma asíncrona, de modo que la respuesta al cliente no depende de la
 * latencia del proveedor.
 *
 * <p>Es una clase plana, sin anotaciones del framework: se registra como bean desde
 * {@code notification-bootstrap}. Así este módulo se prueba sin levantar un contexto de Spring.
 */
public class SendNotificationService implements SendNotificationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationService.class);

    private final NotificationRepository repository;
    private final ChannelCatalogPort channelCatalog;
    private final NotificationEventPublisherPort eventPublisher;
    private final Clock clock;

    public SendNotificationService(
            final NotificationRepository repository,
            final ChannelCatalogPort channelCatalog,
            final NotificationEventPublisherPort eventPublisher,
            final Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.channelCatalog = Objects.requireNonNull(channelCatalog, "channelCatalog");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Mono<SendNotificationResult> send(final SendNotificationCommand command) {
        return Mono.fromCallable(() -> ChannelType.of(command.channel()))
                .flatMap(channel -> channelCatalog.findActiveRoute(channel)
                        .switchIfEmpty(Mono.error(() -> new ChannelNotAvailableException(channel))))
                .flatMap(route -> resolveIdempotency(command, route.channel()));
    }

    /**
     * Si el cliente ya envió esta referencia, se devuelve la notificación existente en lugar de
     * crear una segunda. Es lo que permite a un sistema cliente reintentar su llamada sin miedo.
     */
    private Mono<SendNotificationResult> resolveIdempotency(
            final SendNotificationCommand command, final ChannelType channel) {

        final TenantId tenantId = TenantId.of(command.tenantId());
        final ExternalId externalId = ExternalId.of(command.externalId());

        return repository.findByTenantAndExternalId(tenantId, externalId)
                .map(existing -> {
                    LOG.info("Solicitud repetida para la referencia {} del cliente {}; se devuelve {}",
                            externalId, tenantId, existing.id());
                    return SendNotificationResult.duplicated(
                            existing.id().value(), existing.status().name(), existing.createdAt());
                })
                .switchIfEmpty(Mono.defer(() -> acceptNew(command, channel, tenantId, externalId)));
    }

    private Mono<SendNotificationResult> acceptNew(
            final SendNotificationCommand command,
            final ChannelType channel,
            final TenantId tenantId,
            final ExternalId externalId) {

        final Notification notification = Notification.accept(
                tenantId,
                externalId,
                channel,
                Recipient.of(command.recipient()),
                NotificationContent.of(command.content()),
                Priority.fromNullable(command.priority()),
                clock.instant());

        return repository.save(notification)
                .flatMap(saved -> {
                    saved.markQueued(clock.instant());
                    return eventPublisher.enqueueForDispatch(saved)
                            .then(eventPublisher.publish(saved.pullEvents()))
                            .thenReturn(saved);
                })
                .doOnSuccess(saved -> LOG.info(
                        "Notificación aceptada id={} cliente={} canal={} prioridad={}",
                        saved.id(), saved.tenantId(), saved.channel(), saved.priority()))
                .map(saved -> SendNotificationResult.accepted(
                        saved.id().value(), saved.status().name(), saved.createdAt()));
    }
}
