package co.edu.uco.notification.application.support;

import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.repository.NotificationRepository;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio en memoria para las pruebas de los casos de uso.
 *
 * <p>Que exista y sea trivial de escribir es en sí una comprobación del diseño: si probar un caso
 * de uso obligara a levantar una base de datos, sería señal de que el dominio no está aislado.
 */
public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notification> storage = new ConcurrentHashMap<>();
    private final AtomicInteger saveCount = new AtomicInteger();

    @Override
    public Mono<Notification> save(final Notification notification) {
        saveCount.incrementAndGet();
        storage.put(notification.id().value(), notification);
        return Mono.just(notification);
    }

    @Override
    public Mono<Notification> findById(final NotificationId id) {
        return Mono.justOrEmpty(storage.get(id.value()));
    }

    @Override
    public Mono<Notification> findByTenantAndId(final TenantId tenantId, final NotificationId id) {
        return findById(id).filter(notification -> notification.tenantId().equals(tenantId));
    }

    @Override
    public Mono<Notification> findByTenantAndExternalId(
            final TenantId tenantId, final ExternalId externalId) {
        return Flux.fromIterable(storage.values())
                .filter(notification -> notification.tenantId().equals(tenantId))
                .filter(notification -> notification.externalId().equals(externalId))
                .next();
    }

    @Override
    public Flux<Notification> findPending(final int limit) {
        return Flux.fromIterable(storage.values())
                .filter(notification -> notification.status() == NotificationStatus.PENDING)
                .sort(Comparator.comparing(Notification::createdAt))
                .take(limit);
    }

    /** Número de escrituras, para comprobar que una solicitud repetida no persiste de nuevo. */
    public int saveCount() {
        return saveCount.get();
    }

    public void preload(final Notification notification) {
        storage.put(notification.id().value(), notification);
    }
}
