package mx.tecmilenio.testing;

public class LegacyParkingReceipt {

    public String buildReceipt(String plate, int minutes, int fee) {

        String result = "";
        String label = "PARKING";

        if (plate == null) {
            return "ERROR";
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

        System.out.println("Creating receipt for " + plate);

        boolean free = fee == 0 ? true : false;

        if (free == true) {
            result = label + " - " + plate
                    + " - " + minutes + " min - FREE";
        } else {
            result = label + " - " + plate
                    + " - " + minutes + " min - $" + fee;
        }

        return result;
    }
}
