package co.edu.uco.notification.core.aggregate;

import co.edu.uco.notification.core.entity.DeliveryAttempt;
import co.edu.uco.notification.core.event.DomainEvent;
import co.edu.uco.notification.core.event.NotificationAccepted;
import co.edu.uco.notification.core.event.NotificationDelivered;
import co.edu.uco.notification.core.event.NotificationFailed;
import co.edu.uco.notification.core.exception.InvalidStatusTransitionException;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationContent;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.Recipient;
import co.edu.uco.notification.core.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Notification")
class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

    private static Notification newNotification() {
        return Notification.accept(
                TenantId.of("INQ-101"),
                ExternalId.of("sol-001"),
                ChannelType.of("TEST_CHANNEL"),
                Recipient.of("destino@ejemplo.com"),
                NotificationContent.of(Map.of("asunto", "Hola")),
                Priority.HIGH,
                NOW);
    }

    @Nested
    @DisplayName("Al aceptar la solicitud")
    class OnAccept {

        @Test
        @DisplayName("nace pendiente y con identificador propio")
        void startsPending() {
            final Notification notification = newNotification();

            assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
            assertThat(notification.id().value()).isNotBlank();
            assertThat(notification.createdAt()).isEqualTo(NOW);
            assertThat(notification.attempts()).isEmpty();
        }

        @Test
        @DisplayName("registra el evento de aceptación sin que nadie tenga que acordarse")
        void registersAcceptedEvent() {
            final List<DomainEvent> events = newNotification().pullEvents();

            assertThat(events).hasSize(1).first().isInstanceOf(NotificationAccepted.class);
        }

        @Test
        @DisplayName("los eventos se entregan una sola vez")
        void eventsAreDrained() {
            final Notification notification = newNotification();

            assertThat(notification.pullEvents()).hasSize(1);
            assertThat(notification.pullEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transitions {

        @Test
        @DisplayName("el camino feliz llega a entregada y deja el intento registrado")
        void happyPath() {
            final Notification notification = newNotification();

            notification.markInProcess(NOW);
            notification.markDelivered("simulated", "msg-1", NOW);

            assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.attempts())
                    .singleElement()
                    .satisfies(attempt -> {
                        assertThat(attempt.succeeded()).isTrue();
                        assertThat(attempt.number()).isEqualTo(1);
                        assertThat(attempt.providerId()).isEqualTo("simulated");
                    });
            assertThat(notification.pullEvents())
                    .anyMatch(NotificationDelivered.class::isInstance);
        }

        @Test
        @DisplayName("no se puede entregar sin haber pasado por el despacho")
        void cannotSkipInProcess() {
            final Notification notification = newNotification();

            assertThatThrownBy(() -> notification.markDelivered("simulated", "msg-1", NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("DELIVERED");
        }

        @Test
        @DisplayName("un estado final no admite más transiciones")
        void finalStateIsClosed() {
            final Notification notification = newNotification();
            notification.markInProcess(NOW);
            notification.markDelivered("simulated", "msg-1", NOW);

            assertThatThrownBy(() -> notification.markInProcess(NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("un fallo recuperable permite volver a la cola")
        void recoverableCanBeRequeued() {
            final Notification notification = newNotification();
            notification.markInProcess(NOW);
            notification.markRecoverable("simulated", "429", NOW);

            assertThat(notification.status()).isEqualTo(NotificationStatus.RECOVERABLE);
            assertThat(notification.status().allowsRetry()).isTrue();

            notification.requeue(NOW);

            assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
        }

        @Test
        @DisplayName("un fallo definitivo aísla la notificación y avisa")
        void permanentFailureIsIsolated() {
            final Notification notification = newNotification();
            notification.markInProcess(NOW);
            notification.pullEvents();

            notification.markFailed("simulated", "400_INVALID_DESTINATION", NOW);

            assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.status().isFinal()).isTrue();
            assertThat(notification.attempts())
                    .singleElement()
                    .extracting(DeliveryAttempt::outcome)
                    .isEqualTo(DeliveryAttempt.Outcome.PERMANENT_FAILURE);
            assertThat(notification.pullEvents())
                    .singleElement()
                    .isInstanceOf(NotificationFailed.class);
        }

        @Test
        @DisplayName("los intentos se numeran de forma correlativa")
        void attemptsAreNumbered() {
            final Notification notification = newNotification();

            notification.markInProcess(NOW);
            notification.markRecoverable("simulated", "429", NOW);
            notification.markInProcess(NOW);
            notification.markFailed("simulated", "agotado", NOW);

            assertThat(notification.attempts())
                    .extracting(DeliveryAttempt::number)
                    .containsExactly(1, 2);
        }
    }
}
