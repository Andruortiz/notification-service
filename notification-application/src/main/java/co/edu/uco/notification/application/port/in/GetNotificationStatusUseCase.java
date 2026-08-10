package co.edu.uco.notification.application.port.in;

import co.edu.uco.notification.application.dto.NotificationStatusView;
import co.edu.uco.notification.application.query.GetNotificationStatusQuery;
import reactor.core.publisher.Mono;

/** Puerto de entrada para consultar el estado y la trazabilidad de una notificación. */
public interface GetNotificationStatusUseCase {

    Mono<NotificationStatusView> findStatus(GetNotificationStatusQuery query);
}
