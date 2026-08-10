package co.edu.uco.notification.adapters.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Habilita los repositorios reactivos del adaptador de persistencia.
 *
 * <p>El paquete se declara de forma explícita para que el escaneo no dependa de dónde acabe la
 * clase de arranque.
 */
@Configuration
@EnableReactiveMongoAuditing
@EnableReactiveMongoRepositories(
        basePackages = "co.edu.uco.notification.adapters.out.mongo.repository")
public class MongoConfiguration {
}
