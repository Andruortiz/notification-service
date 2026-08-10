package co.edu.uco.notification.application.usecase;

import co.edu.uco.notification.application.command.SendNotificationCommand;
import co.edu.uco.notification.application.port.in.SendNotificationUseCase;
import co.edu.uco.notification.application.support.InMemoryNotificationRepository;
import co.edu.uco.notification.application.support.RecordingEventPublisher;
import co.edu.uco.notification.application.support.StubChannelCatalog;
import co.edu.uco.notification.core.event.NotificationAccepted;
import co.edu.uco.notification.core.event.NotificationQueued;
import co.edu.uco.notification.core.exception.ChannelNotAvailableException;
import co.edu.uco.notification.core.exception.InvalidNotificationDataException;
import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caso de uso Enviar Notificación")
class SendNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");
    private static final String CHANNEL = "TEST_CHANNEL";

    private InMemoryNotificationRepository repository;
    private RecordingEventPublisher eventPublisher;
    private SendNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
        eventPublisher = new RecordingEventPublisher();
        final StubChannelCatalog catalog =
                new StubChannelCatalog().register(CHANNEL, "simulated", RetryPolicy.defaultPolicy());

        useCase = new SendNotificationService(
                repository, catalog, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static SendNotificationCommand command(final String externalId) {
        return new SendNotificationCommand(
                "INQ-101", externalId, CHANNEL, "destino@ejemplo.com",
                Map.of("asunto", "Hola"), "HIGH");
    }

    @Test
    @DisplayName("acepta la solicitud, la persiste y la encola")
    void acceptsAndEnqueues() {
        StepVerifier.create(useCase.send(command("sol-001")))
                .assertNext(result -> {
                    assertThat(result.notificationId()).isNotBlank();
                    assertThat(result.status()).isEqualTo(NotificationStatus.PENDING.name());
                    assertThat(result.duplicate()).isFalse();
                    assertThat(result.acceptedAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(eventPublisher.enqueued()).hasSize(1);
        assertThat(eventPublisher.published())
                .hasAtLeastOneElementOfType(NotificationAccepted.class)
                .hasAtLeastOneElementOfType(NotificationQueued.class);
    }

    @Test
    @DisplayName("rechaza un canal que no está en el catálogo")
    void rejectsUnknownChannel() {
        final SendNotificationCommand unknown = new SendNotificationCommand(
                "INQ-101", "sol-002", "CANAL_QUE_NO_EXISTE", "destino@ejemplo.com",
                Map.of("asunto", "Hola"), null);

        StepVerifier.create(useCase.send(unknown))
                .expectError(ChannelNotAvailableException.class)
                .verify();

        assertThat(repository.saveCount()).isZero();
        assertThat(eventPublisher.enqueued()).isEmpty();
    }

    @Test
    @DisplayName("una solicitud repetida devuelve la misma notificación y no genera otro envío")
    void isIdempotent() {
        final String first = useCase.send(command("sol-003")).block().notificationId();

        StepVerifier.create(useCase.send(command("sol-003")))
                .assertNext(result -> {
                    assertThat(result.duplicate()).isTrue();
                    assertThat(result.notificationId()).isEqualTo(first);
                })
                .verifyComplete();

        assertThat(repository.saveCount())
                .as("la repetición no debe persistir una segunda notificación")
                .isEqualTo(1);
        assertThat(eventPublisher.enqueued())
                .as("la repetición no debe encolar un segundo despacho")
                .hasSize(1);
    }

    @Test
    @DisplayName("dos clientes pueden usar la misma referencia externa sin interferirse")
    void idempotencyIsScopedByTenant() {
        useCase.send(command("misma-referencia")).block();

        final SendNotificationCommand otherTenant = new SendNotificationCommand(
                "INQ-999", "misma-referencia", CHANNEL, "otro@ejemplo.com",
                Map.of("asunto", "Hola"), null);

        StepVerifier.create(useCase.send(otherTenant))
                .assertNext(result -> assertThat(result.duplicate()).isFalse())
                .verifyComplete();

        assertThat(repository.saveCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza una solicitud sin destinatario")
    void rejectsMissingRecipient() {
        final SendNotificationCommand invalid = new SendNotificationCommand(
                "INQ-101", "sol-004", CHANNEL, "  ", Map.of("asunto", "Hola"), null);

        StepVerifier.create(useCase.send(invalid))
                .expectError(InvalidNotificationDataException.class)
                .verify();
    }
}
