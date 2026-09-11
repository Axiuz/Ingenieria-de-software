package mx.tecmilenio.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingReservationPolicyTest {

    @Test
    void reservationOneHourAheadShouldBeAccepted() {
        // Arrange - Act
        boolean result = ParkingReservationPolicy.canReserve("ABC123", 1);

        // Assert
        assertTrue(result);
    }

    @Test
    void reservationSeventyTwoHoursAheadShouldBeAccepted() {
        boolean result = ParkingReservationPolicy.canReserve("ABC123", 72);

        assertTrue(result);
    }

    @Test
    void reservationInTheSameHourShouldBeRejected() {
        assertFalse(ParkingReservationPolicy.canReserve("ABC123", 0));
    }

    @Test
    void reservationBeyondSeventyTwoHoursShouldBeRejected() {
        assertFalse(ParkingReservationPolicy.canReserve("ABC123", 73));
    }

    @Test
    void nullPlateShouldBeRejected() {
        assertFalse(ParkingReservationPolicy.canReserve(null, 24));
    }

    @Test
    void blankPlateShouldBeRejected() {
        assertFalse(ParkingReservationPolicy.canReserve("   ", 24));
    }
}
