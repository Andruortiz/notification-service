package co.edu.uco.notification.adapters.in.scheduler;

import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.core.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Reconciliador: devuelve a la cola las notificaciones que quedaron pendientes.
 *
 * <p>Cubre el hueco entre persistir la notificación y publicarla: si el proceso cae justo en medio,
 * la notificación existe en la base de datos pero nadie la despacharía. Este proceso la recupera.
 *
 * <p>Se apoya en {@code Flux.interval} y no en {@code @Scheduled} de forma deliberada: la tarea es
 * reactiva de principio a fin y {@code concatMap} garantiza que una ejecución no se solape con la
 * siguiente sin necesidad de un candado manual.
 *
 * <p>En esta línea base solo reencola. La espera creciente por canal y el tratamiento de las
 * notificaciones recuperables llegan en la siguiente etapa.
 */
@Component
public class PendingNotificationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PendingNotificationScheduler.class);

    private final NotificationRepository repository;
    private final NotificationEventPublisherPort eventPublisher;
    private final boolean enabled;
    private final Duration interval;
    private final int batchSize;

    public PendingNotificationScheduler(
            final NotificationRepository repository,
            final NotificationEventPublisherPort eventPublisher,
            @Value("${notification.reconciler.enabled:true}") final boolean enabled,
            @Value("${notification.reconciler.interval:PT2M}") final Duration interval,
            @Value("${notification.reconciler.batch-size:50}") final int batchSize) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
        this.interval = interval;
        this.batchSize = batchSize;
    }

    /**
     * Arranca la tarea periódica. Se suscribe una sola vez al construirse el contexto y vive
     * mientras viva la aplicación.
     */
    @PostConstruct
    public void start() {
        if (!enabled) {
            LOG.info("Reconciliador deshabilitado por configuración");
            return;
        }
        LOG.info("Reconciliador activo cada {} con lotes de {}", interval, batchSize);
        Flux.interval(interval, interval)
                .onBackpressureDrop()
                .concatMap(tick -> reconcile())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        error -> LOG.error("El reconciliador se detuvo por un error", error));
    }

    private Mono<Long> reconcile() {
        return repository.findPending(batchSize)
                .concatMap(eventPublisher::enqueueForDispatch)
                .count()
                .doOnNext(reenqueued -> {
                    if (reenqueued > 0) {
                        LOG.info("Reconciliador: {} notificaciones devueltas a la cola", reenqueued);
                    }
                })
                .onErrorResume(error -> {
                    LOG.error("Fallo en una pasada del reconciliador", error);
                    return Mono.just(0L);
                });
    }
}
