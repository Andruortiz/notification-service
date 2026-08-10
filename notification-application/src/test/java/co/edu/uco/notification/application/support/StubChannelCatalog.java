package co.edu.uco.notification.application.support;

import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.ChannelType;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/** Catálogo de canales configurable desde la propia prueba. */
public class StubChannelCatalog implements ChannelCatalogPort {

    private final Map<String, ChannelRoute> routes = new HashMap<>();

    public StubChannelCatalog register(
            final String channel, final String providerId, final RetryPolicy retryPolicy) {
        final ChannelType type = ChannelType.of(channel);
        routes.put(type.value(), new ChannelRoute(type, providerId, retryPolicy));
        return this;
    }

    @Override
    public Mono<ChannelRoute> findActiveRoute(final ChannelType channel) {
        return Mono.justOrEmpty(routes.get(channel.value()));
    }
}
