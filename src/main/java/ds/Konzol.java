package ds;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import ds.KeszletApi.FuelStock;
import ds.KeszletApi.KeszletMuvelet;
import ds.KeszletApi.Station;
import ds.KolcsonzoApi.Bejelentkezes;
import ds.KolcsonzoApi.Car;
import ds.KolcsonzoApi.Kolcsonzes;
import ds.KolcsonzoApi.Location;
import ds.UzemanyagNaplo.Bejegyzes;
import ds.UzemanyagSzolgaltatas.KutTartaly;

//Konzolos felület: autókölcsönzés, és a kölcsönzött autókhoz üzemanyag-foglalás.
//Az üzleti logika a KolcsonzesSzolgaltatas és az UzemanyagSzolgaltatas osztályban vannak.
public class Konzol {
    static Scanner in = new Scanner(System.in);

    private static Bejelentkezes bejelentkezes;

    private static String kerdez(String kerdes) {
        System.out.print(kerdes + ": ");
        return in.nextLine().trim();
    }

    private static boolean igen(String kerdes) {
        return kerdez(kerdes + " (i/n)").equalsIgnoreCase("i");
    }

    private static String token() {
        return bejelentkezes.accessToken();
    }

    // Főmenü

    public static void futtat() throws Exception {
        System.out.println("=== Autókölcsönző ===");
        if (kerdez("Van már fiókod? (i/n)").equalsIgnoreCase("n")) {
            regisztral();
        }
        bejelentkezes = bejelentkezik();

        while (true) {
            System.out.println();
            System.out.println("--- Főmenü (" + bejelentkezes.user().email() + ") ---");
            System.out.println("  1  Autók listája");
            System.out.println("  2  Autó kölcsönzése");
            System.out.println("  3  Foglalásaim és üzemanyag");
            System.out.println("  4  Üzemanyag foglalása egy kölcsönzött autóhoz");
            System.out.println("  5  Kölcsönzés lemondása");
            System.out.println("  0  Kilépés");
            String valasz = kerdez("Válassz");
            System.out.println();
            try {
                switch (valasz) {
                    case "1" -> listazzAutokat();
                    case "2" -> kolcsonozz();
                    case "3" -> listazdFoglalasaimat();
                    case "4" -> foglaljUzemanyagotKolcsonzeshez();
                    case "5" -> mondjLeKolcsonzest();
                    case "0" -> {
                        System.out.println("Viszlát!");
                        return;
                    }
                    default -> System.out.println("Nincs ilyen menüpont.");
                }
            } catch (HttpHiba e) {
                if (e.statusz() == 401) {
                    // a kölcsönző tokenje csak 15 percig érvényes
                    System.out.println("Lejárt a bejelentkezésed, lépj be újra.");
                    bejelentkezes = bejelentkezik();
                } else {
                    System.out.println("Hiba: " + e.uzenet());
                }
            } catch (Exception e) {
                System.out.println("Nem sikerült elérni a szervert: " + e.getMessage());
            }
        }
    }

    // Belépés

    private static void regisztral() throws Exception {
        System.out.println("=== Regisztráció ===");
        while (true) {
            try {
                var user = KolcsonzesSzolgaltatas.regisztral(kerdez("Email"), kerdez("Jelszó"), kerdez("Keresztnév"),
                        kerdez("Vezetéknév"), kerdez("Jogosítványszám"));
                System.out.println("Sikeres regisztráció: " + user.email());
                return;
            } catch (HttpHiba e) {
                System.out.println("A regisztráció nem sikerült: " + e.uzenet() + " Próbáld újra!");
            }
        }
    }

    private static Bejelentkezes bejelentkezik() throws Exception {
        System.out.println("=== Bejelentkezés ===");
        while (true) {
            try {
                var b = KolcsonzesSzolgaltatas.bejelentkezik(kerdez("Email"), kerdez("Jelszó"));
                System.out.println("Bejelentkezve: " + b.user().email());
                return b;
            } catch (HttpHiba e) {
                System.out.println("A bejelentkezés nem sikerült: " + e.uzenet() + " Próbáld újra!");
            }
        }
    }

    // 1. Autók

