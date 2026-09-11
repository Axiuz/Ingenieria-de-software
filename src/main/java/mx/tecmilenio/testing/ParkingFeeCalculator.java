package mx.tecmilenio.testing;

public class ParkingFeeCalculator {

    public int calculateFee(int minutes, boolean lostTicket) {
        if (lostTicket) {
            return 150;
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("minutes cannot be negative");
        }

        if (minutes <= 15) {
            return 0;
        }

        if (minutes <= 60) {
            return 20;
        }

        int additionalHours = (int) Math.ceil((minutes - 60) / 60.0);
        int fee = 20 + additionalHours * 15;

        return Math.min(fee, 80);
    }
}
