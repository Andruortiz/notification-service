package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Objeto de valor ChannelType")
class ChannelTypeTest {

    @Test
    @DisplayName("normaliza a mayúsculas y recorta espacios")
    void normalizes() {
        assertThat(ChannelType.of("  correo_masivo  ").value()).isEqualTo("CORREO_MASIVO");
    }

    @Test
    @DisplayName("acepta cualquier canal bien formado, incluso uno que no existe todavía")
    void acceptsUnknownChannels() {
        assertThat(ChannelType.of("CANAL_INVENTADO_2030").value()).isEqualTo("CANAL_INVENTADO_2030");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "con espacio", "con-guion", "acentuación", "signo!"})
    @DisplayName("rechaza valores mal formados")
    void rejectsMalformed(final String candidate) {
        assertThatThrownBy(() -> ChannelType.of(candidate))
                .isInstanceOf(InvalidNotificationDataException.class);
    }

    @Test
    @DisplayName("rechaza el valor nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> ChannelType.of(null))
                .isInstanceOf(InvalidNotificationDataException.class);
    }

    @Test
    @DisplayName("dos canales con el mismo nombre son el mismo valor")
    void valueSemantics() {
        assertThat(ChannelType.of("sms")).isEqualTo(ChannelType.of("SMS"));
    }
}
