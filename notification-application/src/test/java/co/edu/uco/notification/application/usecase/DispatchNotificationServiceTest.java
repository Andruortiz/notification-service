package co.edu.uco.notification.application.usecase;

import co.edu.uco.notification.application.port.in.DispatchNotificationUseCase;
import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.application.support.InMemoryNotificationRepository;
import co.edu.uco.notification.application.support.RecordingEventPublisher;
import co.edu.uco.notification.application.support.StubChannelCatalog;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.event.NotificationDelivered;
import co.edu.uco.notification.core.event.NotificationFailed;
import co.edu.uco.notification.core.exception.NotificationNotFoundException;
import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationContent;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.Recipient;
import co.edu.uco.notification.core.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caso de uso Despachar Notificación")
class DispatchNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");
    private static final String CHANNEL = "TEST_CHANNEL";
    private static final String PROVIDER = "stub-provider";

    private InMemoryNotificationRepository repository;
    private RecordingEventPublisher eventPublisher;
    private StubChannelCatalog catalog;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
        eventPublisher = new RecordingEventPublisher();
        catalog = new StubChannelCatalog()
                .register(CHANNEL, PROVIDER, new RetryPolicy(3, Duration.ofSeconds(1), 2.0));
    }

    private DispatchNotificationUseCase useCaseWith(final NotificationSenderPort sender) {
        return new DispatchNotificationService(
                repository, catalog, sender, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Notification storedNotification() {
        final Notification notification = Notification.accept(
                TenantId.of("INQ-101"),
                ExternalId.of("sol-" + UUID.randomUUID()),
                ChannelType.of(CHANNEL),
                Recipient.of("destino@ejemplo.com"),
                NotificationContent.of(Map.of("asunto", "Hola")),
                Priority.MEDIUM,
                NOW);
        notification.pullEvents();
        repository.preload(notification);
        return notification;
    }

    @Test
    @DisplayName("un envío aceptado deja la notificación entregada")
    void deliversOnSuccess() {
        final Notification notification = storedNotification();
        final NotificationSenderPort sender = (target, route) ->
                Mono.just(NotificationSenderPort.SendOutcome.accepted(PROVIDER, "msg-1"));

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();

        final Notification stored = repository.findById(notification.id()).block();
        assertThat(stored.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(stored.attempts()).singleElement().matches(attempt -> attempt.succeeded());
        assertThat(eventPublisher.published()).hasAtLeastOneElementOfType(NotificationDelivered.class);
    }

    @Test
    @DisplayName("un fallo transitorio deja la notificación a la espera de otro intento")
    void marksRecoverableOnTransientFailure() {
        final Notification notification = storedNotification();
        final NotificationSenderPort sender = (target, route) ->
                Mono.just(NotificationSenderPort.SendOutcome.recoverableFailure(PROVIDER, "429"));

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();

        final Notification stored = repository.findById(notification.id()).block();
        assertThat(stored.status()).isEqualTo(NotificationStatus.RECOVERABLE);
        assertThat(stored.status().allowsRetry()).isTrue();
    }

    @Test
    @DisplayName("un fallo definitivo aísla la notificación aunque queden reintentos")
    void marksFailedOnPermanentFailure() {
        final Notification notification = storedNotification();
        final NotificationSenderPort sender = (target, route) ->
                Mono.just(NotificationSenderPort.SendOutcome.permanentFailure(
                        PROVIDER, "400_INVALID_DESTINATION"));

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();

        final Notification stored = repository.findById(notification.id()).block();
        assertThat(stored.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(eventPublisher.published()).hasAtLeastOneElementOfType(NotificationFailed.class);
    }

    @Test
    @DisplayName("agotados los reintentos, un fallo transitorio pasa a definitivo")
    void failsWhenRetriesAreExhausted() {
        catalog.register(CHANNEL, PROVIDER, new RetryPolicy(1, Duration.ofSeconds(1), 2.0));
        final Notification notification = storedNotification();
        final NotificationSenderPort sender = (target, route) ->
                Mono.just(NotificationSenderPort.SendOutcome.recoverableFailure(PROVIDER, "429"));

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();

        final Notification stored = repository.findById(notification.id()).block();
        assertThat(stored.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(stored.attempts()).singleElement()
                .satisfies(attempt -> assertThat(attempt.detail()).contains("Reintentos agotados"));
    }

    @Test
    @DisplayName("un error inesperado del adaptador se trata como recuperable")
    void treatsUnexpectedErrorAsRecoverable() {
        final Notification notification = storedNotification();
        final NotificationSenderPort sender = (target, route) ->
                Mono.error(new IllegalStateException("el proveedor cerró la conexión"));

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();

        final Notification stored = repository.findById(notification.id()).block();
        assertThat(stored.status()).isEqualTo(NotificationStatus.RECOVERABLE);
    }

    @Test
    @DisplayName("no se vuelve a despachar una notificación ya cerrada")
    void ignoresFinalNotifications() {
        final Notification notification = storedNotification();
        notification.markInProcess(NOW);
        notification.markDelivered(PROVIDER, "msg-1", NOW);
        notification.pullEvents();
        repository.preload(notification);

        final NotificationSenderPort sender = (target, route) -> {
            throw new AssertionError("no debería intentarse el envío de una notificación cerrada");
        };

        StepVerifier.create(useCaseWith(sender).dispatch(notification.id().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("falla si la notificación no existe")
    void failsWhenNotificationIsMissing() {
        final NotificationSenderPort sender = (target, route) ->
                Mono.just(NotificationSenderPort.SendOutcome.accepted(PROVIDER, "msg-1"));

        StepVerifier.create(useCaseWith(sender).dispatch(NotificationId.generate().value()))
                .expectError(NotificationNotFoundException.class)
                .verify();
    }
}
