package co.edu.uco.notification.adapters.out.provider;

import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.core.aggregate.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Proveedor simulado para desarrollo y pruebas.
 *
 * <p>No llama a ningún servicio externo, de modo que el flujo completo puede ejercitarse sin
 * credenciales ni consumo de cuota, y las pruebas de extremo a extremo son deterministas.
 *
 * <p>El comportamiento se decide por la dirección del destinatario, para poder provocar cada
 * escenario a voluntad durante una demostración:
 * <ul>
 *   <li>contiene {@code fail} &rarr; fallo definitivo, la notificación se aísla.</li>
 *   <li>contiene {@code retry} &rarr; fallo recuperable, se reintenta.</li>
 *   <li>cualquier otro caso &rarr; envío aceptado.</li>
 * </ul>
 *
 * <p>Su existencia junto a un proveedor real es también la prueba de que la abstracción sirve: una
 * sola implementación nunca demuestra que la costura esté bien puesta.
 */
@Component
public class SimulatedNotificationProvider implements NotificationProvider {

    public static final String PROVIDER_ID = "simulated";

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedNotificationProvider.class);

    private static final Set<String> SUPPORTED_CHANNELS =
            Set.of("EMAIL", "SMS", "PUSH", "WHATSAPP", "WEBHOOK");

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(PROVIDER_ID, SUPPORTED_CHANNELS, true);
    }

    @Override
    public Mono<NotificationSenderPort.SendOutcome> send(final Notification notification) {
        return Mono.fromSupplier(() -> {
            final String recipient = notification.recipient().value().toLowerCase(java.util.Locale.ROOT);

            if (recipient.contains("fail")) {
                LOG.warn("[SIMULADO] Rechazo definitivo para {} por {}",
                        notification.id(), notification.recipient());
                return NotificationSenderPort.SendOutcome.permanentFailure(
                        PROVIDER_ID, "400_INVALID_DESTINATION");
            }

            if (recipient.contains("retry")) {
                LOG.warn("[SIMULADO] Fallo transitorio para {} por {}",
                        notification.id(), notification.recipient());
                return NotificationSenderPort.SendOutcome.recoverableFailure(
                        PROVIDER_ID, "429_TOO_MANY_REQUESTS");
            }

            final String messageId = "sim-" + UUID.randomUUID();
            LOG.info("[SIMULADO] Envío aceptado id={} canal={} destino={} messageId={}",
                    notification.id(), notification.channel(), notification.recipient(), messageId);
            return NotificationSenderPort.SendOutcome.accepted(PROVIDER_ID, messageId);
        });
    }
}
