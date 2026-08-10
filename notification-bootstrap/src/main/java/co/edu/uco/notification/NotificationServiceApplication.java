package co.edu.uco.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/**
 * Punto de arranque del componente de notificaciones.
 *
 * <p>La clase vive en el paquete raíz {@code co.edu.uco.notification} de forma deliberada: el
 * escaneo de componentes parte de aquí, de modo que alcanza los adaptadores, y Spring Modulith
 * toma este paquete como raíz para descubrir los módulos de la aplicación.
 */
@Modulithic(systemName = "Notification Service", sharedModules = "shared")
@SpringBootApplication
@ConfigurationPropertiesScan("co.edu.uco.notification")
public class NotificationServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