    private static void listazzAutokat() throws Exception {
        System.out.println("=== Autók telephelyenként ===");
        List<Car> autok = KolcsonzesSzolgaltatas.autok();
        for (Location l : KolcsonzesSzolgaltatas.telephelyek()) {
            System.out.println(l.city() + ", " + l.name());
            for (Car c : autok) {
                if (l.id().equals(c.currentLocationId())) {
                    System.out.println("   " + autoSor(c));
                }
            }
        }
    }

    private static String autoSor(Car c) {
        String allapot = KolcsonzesSzolgaltatas.kolcsonozheto(c) ? "" : "  – nem kölcsönözhető (" + c.status() + ")";
        return "#" + c.id() + "  " + c.brandName() + " " + c.model() + " (" + c.fuelType() + "), "
                + c.dailyPrice() + "/nap" + allapot;
    }

    // Kölcsönzés

    private static void kolcsonozz() throws Exception {
        listazzAutokat();
        System.out.println();
        Car car = valasszAutot();
        if (car == null) {
            return;
        }

        List<Location> helyek = KolcsonzesSzolgaltatas.telephelyek();
        System.out.println("Leadás helye:");
        for (Location l : helyek) {
            System.out.println("   #" + l.id() + "  " + l.city() + ", " + l.name());
        }
        Long celId = valasszHelyet(helyek);

        // Az AVAILABLE csak azt jelenti, hogy az autó most szabad, de a kért napokra
        // már lehet foglalása. Ilyenkor más dátum vagy más autó választható.
        while (true) {
            LocalDate indul = datum("Mikor indulsz? (éééé-hh-nn)");
            LocalDate erkez = datum("Mikor érkezel? (éééé-hh-nn)");
            if (!erkez.isAfter(indul)) {
                System.out.println("Az érkezés legyen későbbi, mint az indulás!");
                continue;
            }
            try {
                Kolcsonzes k = KolcsonzesSzolgaltatas.kolcsonoz(car, celId, indul, erkez, token());
                System.out.println("Lefoglalva: " + kolcsonzesSor(k, helyNevek(helyek)));
                if (igen("Szeretnél ehhez az autóhoz üzemanyagot foglalni?")) {
                    foglaljUzemanyagot(k);
                }
                return;
            } catch (HttpHiba e) {
                if (e.statusz() == 401) {
                    throw e;
                }
                System.out.println("A foglalás nem sikerült: " + e.uzenet());
            }
            String tovabb = kerdez("Más dátummal (d), másik autóval (a), vagy mégsem (m)?");
            if (tovabb.equalsIgnoreCase("m")) {
                return;
            }
            if (tovabb.equalsIgnoreCase("a")) {
                car = valasszAutot();
                if (car == null) {
                    return;
                }
            }
        }
    }

    private static Car valasszAutot() {
        while (true) {
            String id = kerdez("Melyik autót kölcsönzöd? (azonosító, üresen hagyd, ha mégsem!)");
            if (id.isEmpty()) {
                return null;
            }
            try {
                Car car = KolcsonzesSzolgaltatas.auto(Long.parseLong(id));
                if (KolcsonzesSzolgaltatas.kolcsonozheto(car)) {
                    return car;
                }
                System.out.println("Ez az autó most nem kölcsönözhető (" + car.status() + ").");
            } catch (Exception e) {
                System.out.println("Nincs ilyen autó.");
            }
        }
    }

