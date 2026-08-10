package co.edu.uco.notification.bootstrap.config;

import co.edu.uco.notification.application.port.in.DispatchNotificationUseCase;
import co.edu.uco.notification.application.port.in.GetNotificationStatusUseCase;
import co.edu.uco.notification.application.port.in.SendNotificationUseCase;
import co.edu.uco.notification.application.port.out.ChannelCatalogPort;
import co.edu.uco.notification.application.port.out.NotificationEventPublisherPort;
import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.application.usecase.DispatchNotificationService;
import co.edu.uco.notification.application.usecase.GetNotificationStatusService;
import co.edu.uco.notification.application.usecase.SendNotificationService;
import co.edu.uco.notification.core.repository.NotificationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registra los casos de uso como beans.
 *
 * <p>Esta clase es la razón por la que {@code notification-application} no depende de Spring: los
 * casos de uso son clases planas y el cableado ocurre aquí, en el único módulo que conoce el
 * framework. El coste son unas pocas líneas por caso de uso; a cambio, la capa de aplicación se
 * prueba sin levantar un contexto y podría reutilizarse fuera de Spring.
 */
@Configuration
public class UseCaseConfiguration {

    /**
     * Reloj inyectable en lugar de {@code Instant.now()} disperso por el código: permite fijar el
     * tiempo en las pruebas y hace explícita la dependencia.
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SendNotificationUseCase sendNotificationUseCase(
            final NotificationRepository repository,
            final ChannelCatalogPort channelCatalog,
            final NotificationEventPublisherPort eventPublisher,
            final Clock clock) {
        return new SendNotificationService(repository, channelCatalog, eventPublisher, clock);
    }

    @Bean
    public GetNotificationStatusUseCase getNotificationStatusUseCase(
            final NotificationRepository repository) {
        return new GetNotificationStatusService(repository);
    }

    @Bean
    public DispatchNotificationUseCase dispatchNotificationUseCase(
            final NotificationRepository repository,
            final ChannelCatalogPort channelCatalog,
            final NotificationSenderPort sender,
            final NotificationEventPublisherPort eventPublisher,
            final Clock clock) {
        return new DispatchNotificationService(repository, channelCatalog, sender, eventPublisher, clock);
    }
}
