package co.edu.uco.notification.adapters.out.mongo.repository;

import co.edu.uco.notification.adapters.out.mongo.document.NotificationDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo de Spring Data.
 *
 * <p>Es un detalle de infraestructura: el dominio no lo conoce, sino que habla con
 * {@code NotificationRepository}, cuya implementación se apoya en este.
 */
public interface NotificationMongoRepository extends ReactiveMongoRepository<NotificationDocument, String> {

    Mono<NotificationDocument> findByTenantIdAndExternalId(String tenantId, String externalId);

    Mono<NotificationDocument> findByTenantIdAndId(String tenantId, String id);

    Flux<NotificationDocument> findByStatusOrderByPriorityWeightDescCreatedAtAsc(
            String status, Pageable pageable);
}
