package co.edu.uco.notification.core.service;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Política de reintentos")
class RetryPolicyTest {

    @Test
    @DisplayName("permite reintentar mientras queden intentos disponibles")
    void allowsRetryWhileAttemptsRemain() {
        final RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(10), 2.0);

        assertThat(policy.shouldRetry(0)).isTrue();
        assertThat(policy.shouldRetry(2)).isTrue();
        assertThat(policy.shouldRetry(3)).isFalse();
        assertThat(policy.shouldRetry(4)).isFalse();
    }

    @Test
    @DisplayName("la espera crece de forma exponencial")
    void backoffGrows() {
        final RetryPolicy policy = new RetryPolicy(5, Duration.ofSeconds(10), 2.0);

        assertThat(policy.backoffFor(1)).isZero();
        assertThat(policy.backoffFor(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.backoffFor(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.backoffFor(4)).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    @DisplayName("la espera se acota para no programar reintentos a días vista")
    void backoffIsCapped() {
        final RetryPolicy policy = new RetryPolicy(50, Duration.ofMinutes(10), 10.0);

        assertThat(policy.backoffFor(20)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("rechaza una configuración imposible")
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new RetryPolicy(0, Duration.ofSeconds(1), 2.0))
                .isInstanceOf(InvalidNotificationDataException.class);

        assertThatThrownBy(() -> new RetryPolicy(3, Duration.ofSeconds(-1), 2.0))
                .isInstanceOf(InvalidNotificationDataException.class);

        assertThatThrownBy(() -> new RetryPolicy(3, Duration.ofSeconds(1), 0.5))
                .isInstanceOf(InvalidNotificationDataException.class);
    }

    @Test
    @DisplayName("la política por defecto es utilizable sin configurar nada")
    void defaultPolicyIsUsable() {
        final RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertThat(policy.maxAttempts()).isEqualTo(3);
        assertThat(policy.shouldRetry(1)).isTrue();
    }
}
