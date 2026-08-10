/**
 * Capa de aplicación: orquesta el dominio para resolver casos de uso concretos.
 *
 * <p>Declara los puertos de entrada que ofrece y los de salida que necesita, y no conoce ninguna
 * tecnología: ni REST, ni MongoDB, ni RabbitMQ. Tampoco depende del contenedor de Spring — los
 * casos de uso son clases planas que se registran como beans desde {@code notification-bootstrap}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Application",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"core", "shared"})
package co.edu.uco.notification.application;
