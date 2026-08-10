package co.edu.uco.notification.adapters.out.provider;

import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.core.aggregate.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enruta cada envío al proveedor que indica el catálogo.
 *
 * <p>Spring inyecta aquí todos los proveedores registrados, y el enrutador los indexa por su
 * identificador. <strong>No hay ninguna condición sobre el nombre del canal:</strong> la decisión
 * la toma el catálogo, no el código. Esa ausencia de condicionales es lo que permite incorporar
 * canales sin tocar el núcleo, y hay una prueba de arquitectura que impide reintroducirlas.
 */
@Component
public class ProviderRegistrySender implements NotificationSenderPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderRegistrySender.class);

    private final Map<String, NotificationProvider> providersById;

    public ProviderRegistrySender(final List<NotificationProvider> providers) {
        this.providersById = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.descriptor().providerId(), Function.identity()));
        LOG.info("Proveedores registrados: {}", providersById.keySet());
    }

    @Override
    public Mono<SendOutcome> send(
            final Notification notification, final ChannelCatalogPort.ChannelRoute route) {

        final NotificationProvider provider = providersById.get(route.providerId());

        if (provider == null) {
            return Mono.just(SendOutcome.permanentFailure(
                    route.providerId(),
                    "El catálogo apunta a un proveedor que no está registrado: " + route.providerId()));
        }

        if (!provider.descriptor().supports(route.channel().value())) {
            return Mono.just(SendOutcome.permanentFailure(
                    route.providerId(),
                    "El proveedor %s no atiende el canal %s"
                            .formatted(route.providerId(), route.channel())));
        }

        return provider.send(notification);
    }
}
