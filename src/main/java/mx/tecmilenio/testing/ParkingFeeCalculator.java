package mx.tecmilenio.testing;

public class ParkingFeeCalculator {

    private static final int FREE_LIMIT_MINUTES = 15;
    private static final int FLAT_RATE_LIMIT_MINUTES = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int FLAT_RATE = 20;
    private static final int ADDITIONAL_HOUR_RATE = 15;
    private static final int MAX_NORMAL_FEE = 80;
    private static final int LOST_TICKET_FEE = 150;

    public int calculateFee(int minutes, boolean lostTicket) {
        if (lostTicket) {
            return LOST_TICKET_FEE;
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("minutes cannot be negative");
        }

        if (minutes <= FREE_LIMIT_MINUTES) {
            return 0;
        }

        if (minutes <= FLAT_RATE_LIMIT_MINUTES) {
            return FLAT_RATE;
        }

        int additionalHours = (int) Math.ceil(
                (minutes - FLAT_RATE_LIMIT_MINUTES) / (double) MINUTES_PER_HOUR);
        int fee = FLAT_RATE + additionalHours * ADDITIONAL_HOUR_RATE;

        return Math.min(fee, MAX_NORMAL_FEE);
    }
}
