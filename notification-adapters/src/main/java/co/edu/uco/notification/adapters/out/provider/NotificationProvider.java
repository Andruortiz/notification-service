package co.edu.uco.notification.adapters.out.provider;

import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.core.aggregate.Notification;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Contrato que debe cumplir todo proveedor de notificación integrado.
 *
 * <p>Es el punto de extensión del componente: incorporar un proveedor consiste en escribir una
 * clase que implemente esta interfaz y registrarla como bean. El registro la descubre sola, sin
 * que haya que modificar el enrutador ni ninguna otra pieza.
 *
 * <p>El proveedor es responsable de <em>clasificar</em> el fallo, porque es quien conoce los
 * códigos de su API. El caso de uso solo decide qué hacer con esa clasificación.
 */
public interface NotificationProvider {

    /** Metadatos que permiten al registro decidir si este proveedor puede atender un envío. */
    ProviderDescriptor descriptor();

    /** Ejecuta el envío y devuelve un resultado ya normalizado. */
    Mono<NotificationSenderPort.SendOutcome> send(Notification notification);

    /**
     * Capacidades declaradas por el proveedor.
     *
     * @param providerId        identificador con el que se le referencia en el catálogo
     * @param supportedChannels canales que sabe atender
     * @param enabled           permite retirarlo temporalmente sin desplegar
     */
    public record ProviderDescriptor(
            String providerId,
            Set<String> supportedChannels,
            boolean enabled) {

        public ProviderDescriptor {
            supportedChannels = supportedChannels == null
                    ? Set.of()
                    : Set.copyOf(supportedChannels);
        }

        public boolean supports(final String channel) {
            return enabled && supportedChannels.contains(channel);
        }
    }
}
