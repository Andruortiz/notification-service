package co.edu.uco.notification.adapters.in.rest.mapper;

import co.edu.uco.notification.adapters.in.rest.request.SendNotificationRequest;
import co.edu.uco.notification.adapters.in.rest.response.NotificationAcceptedResponse;
import co.edu.uco.notification.adapters.in.rest.response.NotificationDetailResponse;
import co.edu.uco.notification.application.command.SendNotificationCommand;
import co.edu.uco.notification.application.dto.NotificationStatusView;
import co.edu.uco.notification.application.dto.SendNotificationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Traduce entre los objetos de transporte de la API y los de la capa de aplicación.
 *
 * <p>Existir esta frontera es lo que impide que un cambio en el contrato REST llegue al caso de
 * uso, y al revés. Aquí sí se usa MapStruct porque ambos lados son registros con nombres alineados
 * y el mapeo no necesita lógica.
 */
@Mapper(componentModel = "spring")
public interface NotificationRestMapper {

    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "externalId", source = "request.externalId")
    @Mapping(target = "channel", source = "request.channel")
    @Mapping(target = "recipient", source = "request.recipient")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "priority", source = "request.priority")
    SendNotificationCommand toCommand(String tenantId, SendNotificationRequest request);

    NotificationAcceptedResponse toResponse(SendNotificationResult result);

    NotificationDetailResponse toResponse(NotificationStatusView view);

    NotificationDetailResponse.AttemptResponse toResponse(NotificationStatusView.AttemptView attempt);
}
