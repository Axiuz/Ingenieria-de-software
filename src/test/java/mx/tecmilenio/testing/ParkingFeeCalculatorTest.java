package mx.tecmilenio.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingFeeCalculatorTest {

    @Test
    void fifteenMinutesShouldBeFree() {
        // Arrange
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(15, false);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void sixtyOneMinutesShouldChargeOneStartedAdditionalHour() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        int result = calculator.calculateFee(61, false);

        assertEquals(35, result);
    }

    @Test
    void normalFeeShouldNotBeGreaterThanEighty() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        int result = calculator.calculateFee(600, false);

        assertEquals(80, result);
    }

    @Test
    void lostTicketShouldCostOneHundredFifty() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        int result = calculator.calculateFee(10, true);

        assertEquals(150, result);
    }

    @Test
    void negativeMinutesShouldBeRejected() {
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateFee(-1, false)
        );
    }

    // ------------------------------------------------------------------
    // Pruebas nuevas (Actividad 4)
    // ------------------------------------------------------------------

    @Test
    void zeroMinutesShouldBeFree() {
        // Arrange
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(0, false);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void sixteenMinutesShouldChargeFlatRate() {
        // Arrange: primer minuto fuera de la tolerancia gratuita
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(16, false);

        // Assert
        assertEquals(20, result);
    }

    @Test
    void sixtyMinutesShouldStillChargeOnlyFlatRate() {
        // Arrange: último minuto de la tarifa plana
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(60, false);

        // Assert
        assertEquals(20, result);
    }

    @Test
    void partialAdditionalHourShouldBeChargedAsAWholeHour() {
        // Arrange: 121 min = 1 hora adicional completa + 1 minuto iniciado
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(121, false);

        // Assert
        assertEquals(50, result);
    }

    @Test
    void feeShouldBeCappedAtEightyOnTheFirstMinuteThatExceedsTheCap() {
        // Arrange: 241 min darían 95 sin tope; el tope debe recortar a 80
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(241, false);

        // Assert
        assertEquals(80, result);
    }

    @Test
    void lostTicketShouldTakePrecedenceOverNegativeMinutes() {
        // Arrange: boleto perdido combinado con un dato inválido de minutos
        ParkingFeeCalculator calculator = new ParkingFeeCalculator();

        // Act
        int result = calculator.calculateFee(-30, true);

        // Assert
        assertEquals(150, result);
    }
}
