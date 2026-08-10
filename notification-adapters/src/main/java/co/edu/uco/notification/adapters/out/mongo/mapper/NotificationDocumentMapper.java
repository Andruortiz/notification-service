package co.edu.uco.notification.adapters.out.mongo.mapper;

import co.edu.uco.notification.adapters.out.mongo.document.DeliveryAttemptDocument;
import co.edu.uco.notification.adapters.out.mongo.document.NotificationDocument;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.entity.DeliveryAttempt;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationContent;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.Recipient;
import co.edu.uco.notification.core.valueobject.TenantId;

import java.util.List;

/**
 * Traduce entre el agregado y su documento de persistencia.
 *
 * <p>La traducción es explícita a propósito: es la frontera que impide que una decisión de
 * almacenamiento —un nombre de campo, un tipo— se filtre hacia el dominio.
 */
public final class NotificationDocumentMapper {

    private NotificationDocumentMapper() {
    }

    public static NotificationDocument toDocument(final Notification notification) {
        final NotificationDocument document = new NotificationDocument();
        document.setId(notification.id().value());
        document.setTenantId(notification.tenantId().value());
        document.setExternalId(notification.externalId().value());
        document.setChannel(notification.channel().value());
        document.setRecipient(notification.recipient().value());
        document.setContent(notification.content().data());
        document.setPriority(notification.priority().name());
        document.setPriorityWeight(notification.priority().weight());
        document.setStatus(notification.status().name());
        document.setCreatedAt(notification.createdAt());
        document.setUpdatedAt(notification.updatedAt());
        document.setAttempts(notification.attempts().stream()
                .map(NotificationDocumentMapper::toDocument)
                .toList());
        return document;
    }

    public static Notification toDomain(final NotificationDocument document) {
        final List<DeliveryAttempt> attempts = document.getAttempts() == null
                ? List.of()
                : document.getAttempts().stream().map(NotificationDocumentMapper::toDomain).toList();

        return Notification.rehydrate(
                NotificationId.of(document.getId()),
                TenantId.of(document.getTenantId()),
                ExternalId.of(document.getExternalId()),
                ChannelType.of(document.getChannel()),
                Recipient.of(document.getRecipient()),
                NotificationContent.of(document.getContent()),
                Priority.valueOf(document.getPriority()),
                NotificationStatus.valueOf(document.getStatus()),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                attempts);
    }

    private static DeliveryAttemptDocument toDocument(final DeliveryAttempt attempt) {
        return new DeliveryAttemptDocument(
                attempt.number(),
                attempt.providerId(),
                attempt.outcome().name(),
                attempt.detail(),
                attempt.attemptedAt());
    }

    private static DeliveryAttempt toDomain(final DeliveryAttemptDocument document) {
        return new DeliveryAttempt(
                document.number(),
                document.providerId(),
                DeliveryAttempt.Outcome.valueOf(document.outcome()),
                document.detail(),
                document.attemptedAt());
    }
}
