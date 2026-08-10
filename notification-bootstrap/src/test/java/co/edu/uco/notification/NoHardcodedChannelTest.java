package co.edu.uco.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Impide que el nombre de un canal o de un proveedor se cuele en el núcleo.
 *
 * <p>Es la prueba que sostiene la universalidad del componente. La arquitectura hexagonal separa
 * el dominio de la infraestructura, pero no impide por sí sola que alguien escriba
 * {@code if (canal.equals("EMAIL"))} dentro de una regla de negocio. Basta una de esas condiciones
 * para que cada canal nuevo vuelva a exigir tocar el núcleo, que es justo lo que el diseño quiere
 * evitar.
 *
 * <p>Se analiza el código fuente y no el compilado porque los literales de cadena no sobreviven de
 * una forma que ArchUnit pueda inspeccionar dentro de los cuerpos de método.
 */
class NoHardcodedChannelTest {

    /** Rutas de los módulos cuyo código debe permanecer agnóstico. */
    private static final List<Path> PROTECTED_SOURCES = List.of(
            Path.of("..", "notification-core", "src", "main", "java"),
            Path.of("..", "notification-application", "src", "main", "java"));

    private static final Set<String> FORBIDDEN_TOKENS = Set.of(
            // canales
            "EMAIL", "SMS", "PUSH", "PUSH_MOBILE", "PUSH_WEB", "WHATSAPP", "WEBHOOK",
            "WEBHOOK_OUT", "TELEGRAM", "VOICE_CALL", "CORREO",
            // proveedores
            "BREVO", "TWILIO", "SENDGRID", "INFOBIP", "SIMULATED", "AWS_SNS", "AWS_SES");

    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Test
    @DisplayName("Ni el dominio ni la aplicación nombran canales o proveedores concretos")
    void coreAndApplicationStayChannelAgnostic() throws IOException {
        final List<String> violations = new ArrayList<>();

        for (final Path sourceRoot : PROTECTED_SOURCES) {
            assertThat(sourceRoot)
                    .as("no se encontró el código fuente en %s; revise el directorio de trabajo", sourceRoot)
                    .exists();

            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> collectViolations(path, violations));
            }
        }

        assertThat(violations)
                .as("el núcleo debe permanecer agnóstico del canal: estos literales deben vivir en "
                        + "el catálogo o en un adaptador, no aquí")
                .isEmpty();
    }

    private static void collectViolations(final Path file, final List<String> violations) {
        final String source;
        try {
            source = Files.readString(file);
        } catch (final IOException readFailure) {
            throw new IllegalStateException("No se pudo leer " + file, readFailure);
        }

        final Matcher matcher = STRING_LITERAL.matcher(stripComments(source));
        while (matcher.find()) {
            final String literal = matcher.group(1).trim().toUpperCase(Locale.ROOT);
            if (FORBIDDEN_TOKENS.contains(literal)) {
                violations.add("%s -> \"%s\"".formatted(file.getFileName(), matcher.group(1)));
            }
        }
    }

    /** Los comentarios sí pueden nombrar canales: son explicaciones, no comportamiento. */
    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
