package ds;

import java.util.List;
import java.util.Scanner;

import ds.KolcsonzoApi.Bejelentkezes;
import ds.KolcsonzoApi.Car;
import ds.KolcsonzoApi.Kolcsonzes;
import ds.KolcsonzoApi.Location;
import ds.KeszletApi.Station;
import ds.KeszletApi.FuelStock;

public class Konzol {
    static Scanner in = new Scanner(System.in);

    private static String kerdez(String kerdes) {
        System.out.println(kerdes + ": ");
        return in.nextLine().trim();
    }

    public static Bejelentkezes belepes() throws Exception {
        String valasz = kerdez("Van már fiókod? (i/n)");
        if (valasz.equals("n")) {
            KolcsonzoApi.register(kerdez("Email"), kerdez("Jelszó"), kerdez("Keresztnév"),
                    kerdez("Vezetéknév"), kerdez("Jogosítványszám"));
        }
        var bejelentkezes = KolcsonzoApi.login(kerdez("Email"), kerdez("Jelszó"));
        System.out.println("Bejelentkezve: " + bejelentkezes.user().email());
        return bejelentkezes;
    }

    public static void listazzKolcsonzot() throws Exception {

        System.out.println("=== Autókölcsönző telephelyek és autók ===");
        List<Car> cars = KolcsonzoApi.cars();
        for (Location l : KolcsonzoApi.locations()) {
            System.out.println(l.city() + ", " + l.name());
            for (Car c : cars) {
                if (l.id().equals(c.currentLocationId())) {
                    System.out.println("   " + "azonosító: " + c.id() + " " + c.brandName() + " " + c.model()
                            + " – " + c.dailyPrice() + "/nap – " + c.status());
                }
            }
        }
    }

    public static void listazzKutakat() throws Exception {
        System.out.println("=== Benzinkutak és üzemanyagkészlet ===");
        for (Station s : KeszletApi.stations()) {
            System.out.println(s.city() + ", " + s.name());
            for (FuelStock f : KeszletApi.stocks(s.id())) {
                System.out.println("   " + f.fuelType() + ": " + f.availableLiters()
                        + " / " + f.capacityLiters() + " liter szabad");
            }
        }
    }

    public static Kolcsonzes foglaljAutot(String accessToken) throws Exception {
        Car car;
        Long carId;
        while (true) {
            try {
                carId = Long.valueOf(kerdez("kölcsönzendő autó azonosítója"));
                car = KolcsonzoApi.car(carId);
                if (car.status().equals("AVAILABLE")) {
                    break;
                }
                System.out.println("Ez az autó most nem foglalható (" + car.status() + ").");
            } catch (Exception e) {
                System.out.println("Nincs ilyen autó: " + e.getMessage());
            }
            System.out.println("Válassz másikat!");
        }

        List<Location> helyek = KolcsonzoApi.locations();
        for (Location l : helyek) {
            System.out.println(" Célállomás azonosító: " + l.id() + " " + l.city() + ", " + l.name());
        }
        Long celId;
        while (true) {
            Long valasztott = Long.valueOf(kerdez("hová utazol? (LocationId)"));
            boolean letezik = helyek.stream().anyMatch(l -> l.id().equals(valasztott));
            if (letezik) {
                celId = valasztott;
                break;
            }
            System.out.println("Nincs ilyen telephely, válassz a listából!");
        }

        var kolcsonzes = KolcsonzoApi.reserve(carId,
                car.currentLocationId(),
                celId,
                kerdez("mikor indulsz? (yyyy-mm-dd)") + "T10:00:00",
                kerdez("mikor érkezel? (yyyy-mm-dd)") + "T10:00:00",
                accessToken);
        System.out.println(kolcsonzes);
        return kolcsonzes;
    }

    public static void listazzFoglalasaimat(String accessToken) throws Exception {
        System.out.println("=== Foglalásaim ===");
        List<Kolcsonzes> foglalasok = KolcsonzoApi.foglalasaim(accessToken);
        if (foglalasok.isEmpty()) {
            System.out.println("Nincs foglalásod.");
        }
        for (Kolcsonzes f : foglalasok) {
            System.out.println("   foglalás " + f.id() + ": " + f.car().brandName() + " " + f.car().model()
                    + ", " + f.startDate() + " – " + f.endDate()
                    + ", telephely " + f.pickupLocationId() + " → " + f.returnLocationId()
                    + ", " + f.totalPrice() + ", " + f.status());
        }
    }
}
