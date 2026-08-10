package co.edu.uco.notification.adapters.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Catálogo de canales declarado como configuración.
 *
 * <p>Es el mecanismo que hace universal al componente: dar de alta un canal consiste en añadir una
 * entrada bajo {@code notification.catalog.channels}, sin tocar ni recompilar el núcleo. La clave
 * del mapa es el nombre del canal, que por eso no puede ser un enum en el dominio.
 *
 * <p>Cuando exista el componente de Parámetros, este mismo contenido llegará de él y esta clase se
 * limitará a servir de respaldo local.
 *
 * @param channels definición por canal, indexada por su nombre
 */
@ConfigurationProperties(prefix = "notification.catalog")
public record ChannelCatalogProperties(Map<String, ChannelDefinition> channels) {

    public ChannelCatalogProperties {
        channels = channels == null ? Map.of() : Map.copyOf(channels);
    }

    /**
     * Configuración de un canal.
     *
     * @param providerId         proveedor de mayor preferencia que lo atiende
     * @param enabled            permite deshabilitarlo sin borrar su definición
     * @param maxAttempts        intentos máximos, incluido el primero
     * @param initialBackoff     espera antes del primer reintento
     * @param backoffMultiplier  factor de crecimiento de la espera
     */
    public record ChannelDefinition(
            String providerId,
            boolean enabled,
            Integer maxAttempts,
            Duration initialBackoff,
            Double backoffMultiplier) {

        public int maxAttemptsOrDefault() {
            return maxAttempts == null ? 3 : maxAttempts;
        }

        public Duration initialBackoffOrDefault() {
            return initialBackoff == null ? Duration.ofSeconds(30) : initialBackoff;
        }

        public double backoffMultiplierOrDefault() {
            return backoffMultiplier == null ? 2.0 : backoffMultiplier;
        }
    }
}
