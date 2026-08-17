package co.edu.uco.notification.adapters.in.rest;

import co.edu.uco.notification.adapters.in.rest.mapper.NotificationRestMapper;
import co.edu.uco.notification.adapters.in.rest.request.SendNotificationRequest;
import co.edu.uco.notification.adapters.in.rest.response.NotificationAcceptedResponse;
import co.edu.uco.notification.adapters.in.rest.response.NotificationDetailResponse;
import co.edu.uco.notification.application.port.in.GetNotificationStatusUseCase;
import co.edu.uco.notification.application.port.in.SendNotificationUseCase;
import co.edu.uco.notification.application.query.GetNotificationStatusQuery;
import co.edu.uco.notification.shared.correlation.CorrelationId;
import co.edu.uco.notification.shared.logging.LogSanitizer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * API pública del componente.
 *
 * <p>El adaptador solo traduce y delega: no contiene reglas de negocio. Su recorrido es
 * petición &rarr; comando &rarr; caso de uso &rarr; respuesta, y todo lo que decide qué ocurre está
 * al otro lado del puerto.
 *
 * <p>El cliente se identifica hoy con la cabecera {@code X-Tenant-Id}. Cuando el componente de
 * Seguridad esté disponible, ese dato saldrá del token y este será el único punto que cambie.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final SendNotificationUseCase sendNotificationUseCase;
    private final GetNotificationStatusUseCase getNotificationStatusUseCase;
    private final NotificationRestMapper mapper;

    public NotificationController(
            final SendNotificationUseCase sendNotificationUseCase,
            final GetNotificationStatusUseCase getNotificationStatusUseCase,
            final NotificationRestMapper mapper) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.getNotificationStatusUseCase = getNotificationStatusUseCase;
        this.mapper = mapper;
    }

    /**
     * Acepta una solicitud de envío.
     *
     * <p>Responde {@code 202 Accepted} y no {@code 201}: la notificación queda aceptada y encolada,
     * pero la entrega ocurre después. Decir {@code 201} daría a entender que ya se envió.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<NotificationAcceptedResponse> send(
            @RequestHeader(TENANT_HEADER) final String tenantId,
            @RequestHeader(name = CorrelationId.HEADER, required = false) final String correlationId,
            @Valid @RequestBody final SendNotificationRequest request) {

        final CorrelationId correlation = CorrelationId.ofNullable(correlationId);
        final String safeTenantId =
                LogSanitizer.sanitize(tenantId);

        final String safeChannel =
                LogSanitizer.sanitize(request.channel());

        final String safeCorrelation =
                LogSanitizer.sanitize(correlation != null ? correlation.value() : null);

        LOG.info(
                "Solicitud de envío recibida cliente={} canal={} correlacion={}",
                safeTenantId,
                safeChannel,
                safeCorrelation
        );

        return sendNotificationUseCase.send(mapper.toCommand(tenantId, request))
                .map(mapper::toResponse);
    }

    /** Consulta el estado y la trazabilidad de una notificación del cliente indicado. */
    @GetMapping("/{notificationId}")
    public Mono<NotificationDetailResponse> findById(
            @RequestHeader(TENANT_HEADER) final String tenantId,
            @PathVariable final String notificationId) {

        return getNotificationStatusUseCase
                .findStatus(new GetNotificationStatusQuery(tenantId, notificationId))
                .map(mapper::toResponse);
    }
}
