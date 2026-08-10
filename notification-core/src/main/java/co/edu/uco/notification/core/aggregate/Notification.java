package co.edu.uco.notification.core.aggregate;

import co.edu.uco.notification.core.entity.DeliveryAttempt;
import co.edu.uco.notification.core.event.DomainEvent;
import co.edu.uco.notification.core.event.NotificationAccepted;
import co.edu.uco.notification.core.event.NotificationDelivered;
import co.edu.uco.notification.core.event.NotificationFailed;
import co.edu.uco.notification.core.event.NotificationQueued;
import co.edu.uco.notification.core.service.StatusTransitionPolicy;
import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationContent;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.Recipient;
import co.edu.uco.notification.core.valueobject.TenantId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado: una solicitud de comunicación con un destinatario.
 *
 * <p>Concentra el ciclo de vida completo. El estado no se cambia asignando un campo desde fuera:
 * se cambia invocando la transición correspondiente, que valida su legitimidad y registra el
 * evento de dominio asociado. Así el estado del agregado nunca puede quedar en una combinación
 * imposible, y los eventos no dependen de que alguien se acuerde de publicarlos.
 */
public final class Notification {

    private final NotificationId id;
    private final TenantId tenantId;
    private final ExternalId externalId;
    private final ChannelType channel;
    private final Recipient recipient;
    private final NotificationContent content;
    private final Priority priority;
    private final Instant createdAt;

    private NotificationStatus status;
    private Instant updatedAt;
    private final List<DeliveryAttempt> attempts;
    private final transient List<DomainEvent> pendingEvents = new ArrayList<>();

    private Notification(
            final NotificationId id,
            final TenantId tenantId,
            final ExternalId externalId,
            final ChannelType channel,
            final Recipient recipient,
            final NotificationContent content,
            final Priority priority,
            final NotificationStatus status,
            final Instant createdAt,
            final Instant updatedAt,
            final List<DeliveryAttempt> attempts) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.recipient = Objects.requireNonNull(recipient, "recipient");
        this.content = Objects.requireNonNull(content, "content");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.attempts = new ArrayList<>(attempts == null ? List.of() : attempts);
    }

    /**
     * Acepta una solicitud nueva. Es la única forma de crear una notificación desde cero: nace en
     * {@link NotificationStatus#PENDING} y con su evento de aceptación ya registrado.
     */
    public static Notification accept(
            final TenantId tenantId,
            final ExternalId externalId,
            final ChannelType channel,
            final Recipient recipient,
            final NotificationContent content,
            final Priority priority,
            final Instant now) {

        final Notification notification = new Notification(
                NotificationId.generate(), tenantId, externalId, channel, recipient, content,
                priority, NotificationStatus.PENDING, now, now, List.of());

        notification.pendingEvents.add(
                new NotificationAccepted(notification.id, tenantId, channel, priority, now));
        return notification;
    }

    /**
     * Reconstruye el agregado desde la persistencia sin disparar eventos ni validar transiciones:
     * los hechos ya ocurrieron.
     */
    public static Notification rehydrate(
            final NotificationId id,
            final TenantId tenantId,
            final ExternalId externalId,
            final ChannelType channel,
            final Recipient recipient,
            final NotificationContent content,
            final Priority priority,
            final NotificationStatus status,
            final Instant createdAt,
            final Instant updatedAt,
            final List<DeliveryAttempt> attempts) {
        return new Notification(id, tenantId, externalId, channel, recipient, content, priority,
                status, createdAt, updatedAt, attempts);
    }

    /** Marca la publicación en la cola de despacho. */
    public void markQueued(final Instant now) {
        pendingEvents.add(new NotificationQueued(id, channel, priority, now));
        this.updatedAt = now;
    }

    /** El despachador tomó la notificación y comienza el envío. */
    public void markInProcess(final Instant now) {
        transitionTo(NotificationStatus.IN_PROCESS, now);
    }

    /** El proveedor aceptó el envío. */
    public void markDelivered(final String providerId, final String providerMessageId, final Instant now) {
        transitionTo(NotificationStatus.DELIVERED, now);
        attempts.add(DeliveryAttempt.success(attempts.size() + 1, providerId, providerMessageId, now));
        pendingEvents.add(new NotificationDelivered(id, providerId, providerMessageId, now));
    }

    /** Fallo transitorio: la notificación volverá a intentarse más adelante. */
    public void markRecoverable(final String providerId, final String detail, final Instant now) {
        transitionTo(NotificationStatus.RECOVERABLE, now);
        attempts.add(DeliveryAttempt.recoverableFailure(attempts.size() + 1, providerId, detail, now));
    }

    /** Fallo definitivo: la notificación queda aislada para revisión. */
    public void markFailed(final String providerId, final String reason, final Instant now) {
        transitionTo(NotificationStatus.FAILED, now);
        attempts.add(DeliveryAttempt.permanentFailure(attempts.size() + 1, providerId, reason, now));
        pendingEvents.add(new NotificationFailed(id, reason, attempts.size(), now));
    }

    /** Se descarta sin intentar el envío: venció su vigencia o el destinatario no acepta el canal. */
    public void discard(final Instant now) {
        transitionTo(NotificationStatus.DISCARDED, now);
    }

    /** Devuelve la notificación a la cola tras un fallo recuperable. */
    public void requeue(final Instant now) {
        transitionTo(NotificationStatus.PENDING, now);
        pendingEvents.add(new NotificationQueued(id, channel, priority, now));
    }

    private void transitionTo(final NotificationStatus target, final Instant now) {
        StatusTransitionPolicy.verify(status, target);
        this.status = target;
        this.updatedAt = now;
    }

    /**
     * Entrega los eventos acumulados y limpia la lista, para que publicarlos dos veces no sea
     * posible por descuido.
     */
    public List<DomainEvent> pullEvents() {
        final List<DomainEvent> copy = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    public NotificationId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ExternalId externalId() {
        return externalId;
    }

    public ChannelType channel() {
        return channel;
    }

    public Recipient recipient() {
        return recipient;
    }

    public NotificationContent content() {
        return content;
    }

    public Priority priority() {
        return priority;
    }

    public NotificationStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<DeliveryAttempt> attempts() {
        return Collections.unmodifiableList(attempts);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Notification that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Notification[id=%s, tenant=%s, channel=%s, status=%s]"
                .formatted(id, tenantId, channel, status);
    }
}
