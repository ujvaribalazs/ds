package ds;

import java.time.LocalDate;
import java.util.List;

import ds.KolcsonzoApi.Bejelentkezes;
import ds.KolcsonzoApi.Car;
import ds.KolcsonzoApi.Felhasznalo;
import ds.KolcsonzoApi.Kolcsonzes;
import ds.KolcsonzoApi.Location;

//Az autókölcsönzés szabályai: mi kölcsönözhető, mi számít aktív kölcsönzésnek, hogyan
//jön létre és mondható le egy kölcsönzés.
public class KolcsonzesSzolgaltatas {

    // A kölcsönzések napra szólnak; az átvétel és a leadás ideje mindig délelőtt 10
    // óra.
    private static final String NAPI_IDOPONT = "T10:00:00";

    public static Felhasznalo regisztral(String email, String jelszo, String keresztnev, String vezeteknev,
            String jogositvanyszam) throws Exception {
        return KolcsonzoApi.register(email, jelszo, keresztnev, vezeteknev, jogositvanyszam);
    }

    public static Bejelentkezes bejelentkezik(String email, String jelszo) throws Exception {
        return KolcsonzoApi.login(email, jelszo);
    }

    public static List<Car> autok() throws Exception {
        return KolcsonzoApi.cars();
    }

    public static Car auto(long id) throws Exception {
        return KolcsonzoApi.car(id);
    }

    public static List<Location> telephelyek() throws Exception {
        return KolcsonzoApi.locations();
    }

    // Az AVAILABLE csak azt jelenti, hogy az autó most szabad; a kért napokra
    // ettől még lehet foglalása, ezt a kölcsönzéskor a szerver ellenőrzi.
    public static boolean kolcsonozheto(Car car) {
        return car.status().equals("AVAILABLE");
    }

    public static Kolcsonzes kolcsonoz(Car car, Long leadasHelye, LocalDate indul, LocalDate erkez, String token)
            throws Exception {
        if (!erkez.isAfter(indul)) {
            throw new IllegalArgumentException("Az érkezésnek későbbinek kell lennie, mint az indulásnak.");
        }
        return KolcsonzoApi.reserve(car.id(), car.currentLocationId(), leadasHelye,
                indul + NAPI_IDOPONT, erkez + NAPI_IDOPONT, token);
    }

    public static List<Kolcsonzes> foglalasaim(String token) throws Exception {
        return KolcsonzoApi.foglalasaim(token);
    }

    public static boolean aktiv(Kolcsonzes k) {
        return !k.status().equals("CANCELLED") && !k.status().equals("COMPLETED");
    }

    public static List<Kolcsonzes> aktivKolcsonzesek(String token) throws Exception {
        return foglalasaim(token).stream().filter(KolcsonzesSzolgaltatas::aktiv).toList();
    }

    // A kölcsönzéshez foglalt üzemanyag megmarad, mert a készlet-szolgáltatásban
    // nem lehet foglalást lemondani.
    public static Kolcsonzes lemond(Kolcsonzes k, String token) throws Exception {
        return KolcsonzoApi.lemond(k.id(), token);
    }
}
