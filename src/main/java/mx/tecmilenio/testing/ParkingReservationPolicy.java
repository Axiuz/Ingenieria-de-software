package mx.tecmilenio.testing;

/**
 * Reto TDD (Parte 14): la clase se escribió después de sus pruebas
 * (rojo -> verde -> refactor) en {@code ParkingReservationPolicyTest}.
 */
public class ParkingReservationPolicy {

    private static final int FULL_REFUND_HOURS = 24;
    private static final int PARTIAL_REFUND_HOURS = 2;
    private static final int FULL_REFUND_PERCENTAGE = 100;
    private static final int PARTIAL_REFUND_PERCENTAGE = 50;
    private static final int NO_REFUND_PERCENTAGE = 0;

    public int refundPercentage(int hoursBeforeStart) {
        if (hoursBeforeStart < 0) {
            throw new IllegalArgumentException("hoursBeforeStart cannot be negative");
        }

        if (hoursBeforeStart >= FULL_REFUND_HOURS) {
            return FULL_REFUND_PERCENTAGE;
        }

        if (hoursBeforeStart >= PARTIAL_REFUND_HOURS) {
            return PARTIAL_REFUND_PERCENTAGE;
        }

        return NO_REFUND_PERCENTAGE;
    }
}
