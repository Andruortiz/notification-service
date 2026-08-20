package co.edu.uco.notification.adapters.out.provider;

import co.edu.uco.notification.application.port.out.NotificationSenderPort;
import co.edu.uco.notification.core.aggregate.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Component
public class BrevoNotificationProvider implements NotificationProvider {

    public static final String PROVIDER_ID = "brevo";

    private final WebClient webClient;
    private final String senderEmail;
    private final String senderName;

    public BrevoNotificationProvider(
            WebClient.Builder webClientBuilder,
            @Value("${notification.providers.brevo.api-key}") String apiKey,
            @Value("${notification.providers.brevo.sender-email}") String senderEmail,
            @Value("${notification.providers.brevo.sender-name}") String senderName) {

        this.senderEmail = senderEmail;
        this.senderName = senderName;

        this.webClient = webClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
                PROVIDER_ID,
                Set.of("EMAIL"),
                true
        );
    }

    @Override
    public Mono<NotificationSenderPort.SendOutcome> send(
            final Notification notification) {

        final String subject = notification.content()
                .text("subject")
                .orElseThrow(() ->
                        new IllegalArgumentException("Falta content.subject"));

        final String body = notification.content()
                .text("body")
                .orElseThrow(() ->
                        new IllegalArgumentException("Falta content.body"));

        final String recipient = notification.recipient().value();

        final Map<String, Object> request = Map.of(
                "sender", Map.of(
                        "email", senderEmail,
                        "name", senderName
                ),
                "to", new Object[]{
                        Map.of(
                                "email", recipient
                        )
                },
                "subject", subject,
                "htmlContent", body
        );

        return webClient.post()
                .uri("/smtp/email")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println(
                                            "BREVO ERROR [" + response.statusCode() + "]: " + errorBody
                                    );

                                    return Mono.error(
                                            new IllegalArgumentException(
                                                    "Brevo rechazó el envío: " + errorBody
                                            )
                                    );
                                })
                )
                .bodyToMono(BrevoSendResponse.class)
                .map(response ->
                        NotificationSenderPort.SendOutcome.accepted(
                                PROVIDER_ID,
                                response.messageId()
                        ));
    }

    private record BrevoSendResponse(
            String messageId
    ) {
    }
}