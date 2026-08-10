package co.edu.uco.notification.adapters.out.mongo;

import co.edu.uco.notification.adapters.out.mongo.mapper.NotificationDocumentMapper;
import co.edu.uco.notification.adapters.out.mongo.repository.NotificationMongoRepository;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.repository.NotificationRepository;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.core.valueobject.TenantId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementación del repositorio del dominio sobre MongoDB.
 *
 * <p>Es el único punto del componente que sabe que la persistencia es documental. Cambiarla por
 * otra tecnología significa escribir otro adaptador como este; el dominio y los casos de uso no se
 * enteran.
 */
@Repository
public class NotificationMongoAdapter implements NotificationRepository {

    private final NotificationMongoRepository repository;

    public NotificationMongoAdapter(final NotificationMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Notification> save(final Notification notification) {
        return repository.save(NotificationDocumentMapper.toDocument(notification))
                .map(NotificationDocumentMapper::toDomain);
    }

    @Override
    public Mono<Notification> findById(final NotificationId id) {
        return repository.findById(id.value()).map(NotificationDocumentMapper::toDomain);
    }

    @Override
    public Mono<Notification> findByTenantAndId(final TenantId tenantId, final NotificationId id) {
        return repository.findByTenantIdAndId(tenantId.value(), id.value())
                .map(NotificationDocumentMapper::toDomain);
    }

    @Override
    public Mono<Notification> findByTenantAndExternalId(
            final TenantId tenantId, final ExternalId externalId) {
        return repository.findByTenantIdAndExternalId(tenantId.value(), externalId.value())
                .map(NotificationDocumentMapper::toDomain);
    }

    @Override
    public Flux<Notification> findPending(final int limit) {
        return repository.findByStatusOrderByPriorityWeightDescCreatedAtAsc(
                        NotificationStatus.PENDING.name(), PageRequest.ofSize(limit))
                .map(NotificationDocumentMapper::toDomain);
    }
}
