package mx.tecmilenio.testing;

import java.util.logging.Logger;

public class LegacyParkingReceipt {

    private static final Logger LOGGER = Logger.getLogger(LegacyParkingReceipt.class.getName());

    private static final String LABEL = "PARKING";
    private static final String ERROR_RESULT = "ERROR";

    public String buildReceipt(String plate, int minutes, int fee) {
        if (plate == null || plate.isEmpty()) {
            return ERROR_RESULT;
        }

        if (minutes < 0 || fee < 0) {
            return ERROR_RESULT;
        }

        LOGGER.fine(() -> "Creating receipt for " + plate);

        boolean free = (fee == 0);
        String amount = free ? "FREE" : "$" + fee;

        return LABEL + " - " + plate + " - " + minutes + " min - " + amount;
    }
}
