package co.edu.uco.notification.application.support;

import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.event.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Publicador que solo anota lo que se le pide, para poder afirmar sobre ello en las pruebas. */
public class RecordingEventPublisher implements NotificationEventPublisherPort {

    private final List<DomainEvent> published = new CopyOnWriteArrayList<>();
    private final List<String> enqueued = new CopyOnWriteArrayList<>();

    @Override
    public Mono<Void> publish(final List<DomainEvent> events) {
        published.addAll(events);
        return Mono.empty();
    }

    @Override
    public Mono<Void> enqueueForDispatch(final Notification notification) {
        enqueued.add(notification.id().value());
        return Mono.empty();
    }

    public List<DomainEvent> published() {
        return new ArrayList<>(published);
    }

    public List<String> enqueued() {
        return new ArrayList<>(enqueued);
    }
}
