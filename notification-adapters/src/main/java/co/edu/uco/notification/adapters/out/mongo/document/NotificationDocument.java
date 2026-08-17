package co.edu.uco.notification.adapters.out.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Representación de la notificación en la base de datos.
 *
 * <p>Es un documento de persistencia, no el agregado: existe para que el modelo de almacenamiento
 * pueda cambiar sin arrastrar al dominio. El índice único sobre cliente y referencia externa es lo
 * que hace cumplir la idempotencia en la base de datos y no solo en el código, de modo que dos
 * réplicas atendiendo la misma solicitud a la vez no puedan duplicar el envío.
 */
@Document(collection = "notifications")
@CompoundIndex(name = "idx_tenant_external", def = "{'tenantId': 1, 'externalId': 1}", unique = true)
@CompoundIndex(name = "idx_pending_dispatch", def = "{'status': 1, 'priorityWeight': -1, 'createdAt': 1}")
public class NotificationDocument {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String externalId;
    private String channel;
    private String recipient;
    private Map<String, Object> content;
    private String priority;
    private int priorityWeight;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DeliveryAttemptDocument> attempts;

    @Version
    private Long version;

    public NotificationDocument() {
        // Requerido por el mapeo de Spring Data.
    }
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(final String recipient) {
        this.recipient = recipient;
    }

    public Map<String, Object> getContent() {
        return content == null ? Map.of() : Map.copyOf(content);
    }

    public void setContent(final Map<String, Object> content) {
        this.content = content == null ? Map.of() : Map.copyOf(content);
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(final String priority) {
        this.priority = priority;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }

    public void setPriorityWeight(final int priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DeliveryAttemptDocument> getAttempts() {
        return attempts == null ? List.of() : List.copyOf(attempts);
    }

    public void setAttempts(final List<DeliveryAttemptDocument> attempts) {
        this.attempts = attempts == null ? List.of() : List.copyOf(attempts);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }
}
