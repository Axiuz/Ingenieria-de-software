package mx.tecmilenio.testing;

public class LegacyParkingReceipt {

    public String buildReceipt(String plate, int minutes, int fee) {
        String result = "";
        int ticketCounter = 0;

        if (plate == null) {
            result = "ERROR";
            return result;
        }

        if (plate == "") {
            return "ERROR";
        }

        if (minutes < 0) {
            return "ERROR";
        }

        if (fee < 0) {
            return "ERROR";
        }

        boolean free = false;
        if (fee == 0) {
            free = true;
        }

        if (free == true) {
            result = "PARKING" + " - " + plate + " - " + minutes + " min - " + "FREE";
        } else {
            result = "PARKING" + " - " + plate + " - " + minutes + " min - " + "$" + fee;
        }

        return result;
    }
}
