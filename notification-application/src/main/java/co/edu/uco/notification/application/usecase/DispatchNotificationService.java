package co.edu.uco.notification.application.usecase;

import co.edu.uco.notification.application.port.in.DispatchNotificationUseCase;
import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.exception.ChannelNotAvailableException;
import co.edu.uco.notification.core.exception.NotificationNotFoundException;
import co.edu.uco.notification.core.repository.NotificationRepository;
import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.shared.logging.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Objects;

/**
 * Ejecuta el envío de una notificación ya aceptada.
 *
 * <p>Resuelve la ruta en el catálogo, delega en el proveedor y traduce el resultado a una
 * transición del agregado. La clasificación entre fallo recuperable y definitivo decide si la
 * notificación queda a la espera de otro intento o se aísla: por eso el adaptador la devuelve
 * explícita y este servicio no la deduce del texto del error.
 */
public class DispatchNotificationService implements DispatchNotificationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchNotificationService.class);

    private final NotificationRepository repository;
    private final ChannelCatalogPort channelCatalog;
    private final NotificationSenderPort sender;
    private final NotificationEventPublisherPort eventPublisher;
    private final Clock clock;

    public DispatchNotificationService(
            final NotificationRepository repository,
            final ChannelCatalogPort channelCatalog,
            final NotificationSenderPort sender,
            final NotificationEventPublisherPort eventPublisher,
            final Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.channelCatalog = Objects.requireNonNull(channelCatalog, "channelCatalog");
        this.sender = Objects.requireNonNull(sender, "sender");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Mono<Void> dispatch(final String notificationId) {
        final NotificationId id = NotificationId.of(notificationId);

        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotificationNotFoundException(id)))
                .filter(notification -> !notification.status().isFinal())
                .doOnDiscard(Notification.class, notification -> LOG.debug(
                        "Se ignora el despacho de {}: ya está en estado final {}",
                        LogSanitizer.sanitize(notification.id().value()), LogSanitizer.sanitize(notification.status().name())))
                .flatMap(this::execute)
                .then();
    }

    private Mono<Notification> execute(final Notification notification) {
        return channelCatalog.findActiveRoute(notification.channel())
                .switchIfEmpty(Mono.error(() -> new ChannelNotAvailableException(notification.channel())))
                .flatMap(route -> markInProcess(notification)
                        .flatMap(inProcess -> sender.send(inProcess, route)
                                .map(outcome -> applyOutcome(inProcess, outcome, route.retryPolicy()))
                                .onErrorResume(error -> Mono.just(
                                        applyUnexpectedError(inProcess, route, error)))))
                .flatMap(this::persistAndPublish);
    }

    private Mono<Notification> markInProcess(final Notification notification) {
        notification.markInProcess(clock.instant());
        return repository.save(notification);
    }

    private Notification applyOutcome(
            final Notification notification,
            final NotificationSenderPort.SendOutcome outcome,
            final RetryPolicy retryPolicy) {

        if (outcome.accepted()) {
            notification.markDelivered(outcome.providerId(), outcome.providerMessageId(), clock.instant());
            return notification;
        }

        final int attemptsMade = notification.attempts().size() + 1;
        if (outcome.recoverable() && retryPolicy.shouldRetry(attemptsMade)) {
            LOG.warn("Fallo recuperable en {} con {}: {}. Intento {} de {}",
                    attemptsMade,
                    retryPolicy.maxAttempts());
            notification.markRecoverable(outcome.providerId(), outcome.detail(), clock.instant());
            return notification;
        }

        final String reason = outcome.recoverable()
                ? "Reintentos agotados: " + outcome.detail()
                : outcome.detail();
        LOG.error("Fallo definitivo en {} con {}: {}",   LogSanitizer.sanitize(notification.id().value()),
                LogSanitizer.sanitize(outcome.providerId()),
                LogSanitizer.sanitize(reason));
        notification.markFailed(outcome.providerId(), LogSanitizer.sanitize(reason), clock.instant());
        return notification;
    }

    /**
     * Un error que el adaptador no logró clasificar se trata como recuperable: es preferible
     * reintentar de más que descartar en silencio una notificación que sí podía entregarse.
     */
    private Notification applyUnexpectedError(
            final Notification notification,
            final ChannelCatalogPort.ChannelRoute route,
            final Throwable error) {

        final String errorMessage = LogSanitizer.sanitize(error.getMessage());

        LOG.error("Error no clasificado al despachar {}",    LogSanitizer.sanitize(notification.id().value()),
                errorMessage);
        final int attemptsMade = notification.attempts().size() + 1;


        if (route.retryPolicy().shouldRetry(attemptsMade)) {
            notification.markRecoverable(
                    route.providerId(),
                    errorMessage,
                    clock.instant());
        } else {
            notification.markFailed(
                    route.providerId(),
                    errorMessage,
                    clock.instant());
        }
        return notification;
    }

    private Mono<Notification> persistAndPublish(final Notification notification) {
        return repository.save(notification)
                .flatMap(saved -> eventPublisher.publish(saved.pullEvents()).thenReturn(saved));
    }


}
