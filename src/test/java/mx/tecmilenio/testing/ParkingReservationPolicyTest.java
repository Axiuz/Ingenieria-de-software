package mx.tecmilenio.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingReservationPolicyTest {

    @Test
    void twentyFourHoursBeforeShouldRefundEverything() {
        // Arrange
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        // Act
        int result = policy.refundPercentage(24);

        // Assert
        assertEquals(100, result);
    }

    @Test
    void moreThanTwentyFourHoursBeforeShouldRefundEverything() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        int result = policy.refundPercentage(48);

        assertEquals(100, result);
    }

    @Test
    void twentyThreeHoursBeforeShouldRefundHalf() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        int result = policy.refundPercentage(23);

        assertEquals(50, result);
    }

    @Test
    void twoHoursBeforeShouldRefundHalf() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        int result = policy.refundPercentage(2);

        assertEquals(50, result);
    }

    @Test
    void oneHourBeforeShouldNotRefundAnything() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        int result = policy.refundPercentage(1);

        assertEquals(0, result);
    }

    @Test
    void startingRightNowShouldNotRefundAnything() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        int result = policy.refundPercentage(0);

        assertEquals(0, result);
    }

    @Test
    void negativeHoursShouldBeRejected() {
        ParkingReservationPolicy policy = new ParkingReservationPolicy();

        assertThrows(IllegalArgumentException.class, () -> policy.refundPercentage(-1));
    }
}
