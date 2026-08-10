package co.edu.uco.notification.core.service;

import co.edu.uco.notification.core.exception.InvalidStatusTransitionException;
import co.edu.uco.notification.core.valueobject.NotificationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static co.edu.uco.notification.core.valueobject.NotificationStatus.DELIVERED;
import static co.edu.uco.notification.core.valueobject.NotificationStatus.DISCARDED;
import static co.edu.uco.notification.core.valueobject.NotificationStatus.FAILED;
import static co.edu.uco.notification.core.valueobject.NotificationStatus.IN_PROCESS;
import static co.edu.uco.notification.core.valueobject.NotificationStatus.PENDING;
import static co.edu.uco.notification.core.valueobject.NotificationStatus.RECOVERABLE;

/**
 * Gobierna qué transiciones de estado son legítimas.
 *
 * <p>Tener la tabla en un solo lugar evita que cada punto del código improvise su propia regla,
 * que es como aparecen las notificaciones en estados imposibles.
 */
public final class StatusTransitionPolicy {

    private static final Map<NotificationStatus, Set<NotificationStatus>> ALLOWED =
            new EnumMap<>(NotificationStatus.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(IN_PROCESS, DISCARDED));
        ALLOWED.put(IN_PROCESS, EnumSet.of(DELIVERED, RECOVERABLE, FAILED));
        ALLOWED.put(RECOVERABLE, EnumSet.of(PENDING, IN_PROCESS, FAILED, DISCARDED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED.put(DISCARDED, EnumSet.noneOf(NotificationStatus.class));
    }

    private StatusTransitionPolicy() {
    }

    public static boolean isAllowed(final NotificationStatus from, final NotificationStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(NotificationStatus.class)).contains(to);
    }

    /**
     * Verifica la transición y falla si no está permitida.
     *
     * @throws InvalidStatusTransitionException cuando la transición no está contemplada
     */
    public static void verify(final NotificationStatus from, final NotificationStatus to) {
        if (!isAllowed(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