    private static Long valasszHelyet(List<Location> helyek) {
        while (true) {
            try {
                Long valasztott = Long.valueOf(kerdez("Hová viszed vissza? (azonosító)"));
                if (helyek.stream().anyMatch(l -> l.id().equals(valasztott))) {
                    return valasztott;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("Nincs ilyen telephely, válassz a listából!");
        }
    }

    private static LocalDate datum(String kerdes) {
        while (true) {
            try {
                return LocalDate.parse(kerdez(kerdes));
            } catch (DateTimeParseException e) {
                System.out.println("Ilyen formában add meg: 2027-03-01");
            }
        }
    }

    // 3. Foglalások és üzemanyag

    private static void listazdFoglalasaimat() throws Exception {
        System.out.println("=== Foglalásaim ===");
        List<Kolcsonzes> mind = KolcsonzesSzolgaltatas.foglalasaim(token());
        Map<Long, String> helyek = helyNevek(KolcsonzesSzolgaltatas.telephelyek());
        Map<String, List<Bejegyzes>> uzemanyagok = UzemanyagSzolgaltatas.kolcsonzesenkent();

        // A lemondott vagy lezárt kölcsönzések csak akkor látszanak, ha van hozzájuk
        // üzemanyag,
        // mert az üzemanyag-foglalás akkor is megmarad.
        int rejtett = 0;
        boolean volt = false;
        for (Kolcsonzes k : mind) {
            List<Bejegyzes> uzemanyag = uzemanyagok.getOrDefault(k.id(), List.of());
            if (!KolcsonzesSzolgaltatas.aktiv(k) && uzemanyag.isEmpty()) {
                rejtett++;
                continue;
            }
            volt = true;
            System.out.println(kolcsonzesSor(k, helyek));
            for (Bejegyzes b : uzemanyag) {
                System.out.println("        üzemanyag: " + uzemanyagSor(b));
            }
        }
        if (!volt) {
            System.out.println("Nincs aktív foglalásod.");
        }
        if (rejtett > 0) {
            System.out.println("(" + rejtett + " lemondott vagy lezárt foglalásod nem látszik.)");
        }
    }

    private static String kolcsonzesSor(Kolcsonzes k, Map<Long, String> helyek) {
        return "#" + k.id() + "  " + k.car().brandName() + " " + k.car().model() + " (" + k.car().fuelType() + "), "
                + nap(k.startDate()) + " – " + nap(k.endDate()) + ", "
                + helyek.getOrDefault(k.pickupLocationId(), "?") + " → "
                + helyek.getOrDefault(k.returnLocationId(), "?") + ", "
                + k.totalPrice() + ", " + kolcsonzesAllapot(k.status());
    }

    private static String uzemanyagSor(Bejegyzes b) {
        String allapot;
        try {
            KeszletMuvelet f = UzemanyagSzolgaltatas.allapot(b);
            allapot = switch (f.status()) {
                case DONE -> "lefoglalva";
                case FAILED -> "sikertelen: " + f.failureReason();
                default -> "feldolgozás alatt";
            };
        } catch (Exception e) {
            allapot = "az állapota most nem kérdezhető le";
        }
        return b.liter() + " liter " + b.uzemanyag() + ", " + b.varos() + ", " + b.kut() + " – " + allapot;
    }

    private static Map<Long, String> helyNevek(List<Location> helyek) {
        return helyek.stream().collect(Collectors.toMap(Location::id, Location::name));
    }

    private static String nap(String idopont) {
        return idopont == null || idopont.length() < 10 ? String.valueOf(idopont) : idopont.substring(0, 10);
    }

    private static String kolcsonzesAllapot(String status) {
        return switch (status) {
            case "PENDING" -> "függőben";
            case "CONFIRMED" -> "megerősítve";
            case "ACTIVE" -> "folyamatban";
            case "COMPLETED" -> "lezárva";
            case "CANCELLED" -> "lemondva";
            default -> status;
        };
    }

    // Az aktív kölcsönzések közül választ. Null, ha nincs ilyen kölcsönzés, vagy a
    // felhasználó üresen hagyja.
    private static Kolcsonzes valasszKolcsonzest(String kerdes) throws Exception {
        List<Kolcsonzes> aktivak = KolcsonzesSzolgaltatas.aktivKolcsonzesek(token());
        if (aktivak.isEmpty()) {
            System.out.println("Nincs aktív kölcsönzésed.");
            return null;
        }
        Map<Long, String> helyek = helyNevek(KolcsonzesSzolgaltatas.telephelyek());
        for (Kolcsonzes k : aktivak) {
            System.out.println(kolcsonzesSor(k, helyek));
        }
        while (true) {
            String id = kerdez(kerdes + " (azonosító, hagyd üresen, ha mégsem!)");
            if (id.isEmpty()) {
                return null;
            }
            for (Kolcsonzes k : aktivak) {
                if (k.id().equals(id)) {
                    return k;
                }
            }
            System.out.println("Nincs ilyen aktív kölcsönzésed, válassz a listából!");
        }
    }

    // 4. Üzemanyag-foglalás

    private static void foglaljUzemanyagotKolcsonzeshez() throws Exception {
        Kolcsonzes k = valasszKolcsonzest("Melyik autóhoz foglalsz üzemanyagot?");
        if (k != null) {
            foglaljUzemanyagot(k);
        }
    }

    private static void foglaljUzemanyagot(Kolcsonzes kolcsonzes) throws Exception {
        AutoUzemanyag uzemanyag = kolcsonzes.car().fuelType();
        if (uzemanyag == null) {
            System.out.println("Ennek az autónak az üzemanyagához nincs kút.");
            return;
        }

        // 1. A megfelelő tartályok kutanként
        List<KutTartaly> mind = UzemanyagSzolgaltatas.illoTartalyok(uzemanyag);
        System.out.println("=== " + uzemanyag + " autóhoz való kutak ===");
        Station elozo = null;
        for (KutTartaly j : mind) {
            if (elozo == null || !elozo.id().equals(j.kut().id())) {
                System.out.println(j.kut().city() + ", " + j.kut().name());
                elozo = j.kut();
            }
            System.out.println("   " + j.tartaly().fuelType() + ": " + j.tartaly().availableLiters() + " liter szabad");
        }

        // 2. Város alapján szűkítünk
        List<KutTartaly> jeloltek;
        while (true) {
            String varos = kerdez("Melyik városban tankolnál? (üresen hagyd, ha mégsem tankolsz!)");
            if (varos.isEmpty()) {
                return;
            }
            jeloltek = UzemanyagSzolgaltatas.varosban(mind, varos);
            if (!jeloltek.isEmpty()) {
                break;
            }
            System.out.println("Ebben a városban nincs megfelelő kút, válassz a listából!");
        }

        // 3. Ha több tartály is jó (pl. 95-ös és 100-as benzin), a felhasználó választ
        KutTartaly valasztott = jeloltek.get(0);
        if (jeloltek.size() > 1) {
            for (int i = 0; i < jeloltek.size(); i++) {
                KutTartaly j = jeloltek.get(i);
                System.out.println("   " + (i + 1) + ") " + j.kut().name() + " – " + j.tartaly().fuelType()
                        + ": " + j.tartaly().availableLiters() + " liter szabad");
            }
            while (true) {
                try {
                    valasztott = jeloltek.get(Integer.parseInt(kerdez("Melyiket választod? (sorszám)")) - 1);
                    break;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    System.out.println("Írd be a lista egyik sorszámát!");
                }
            }
        }
        FuelStock tartaly = valasztott.tartaly();
        if (tartaly.availableLiters() <= 0) {
            System.out.println("Ebben a tartályban most nincs szabad üzemanyag.");
            return;
        }

        // 4. Mennyiség, legfeljebb a szabad készletig
        long liter;
        while (true) {
            try {
                liter = Long.parseLong(kerdez("Hány litert foglalsz? (legfeljebb " + tartaly.availableLiters() + ")"));
                if (liter > 0 && liter <= tartaly.availableLiters()) {
                    break;
                }
            } catch (NumberFormatException e) {
                // alább kiírjuk a hibát
            }
            System.out.println("1 és " + tartaly.availableLiters() + " közötti egész számot adj meg!");
        }

        // 5. Foglalás, majd várakozás, amíg a worker feldolgozza
        KeszletMuvelet foglalas = UzemanyagSzolgaltatas.foglal(kolcsonzes, valasztott, liter);
        if (foglalas.status() == OperationStatus.FAILED) {
            System.out.println("Az üzemanyag-foglalás nem sikerült: " + foglalas.failureReason());
            return;
        }
        if (foglalas.status() == OperationStatus.DONE) {
            System.out.println("Lefoglalva: " + liter + " liter " + tartaly.fuelType() + ", "
                    + valasztott.kut().city() + ", " + valasztott.kut().name());
        } else {
            System.out.println("Az üzemanyag-foglalás még feldolgozás alatt van, a Foglalásaim menüben látod majd.");
        }
    }

    // 5. Lemondás

    private static void mondjLeKolcsonzest() throws Exception {
        Kolcsonzes k = valasszKolcsonzest("Melyik kölcsönzést mondod le?");
        if (k == null) {
            return;
        }
        long liter = UzemanyagSzolgaltatas.foglaltLiter(k);
        if (liter > 0) {
            System.out.println("Figyelem: az autóhoz foglalt " + liter
                    + " liter üzemanyag foglalva marad, azt nem lehet lemondani.");
        }
        if (!igen("Biztosan lemondod a #" + k.id() + " kölcsönzést?")) {
            return;
        }
        Kolcsonzes lemondott = KolcsonzesSzolgaltatas.lemond(k, token());
        System.out.println("A #" + lemondott.id() + " kölcsönzés lemondva.");
    }
}
