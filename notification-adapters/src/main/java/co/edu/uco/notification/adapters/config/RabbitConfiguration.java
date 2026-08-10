package co.edu.uco.notification.adapters.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Topología de mensajería del componente.
 *
 * <p>Tres colas con responsabilidades distintas:
 * <ul>
 *   <li><b>despacho</b>: cola principal, con prioridades, de la que consume el despachador.</li>
 *   <li><b>reintento</b>: retiene el mensaje durante un tiempo y lo devuelve a la principal al
 *       expirar, que es como se consigue la espera creciente sin bloquear un hilo.</li>
 *   <li><b>fallidos</b>: destino de lo que no admite reintento, para revisión manual.</li>
 * </ul>
 */
@Configuration
public class RabbitConfiguration {

    public static final String EXCHANGE = "notifications.exchange";
    public static final String DISPATCH_QUEUE = "notifications.dispatch";
    public static final String RETRY_QUEUE = "notifications.retry";
    public static final String DEAD_LETTER_QUEUE = "notifications.dlq";

    public static final String DISPATCH_ROUTING_KEY = "notification.dispatch";
    public static final String RETRY_ROUTING_KEY = "notification.retry";
    public static final String DEAD_LETTER_ROUTING_KEY = "notification.dlq";

    private static final int MAX_PRIORITY = 10;
    private static final Duration RETRY_TTL = Duration.ofSeconds(30);

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue dispatchQueue() {
        return QueueBuilder.durable(DISPATCH_QUEUE)
                .maxPriority(MAX_PRIORITY)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /** Al expirar el mensaje, la cola muerta lo devuelve a la de despacho. */
    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl((int) RETRY_TTL.toMillis())
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(DISPATCH_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding dispatchBinding(final Queue dispatchQueue, final DirectExchange notificationsExchange) {
        return BindingBuilder.bind(dispatchQueue).to(notificationsExchange).with(DISPATCH_ROUTING_KEY);
    }

    @Bean
    public Binding retryBinding(final Queue retryQueue, final DirectExchange notificationsExchange) {
        return BindingBuilder.bind(retryQueue).to(notificationsExchange).with(RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(final Queue deadLetterQueue, final DirectExchange notificationsExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(notificationsExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
