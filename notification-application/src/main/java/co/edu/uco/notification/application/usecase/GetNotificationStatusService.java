package co.edu.uco.notification.application.usecase;

import co.edu.uco.notification.application.dto.NotificationStatusView;
import co.edu.uco.notification.application.mapper.NotificationViewMapper;
import co.edu.uco.notification.application.port.in.GetNotificationStatusUseCase;
import co.edu.uco.notification.application.query.GetNotificationStatusQuery;
import co.edu.uco.notification.core.exception.NotificationNotFoundException;
import co.edu.uco.notification.core.repository.NotificationRepository;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Consulta del estado y la trazabilidad de una notificación.
 *
 * <p>La búsqueda se acota por cliente. Una notificación de otro cliente se comporta igual que una
 * inexistente: se responde «no encontrada» en lugar de «no autorizado», para no revelar que el
 * identificador existe.
 */
public class GetNotificationStatusService implements GetNotificationStatusUseCase {

    private final NotificationRepository repository;

    public GetNotificationStatusService(final NotificationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Mono<NotificationStatusView> findStatus(final GetNotificationStatusQuery query) {
        final NotificationId id = NotificationId.of(query.notificationId());
        return repository.findByTenantAndId(TenantId.of(query.tenantId()), id)
                .switchIfEmpty(Mono.error(() -> new NotificationNotFoundException(id)))
                .map(NotificationViewMapper::toView);
    }
}
