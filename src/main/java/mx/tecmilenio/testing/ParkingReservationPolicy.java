package mx.tecmilenio.testing;

/**
 * Reto TDD: la política se escribió después de las pruebas
 * (rojo -> verde -> refactor) en {@code ParkingReservationPolicyTest}.
 */
public class ParkingReservationPolicy {

    private static final int MIN_HOURS_AHEAD = 1;
    private static final int MAX_HOURS_AHEAD = 72;

    private ParkingReservationPolicy() {
        // Clase de utilidad: no se instancia.
    }

    public static boolean canReserve(String plate, int hoursAhead) {
        if (plate == null || plate.isBlank()) {
            return false;
        }

        return hoursAhead >= MIN_HOURS_AHEAD && hoursAhead <= MAX_HOURS_AHEAD;
    }
}
