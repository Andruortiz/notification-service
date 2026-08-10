package co.edu.uco.notification.adapters.out.provider;

import co.edu.uco.notification.adapters.config.ChannelCatalogProperties;
import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Catálogo de canales resuelto desde la configuración del servicio.
 *
 * <p>Es la implementación provisional del puerto: alcanza para la línea base y demuestra la
 * propiedad que interesa, que es poder dar de alta un canal sin recompilar. Cuando exista el
 * componente de Parámetros, su cliente ocupará este mismo puerto y esta clase pasará a ser el
 * respaldo local para desarrollo.
 */
@Component
public class ConfigurationChannelCatalogAdapter implements ChannelCatalogPort {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationChannelCatalogAdapter.class);

    private final ChannelCatalogProperties properties;

    public ConfigurationChannelCatalogAdapter(final ChannelCatalogProperties properties) {
        this.properties = properties;
        LOG.info("Canales configurados: {}", properties.channels().keySet());
    }

    @Override
    public Mono<ChannelRoute> findActiveRoute(final ChannelType channel) {
        return Mono.justOrEmpty(properties.channels().get(channel.value()))
                .filter(ChannelCatalogProperties.ChannelDefinition::enabled)
                .map(definition -> new ChannelRoute(
                        channel,
                        definition.providerId(),
                        new RetryPolicy(
                                definition.maxAttemptsOrDefault(),
                                definition.initialBackoffOrDefault(),
                                definition.backoffMultiplierOrDefault())));
    }
}
