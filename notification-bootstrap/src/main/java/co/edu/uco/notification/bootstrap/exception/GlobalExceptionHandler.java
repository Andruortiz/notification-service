package co.edu.uco.notification.bootstrap.exception;

import co.edu.uco.notification.core.exception.DomainException;
import co.edu.uco.notification.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas de error uniformes.
 *
 * <p>Usa {@link ProblemDetail}, el formato del RFC 7807, en lugar de un mapa improvisado: los
 * clientes reciben siempre la misma estructura y el código de error viaja en un campo propio, de
 * modo que puedan reaccionar sin analizar el texto del mensaje.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** El dominio ya clasificó el problema; aquí solo se le asigna su código HTTP. */
    @ExceptionHandler(DomainException.class)
    public Mono<ProblemDetail> handleDomain(final DomainException exception) {
        final HttpStatus status = statusFor(exception.errorCode());

        if (status.is5xxServerError()) {
            LOG.error("Fallo de dominio {}", exception.errorCode().code(), exception);
        } else {
            LOG.warn("Solicitud rechazada {}: {}", exception.errorCode().code(), exception.getMessage());
        }

        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(titleFor(exception.errorCode()));
        problem.setProperty("errorCode", exception.errorCode().code());
        return Mono.just(problem);
    }

    /** Fallos de validación del cuerpo de la petición, con el detalle por campo. */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidation(final WebExchangeBindException exception) {
        final Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La solicitud no cumple el contrato de la API");
        problem.setTitle("Solicitud inválida");
        problem.setProperty("errorCode", ErrorCode.INVALID_NOTIFICATION_DATA.code());
        problem.setProperty("fields", fieldErrors);
        return Mono.just(problem);
    }

    /** Cabecera obligatoria ausente o cuerpo ilegible. */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ProblemDetail> handleInput(final ServerWebInputException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getReason());
        problem.setTitle("Solicitud inválida");
        problem.setProperty("errorCode", ErrorCode.INVALID_NOTIFICATION_DATA.code());
        return Mono.just(problem);
    }

    /**
     * Red de seguridad. Devuelve un mensaje genérico a propósito: el detalle queda en el registro
     * del servidor y no se expone al cliente.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleUnexpected(final Exception exception) {
        LOG.error("Error no controlado", exception);
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado al procesar la solicitud");
        problem.setTitle("Error interno");
        problem.setProperty("errorCode", ErrorCode.UNEXPECTED_ERROR.code());
        return Mono.just(problem);
    }

    private static HttpStatus statusFor(final ErrorCode errorCode) {
        return switch (errorCode) {
            case CHANNEL_NOT_AVAILABLE, INVALID_NOTIFICATION_DATA -> HttpStatus.BAD_REQUEST;
            case NOTIFICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            case PROVIDER_REJECTED -> HttpStatus.BAD_GATEWAY;
            case PROVIDER_NOT_AVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case UNEXPECTED_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String titleFor(final ErrorCode errorCode) {
        return switch (errorCode) {
            case CHANNEL_NOT_AVAILABLE -> "Canal no disponible";
            case INVALID_NOTIFICATION_DATA -> "Solicitud inválida";
            case INVALID_STATUS_TRANSITION -> "Transición de estado no permitida";
            case NOTIFICATION_NOT_FOUND -> "Notificación no encontrada";
            case PROVIDER_NOT_AVAILABLE -> "Proveedor no disponible";
            case PROVIDER_REJECTED -> "El proveedor rechazó el envío";
            case UNEXPECTED_ERROR -> "Error interno";
        };
    }
}
