package co.edu.uco.notification.adapters.config;

import co.edu.uco.notification.shared.correlation.CorrelationId;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Asigna un identificador de correlación a cada petición y lo devuelve en la respuesta.
 *
 * <p>Si el sistema cliente envía el suyo se respeta, de modo que su registro y el nuestro puedan
 * cruzarse; si no, se genera uno. El valor viaja por el contexto reactivo y no por una variable de
 * hilo, porque en un modelo no bloqueante una misma petición cambia de hilo varias veces y
 * {@link MDC} por sí solo no la seguiría.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        final CorrelationId correlationId =
                CorrelationId.ofNullable(exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER));

        exchange.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId.value());

        return chain.filter(exchange)
                .contextWrite(Context.of(CONTEXT_KEY, correlationId.value()));
    }
}
