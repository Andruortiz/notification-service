package co.edu.uco.notification.bootstrap.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Punto reservado para la integración con el componente de Seguridad.
 *
 * <p>Hoy el servicio <strong>no autentica</strong>: identifica al sistema cliente por la cabecera
 * {@code X-Tenant-Id}, que cualquiera podría falsificar. Es aceptable en la línea base y en
 * desarrollo, y no lo es en un entorno real; por eso esta clase emite una advertencia visible
 * cuando arranca fuera de local, en lugar de dejar el hueco pasar inadvertido.
 *
 * <p><strong>Qué cambiará al integrar Seguridad.</strong> Se añadirá
 * {@code spring-boot-starter-oauth2-resource-server}, esta clase declarará la cadena de filtros
 * que valida el token, y el identificador del cliente pasará a leerse de sus atributos en lugar de
 * la cabecera. El único otro punto afectado será
 * {@code co.edu.uco.notification.adapters.in.rest.NotificationController}: ni el dominio ni los
 * casos de uso cambian, porque reciben el identificador ya resuelto.
 */
@Configuration
public class SecurityPlaceholderConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityPlaceholderConfiguration.class);

    private static final String LOCAL_PROFILE = "local";

    private final Environment environment;

    public SecurityPlaceholderConfiguration(final Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void warnWhenUnprotected() {
        final boolean isLocal = environment.matchesProfiles(LOCAL_PROFILE)
                || environment.getActiveProfiles().length == 0;

        if (isLocal) {
            LOG.info("Seguridad no integrada: el cliente se identifica por la cabecera X-Tenant-Id");
            return;
        }

        LOG.warn("======================================================================");
        LOG.warn(" ATENCIÓN: el servicio está expuesto SIN autenticación.");
        LOG.warn(" El cliente se identifica por la cabecera X-Tenant-Id, que es falsificable.");
        LOG.warn(" Pendiente de integrar el componente de Seguridad antes de exponerlo.");
        LOG.warn("======================================================================");
    }
}
