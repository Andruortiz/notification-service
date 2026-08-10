package co.edu.uco.notification;

import co.edu.uco.notification.adapters.in.rest.NotificationController;
import co.edu.uco.notification.adapters.in.rest.mapper.NotificationRestMapperImpl;
import co.edu.uco.notification.application.dto.NotificationStatusView;
import co.edu.uco.notification.application.dto.SendNotificationResult;
import co.edu.uco.notification.application.port.in.GetNotificationStatusUseCase;
import co.edu.uco.notification.application.port.in.SendNotificationUseCase;
import co.edu.uco.notification.bootstrap.exception.GlobalExceptionHandler;
import co.edu.uco.notification.core.exception.ChannelNotAvailableException;
import co.edu.uco.notification.core.exception.NotificationNotFoundException;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.shared.correlation.CorrelationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Verifica el contrato HTTP de la API sin necesidad de base de datos ni broker.
 *
 * <p>Cubre lo que un sistema cliente percibe: el código de respuesta, la forma del cuerpo y el
 * tratamiento de los errores. Los casos de uso van simulados a propósito: su comportamiento ya se
 * prueba en su propio módulo, y mezclarlo aquí haría la prueba lenta y ambigua.
 */
@WebFluxTest(controllers = NotificationController.class)
@Import({NotificationRestMapperImpl.class, GlobalExceptionHandler.class})
@DisplayName("API de notificaciones")
class NotificationApiTest {

    private static final String TENANT = "INQ-101";
    private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private SendNotificationUseCase sendNotificationUseCase;

    @MockitoBean
    private GetNotificationStatusUseCase getNotificationStatusUseCase;

    private static Map<String, Object> validBody() {
        return Map.of(
                "externalId", "sol-001",
                "channel", "EMAIL",
                "recipient", "destino@ejemplo.com",
                "content", Map.of("asunto", "Hola"),
                "priority", "HIGH");
    }

    @Test
    @DisplayName("acepta una solicitud válida con 202 y devuelve el identificador de seguimiento")
    void acceptsValidRequest() {
        given(sendNotificationUseCase.send(any()))
                .willReturn(Mono.just(SendNotificationResult.accepted("not-1", "PENDING", NOW)));

        client.post().uri("/api/v1/notifications")
                .header("X-Tenant-Id", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validBody())
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().exists(CorrelationId.HEADER)
                .expectBody()
                .jsonPath("$.notificationId").isEqualTo("not-1")
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.duplicate").isEqualTo(false);
    }

    @Test
    @DisplayName("rechaza con 400 y detalle por campo cuando falta un dato obligatorio")
    void rejectsInvalidBody() {
        final Map<String, Object> incomplete = Map.of(
                "channel", "EMAIL",
                "recipient", "destino@ejemplo.com",
                "content", Map.of("asunto", "Hola"));

        client.post().uri("/api/v1/notifications")
                .header("X-Tenant-Id", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(incomplete)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("NOT-0002")
                .jsonPath("$.fields.externalId").exists();
    }

    @Test
    @DisplayName("rechaza con 400 cuando falta la cabecera del cliente")
    void rejectsMissingTenantHeader() {
        client.post().uri("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validBody())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("un canal inexistente responde 400 con su código de error")
    void reportsUnknownChannel() {
        given(sendNotificationUseCase.send(any()))
                .willReturn(Mono.error(new ChannelNotAvailableException(ChannelType.of("NO_EXISTE"))));

        client.post().uri("/api/v1/notifications")
                .header("X-Tenant-Id", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validBody())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("NOT-0001")
                .jsonPath("$.title").isEqualTo("Canal no disponible");
    }

    @Test
    @DisplayName("devuelve el detalle de una notificación existente")
    void returnsNotificationDetail() {
        given(getNotificationStatusUseCase.findStatus(any()))
                .willReturn(Mono.just(new NotificationStatusView(
                        "not-1", TENANT, "EMAIL", "destino@ejemplo.com", "DELIVERED", "HIGH",
                        NOW, NOW,
                        List.of(new NotificationStatusView.AttemptView(
                                1, "simulated", "SUCCESS", "OK", NOW)))));

        client.get().uri("/api/v1/notifications/not-1")
                .header("X-Tenant-Id", TENANT)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("DELIVERED")
                .jsonPath("$.attempts[0].providerId").isEqualTo("simulated");
    }

    @Test
    @DisplayName("una notificación de otro cliente se responde como no encontrada")
    void hidesForeignNotifications() {
        given(getNotificationStatusUseCase.findStatus(any()))
                .willReturn(Mono.error(new NotificationNotFoundException(NotificationId.of("not-9"))));

        client.get().uri("/api/v1/notifications/not-9")
                .header("X-Tenant-Id", TENANT)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("NOT-0004");
    }
}
