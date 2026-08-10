package co.edu.uco.notification.application.mapper;

import co.edu.uco.notification.application.dto.NotificationStatusView;
import co.edu.uco.notification.core.aggregate.Notification;

/**
 * Traduce el agregado a su vista de consulta.
 *
 * <p>Está escrito a mano de forma deliberada. El agregado expone accesores al estilo de los
 * registros ({@code id()}, {@code status()}) en lugar de métodos {@code getX()}, y forzar a
 * MapStruct a trabajar con ellos exigiría una expresión explícita por campo: más ruido que el
 * mapeo directo. MapStruct sí se usa en la frontera REST, donde ambos lados son registros.
 */
public final class NotificationViewMapper {

    private NotificationViewMapper() {
    }

    public static NotificationStatusView toView(final Notification notification) {
        return new NotificationStatusView(
                notification.id().value(),
                notification.tenantId().value(),
                notification.channel().value(),
                notification.recipient().value(),
                notification.status().name(),
                notification.priority().name(),
                notification.createdAt(),
                notification.updatedAt(),
                notification.attempts().stream()
                        .map(attempt -> new NotificationStatusView.AttemptView(
                                attempt.number(),
                                attempt.providerId(),
                                attempt.outcome().name(),
                                attempt.detail(),
                                attempt.attemptedAt()))
                        .toList());
    }
}
