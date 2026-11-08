package ds;

// Hibás HTTP válasz: a státuszkód és a szerver által küldött hibaüzenet.
public class HttpHiba extends RuntimeException {

    private final int statusz;
    private final String uzenet;

    public HttpHiba(int statusz, String uzenet) {
        super("HTTP " + statusz + ": " + uzenet);
        this.statusz = statusz;
        this.uzenet = uzenet;
    }

    public int statusz() {
        return statusz;
    }

    // A szerver hibaüzenete (a JSON `message` mezője, ha van), különben a teljes
    // válasz.
    public String uzenet() {
        return uzenet;
    }
}
