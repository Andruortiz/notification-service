package co.edu.uco.notification.application.port.out;

import co.edu.uco.notification.core.service.RetryPolicy;
import co.edu.uco.notification.core.valueobject.ChannelType;
import reactor.core.publisher.Mono;

/**
 * Acceso al catálogo de canales: qué canales existen, qué proveedor los atiende y con qué
 * política de reintentos.
 *
 * <p>Es la pieza que sostiene la extensibilidad. El caso de uso pregunta al catálogo en lugar de
 * decidir con condicionales sobre el nombre del canal, de modo que dar de alta un canal es añadir
 * una entrada al catálogo y no modificar el núcleo.
 *
 * <p>Hoy lo implementa un adaptador en memoria. Cuando exista el componente de Parámetros, su
 * cliente ocupará este mismo puerto sin que cambie nada de este módulo.
 */
public interface ChannelCatalogPort {

    /** Devuelve la ruta activa del canal, o vacío si no existe o está deshabilitado. */
    Mono<ChannelRoute> findActiveRoute(ChannelType channel);

    /**
     * Ruta resuelta para un canal.
     *
     * @param channel     canal atendido
     * @param providerId  proveedor de mayor preferencia habilitado para el canal
     * @param retryPolicy política de reintentos aplicable
     */
    record ChannelRoute(ChannelType channel, String providerId, RetryPolicy retryPolicy) {
    }
}
