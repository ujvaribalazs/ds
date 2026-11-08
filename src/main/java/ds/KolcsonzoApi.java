package ds;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/**
 * Autókölcsönző API: telephelyek és autók (bejelentkezés nélkül elérhető
 * részek).
 */
public class KolcsonzoApi {

    private static final String URL = "https://api.pistidevops.dev";

    public record Location(Long id, String name, String city) {
    }

    public record Car(Long id, String brandName, String model, AutoUzemanyag fuelType, String status,
            Double dailyPrice, Long currentLocationId) {
    }

    public record RegisztracioKeres(String email,
            String password,
            String firstName,
            String lastName,
            String drivingLicenseNumber) {
    }

    public static List<Location> locations() throws Exception {
        return JsonHttp.get(URL + "/api/locations", new TypeReference<>() {
        });
    }

    public static List<Car> cars() throws Exception {
        return JsonHttp.get(URL + "/api/cars", new TypeReference<>() {
        });
    }

    public static Car car(Long carId) throws Exception {
        return JsonHttp.get(URL + "/api/cars/" + carId, new TypeReference<>() {
        });
    }

    // Bejelentkezés

    public record LoginKeres(String email, String password) {
    }

    public record Bejelentkezes(String accessToken, Felhasznalo user) {
    }

    public static Bejelentkezes login(String email, String password) throws Exception {
        Bejelentkezes bejelentkezes = JsonHttp.post(URL + "/api/auth/login", new LoginKeres(email, password), null,
                new TypeReference<>() {
                });
        return bejelentkezes;

    }

    // Felhasználó regisztrálása

    public record Felhasznalo(String email) {
    }

    public static Felhasznalo register(String email,
            String password,
            String firstName,
            String lastName,
            String drivingLicenseNumber) throws Exception {
        Felhasznalo user = JsonHttp.post(URL + "/api/auth/register",
                new RegisztracioKeres(email, password, firstName, lastName, drivingLicenseNumber), null,
                new TypeReference<>() {
                });
        return user;
    }

    // Autó kölcsönzése
    public record KolcsonzesKeres(Long carId, Long pickupLocationId, Long returnLocationId, String startDate,
            String endDate) {
    }

    public record Kolcsonzes(String id, String customerId, Car car, Long pickupLocationId,
            Long returnLocationId, String startDate, String endDate, Double totalPrice, String status) {
    }

    public static Kolcsonzes reserve(Long carId, Long pickupLocationId, Long returnLocationId, String startDate,
            String endDate, String accessToken)
            throws Exception {
        Kolcsonzes reservation = JsonHttp.post(URL + "/api/reservations",
                new KolcsonzesKeres(carId, pickupLocationId,
                        returnLocationId,
                        startDate, endDate),
                accessToken,
                new TypeReference<>() {
                });
        return reservation;
    }

    // Saját foglalások lekérdezése
    public static List<Kolcsonzes> foglalasaim(String accessToken) throws Exception {
        return JsonHttp.get(URL + "/api/reservations", accessToken, new TypeReference<>() {
        });
    }

    // Foglalás lemondása (az állapota CANCELLED lesz)
    public static Kolcsonzes lemond(String reservationId, String accessToken) throws Exception {
        return JsonHttp.post(URL + "/api/reservations/" + reservationId + "/cancel", null, accessToken,
                new TypeReference<>() {
                });
    }
}
