package co.edu.uco.notification.bootstrap.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Deja constancia al arrancar de con qué configuración quedó levantado el servicio.
 *
 * <p>Es barato y evita la pregunta más frecuente ante un comportamiento raro en un entorno
 * compartido: contra qué base de datos y qué broker está apuntando realmente la réplica.
 */
@Component
public class ApplicationStartupLogger implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStartupLogger.class);

    private final Environment environment;

    public ApplicationStartupLogger(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(final ApplicationArguments args) {
        final String[] profiles = environment.getActiveProfiles();

        LOG.info("======================================================================");
        LOG.info(" {} listo", environment.getProperty("spring.application.name", "notification-service"));
        LOG.info(" Perfiles activos : {}",
                profiles.length == 0 ? "(ninguno, se aplican los valores por defecto)" : Arrays.toString(profiles));
        LOG.info(" Puerto           : {}", environment.getProperty("server.port", "8080"));
        LOG.info(" MongoDB          : {}", maskCredentials(
                environment.getProperty("spring.data.mongodb.uri", "(no configurado)")));
        LOG.info(" RabbitMQ         : {}:{}",
                environment.getProperty("spring.rabbitmq.host", "(no configurado)"),
                environment.getProperty("spring.rabbitmq.port", ""));
        LOG.info(" Reconciliador    : {}", environment.getProperty("notification.reconciler.enabled", "true"));
        LOG.info("======================================================================");
    }

    /** Nunca se registra una cadena de conexión con su contraseña en claro. */
    private static String maskCredentials(final String uri) {
        return uri.replaceAll("://[^@/]+@", "://****@");
    }
}
